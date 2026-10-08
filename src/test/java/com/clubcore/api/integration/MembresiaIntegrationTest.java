package com.clubcore.api.integration;

import com.clubcore.api.entity.EstadoMembresia;
import com.clubcore.api.entity.Membresia;
import com.clubcore.api.entity.Cliente;
import com.clubcore.api.entity.Plan;
import com.clubcore.api.repository.ClienteRepository;
import com.clubcore.api.repository.MembresiaRepository;
import com.clubcore.api.repository.PlanRepository;
import com.clubcore.api.service.MembresiaStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de integración contra MySQL real vía Testcontainers.
 *
 * <p>Complementan la suite rápida de H2 — no la reemplazan. Verifican que:</p>
 * <ol>
 *   <li>Flyway migra V1 + V2 correctamente en MySQL 8.4 real (no H2 emulado con MODE=MySQL).</li>
 *   <li>El motor de estados {@link MembresiaStateService} funciona de punta a punta
 *       contra MySQL InnoDB con constraints FK reales y comportamiento de fechas MySQL.</li>
 * </ol>
 *
 * <p><b>Requisito:</b> Docker disponible en el host. Ver {@code RUN.md} sección
 * "Tests de integración (Testcontainers)" para cómo correrlos con o sin JDK local.</p>
 *
 * <h2>Estrategia de aislamiento</h2>
 * <p>La clase es {@code @Transactional}: cada test method arranca y hace rollback de su
 * propia transacción. Los datos del {@code @BeforeEach} (plan + cliente) son visibles
 * durante el test y se descartan al finalizar. El schema Flyway persiste durante toda
 * la ejecución de la clase (el contenedor se levanta una sola vez y lo comparte).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Transactional
class MembresiaIntegrationTest {

    /**
     * Contenedor MySQL 8.4 — misma imagen que producción.
     * {@code static} para que se levante una sola vez y lo compartan todos los tests
     * de la clase (más rápido que levantar un contenedor por test).
     */
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("clubcore_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    /**
     * Inyecta las propiedades del contenedor en el {@code Environment} de Spring Boot
     * ANTES de que arranque el contexto.
     *
     * <p>{@code @DynamicPropertySource} tiene mayor prioridad que las propiedades de
     * perfil ({@code application-test.yml}), por lo que sobreescribe la configuración H2
     * sin necesidad de cambiar el perfil activo (build.gradle sigue usando "test").</p>
     */
    @DynamicPropertySource
    static void configureMysqlContainer(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",                        mysql::getJdbcUrl);
        registry.add("spring.datasource.username",                   mysql::getUsername);
        registry.add("spring.datasource.password",                   mysql::getPassword);
        registry.add("spring.datasource.driver-class-name",          () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform",                 () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.properties.hibernate.dialect",      () -> "org.hibernate.dialect.MySQLDialect");
        // Sobreescribe el create-drop y Flyway disabled del perfil test
        registry.add("spring.jpa.hibernate.ddl-auto",                () -> "validate");
        registry.add("spring.flyway.enabled",                        () -> "true");
        registry.add("spring.flyway.locations",                      () -> "classpath:db/migration,classpath:db/seed");
    }

    @Autowired private PlanRepository       planRepo;
    @Autowired private ClienteRepository    clienteRepo;
    @Autowired private MembresiaRepository  membresiaRepo;
    @Autowired private MembresiaStateService stateService;

    private Plan    plan;
    private Cliente cliente;

    /**
     * Fixture compartido por todos los tests: plan sin límite de reservas + cliente sin cuenta.
     * Al ser parte de la transacción del test, se descarta en el rollback de cada método.
     */
    @BeforeEach
    void setUp() {
        plan = planRepo.save(Plan.builder()
                .nombre("Plan Integración Test")
                .precio(BigDecimal.valueOf(5000))
                .duracionDias(30)
                // limiteReservasMes = null → ilimitado (evita el camino de código de cupos)
                .build());

        cliente = clienteRepo.save(Cliente.builder()
                .nombre("Test")
                .apellido("Integración")
                .email("integracion.test@clubcore.test")
                .build());
    }

    @Test
    void membresiaVigente_sinSuspension_sinDeuda_evaluarEstadoRetornaActiva() {
        Membresia m = membresiaRepo.save(Membresia.builder()
                .cliente(cliente)
                .plan(plan)
                .fechaInicio(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .build());

        assertThat(stateService.evaluarEstado(m))
                .isEqualTo(EstadoMembresia.ACTIVA);
    }

    @Test
    void membresiaConFlagSuspendida_evaluarEstadoRetornaSuspendida() {
        // El flag suspendida tiene máxima prioridad: vence todo lo demás
        Membresia m = membresiaRepo.save(Membresia.builder()
                .cliente(cliente)
                .plan(plan)
                .fechaInicio(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .suspendida(true)
                .build());

        assertThat(stateService.evaluarEstado(m))
                .isEqualTo(EstadoMembresia.SUSPENDIDA);
    }

    @Test
    void membresiaConFechaVencidaAyer_evaluarEstadoRetornaInactiva() {
        Membresia m = membresiaRepo.save(Membresia.builder()
                .cliente(cliente)
                .plan(plan)
                .fechaInicio(LocalDate.now().minusDays(31))
                .fechaVencimiento(LocalDate.now().minusDays(1))
                .build());

        assertThat(stateService.evaluarEstado(m))
                .isEqualTo(EstadoMembresia.INACTIVA);
    }

    @Test
    void membresiaActiva_suspenderLuego_evaluarEstadoRetornaSuspendida() {
        // Flujo de negocio: activar → luego ADMIN suspende
        Membresia m = membresiaRepo.save(Membresia.builder()
                .cliente(cliente)
                .plan(plan)
                .fechaInicio(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .build());

        assertThat(stateService.evaluarEstado(m))
                .isEqualTo(EstadoMembresia.ACTIVA);

        m.setSuspendida(true);
        membresiaRepo.save(m);

        assertThat(stateService.evaluarEstado(m))
                .isEqualTo(EstadoMembresia.SUSPENDIDA);
    }

    @Test
    void flywayV1yV2MigranCorrectamente_dataSeedDisponible() {
        // V2__seed_roles.sql inserta al menos 3 planes de ejemplo
        assertThat(planRepo.count()).isGreaterThanOrEqualTo(3);
    }
}
