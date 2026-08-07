package com.clubcore.api.service;

import com.clubcore.api.dto.response.RutinaResponse;
import com.clubcore.api.entity.Cliente;
import com.clubcore.api.entity.Rutina;
import com.clubcore.api.exception.ResourceNotFoundException;
import com.clubcore.api.repository.ClienteRepository;
import com.clubcore.api.repository.RutinaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fix 3 (IDOR en rutinas): un CLIENTE solo debe poder listar/leer sus propias rutinas,
 * nunca las de otro cliente. ADMIN/ENTRENADOR conservan acceso completo.
 *
 * <p>Ver Javadoc de {@link ReservaOwnershipTest} sobre por qué el SecurityContext se
 * puebla manualmente en vez de usar {@code @WithMockUser}.</p>
 */
@SpringBootTest
class RutinaOwnershipTest {

    @Autowired private RutinaService rutinaService;
    @Autowired private RutinaRepository rutinaRepository;
    @Autowired private ClienteRepository clienteRepository;

    private Cliente propio;
    private Cliente ajeno;
    private Rutina rutinaPropia;
    private Rutina rutinaAjena;

    @BeforeEach
    void setUp() {
        propio = clienteRepository.save(Cliente.builder()
                .nombre("Propio").apellido("Rutina")
                .email("rutina.propio+" + System.nanoTime() + "@test.com")
                .build());

        ajeno = clienteRepository.save(Cliente.builder()
                .nombre("Ajeno").apellido("Rutina")
                .email("rutina.ajeno+" + System.nanoTime() + "@test.com")
                .build());

        rutinaPropia = rutinaRepository.save(Rutina.builder()
                .cliente(propio)
                .nombre("Rutina propia")
                .descripcion("full body")
                .fechaCreacion(LocalDate.now())
                .build());

        rutinaAjena = rutinaRepository.save(Rutina.builder()
                .cliente(ajeno)
                .nombre("Rutina ajena")
                .descripcion("upper body")
                .fechaCreacion(LocalDate.now())
                .build());
    }

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("CLIENTE no puede leer la rutina de otro cliente (404, no 403)")
    void obtenerRutina_ajena_lanza404() {
        autenticarComo(propio, "CLIENTE");

        assertThatThrownBy(() -> rutinaService.obtenerRutina(rutinaAjena.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("CLIENTE puede leer su propia rutina")
    void obtenerRutina_propia_funciona() {
        autenticarComo(propio, "CLIENTE");

        RutinaResponse response = rutinaService.obtenerRutina(rutinaPropia.getId());

        assertThat(response.id()).isEqualTo(rutinaPropia.getId());
    }

    @Test
    @DisplayName("CLIENTE solo ve sus propias rutinas al listar, no las de otros clientes")
    void listarRutinas_comoCliente_soloDevuelveLasPropias() {
        autenticarComo(propio, "CLIENTE");
        Pageable pageable = PageRequest.of(0, 20);

        List<Long> ids = rutinaService.listarRutinas(pageable).map(RutinaResponse::id).toList();

        assertThat(ids).contains(rutinaPropia.getId());
        assertThat(ids).doesNotContain(rutinaAjena.getId());
    }

    @Test
    @DisplayName("ADMIN ve todas las rutinas al listar, incluidas las de otros clientes")
    void listarRutinas_comoAdmin_devuelveTodas() {
        autenticarComo(propio, "ADMIN");
        Pageable pageable = PageRequest.of(0, 50);

        List<Long> ids = rutinaService.listarRutinas(pageable).map(RutinaResponse::id).toList();

        assertThat(ids).contains(rutinaPropia.getId(), rutinaAjena.getId());
    }

    @Test
    @DisplayName("ENTRENADOR puede leer la rutina de cualquier cliente")
    void obtenerRutina_comoEntrenador_accedeAcualquierRutina() {
        autenticarComo(propio, "ENTRENADOR");

        RutinaResponse response = rutinaService.obtenerRutina(rutinaAjena.getId());

        assertThat(response.id()).isEqualTo(rutinaAjena.getId());
    }

    private void autenticarComo(Cliente cliente, String rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        cliente.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }
}
