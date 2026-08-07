package com.clubcore.api.service;

import com.clubcore.api.dto.request.ReservaRequest;
import com.clubcore.api.dto.response.ReservaResponse;
import com.clubcore.api.entity.Cliente;
import com.clubcore.api.entity.EstadoMembresia;
import com.clubcore.api.entity.EstadoReserva;
import com.clubcore.api.entity.Membresia;
import com.clubcore.api.entity.Plan;
import com.clubcore.api.entity.Reserva;
import com.clubcore.api.exception.ResourceNotFoundException;
import com.clubcore.api.repository.ClienteRepository;
import com.clubcore.api.repository.MembresiaRepository;
import com.clubcore.api.repository.PlanRepository;
import com.clubcore.api.repository.ReservaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fix 2 (IDOR en reservas): un CLIENTE no debe poder crear una reserva a nombre de otro
 * cliente, ni leer o cancelar reservas ajenas. ADMIN conserva acceso completo.
 *
 * <p>El SecurityContext se puebla manualmente (igual que hace {@code JwtFilter} en un
 * request real) porque las emails de los fixtures se generan en runtime y
 * {@code @WithMockUser} solo acepta un username constante en tiempo de compilación.</p>
 */
@SpringBootTest
class ReservaOwnershipTest {

    @Autowired private ReservaService reservaService;
    @Autowired private ReservaRepository reservaRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private MembresiaRepository membresiaRepository;
    @Autowired private PlanRepository planRepository;

    private Cliente propio;
    private Cliente ajeno;
    private Reserva reservaAjena;

    @BeforeEach
    void setUp() {
        Plan plan = planRepository.save(Plan.builder()
                .nombre("Plan ownership reservas")
                .precio(BigDecimal.valueOf(1000))
                .duracionDias(30)
                .activo(true)
                .build());

        propio = clienteRepository.save(Cliente.builder()
                .nombre("Propio").apellido("Test")
                .email("reserva.propio+" + System.nanoTime() + "@test.com")
                .build());

        ajeno = clienteRepository.save(Cliente.builder()
                .nombre("Ajeno").apellido("Test")
                .email("reserva.ajeno+" + System.nanoTime() + "@test.com")
                .build());

        membresiaRepository.save(Membresia.builder()
                .cliente(propio).plan(plan)
                .fechaInicio(LocalDate.now()).fechaVencimiento(LocalDate.now().plusDays(30))
                .suspendida(false).estadoCacheado(EstadoMembresia.ACTIVA)
                .build());

        membresiaRepository.save(Membresia.builder()
                .cliente(ajeno).plan(plan)
                .fechaInicio(LocalDate.now()).fechaVencimiento(LocalDate.now().plusDays(30))
                .suspendida(false).estadoCacheado(EstadoMembresia.ACTIVA)
                .build());

        reservaAjena = reservaRepository.save(Reserva.builder()
                .cliente(ajeno)
                .descripcion("reserva del cliente ajeno")
                .fechaReserva(LocalDateTime.now().plusDays(1))
                .estado(EstadoReserva.CONFIRMADA)
                .build());
    }

    @AfterEach
    void limpiarContextoSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("CLIENTE no puede crear una reserva con el clienteId de otro cliente (404)")
    void crearReserva_conClienteIdAjeno_lanza404() {
        autenticarComo(propio, "CLIENTE");

        assertThatThrownBy(() -> reservaService.crearReserva(
                new ReservaRequest(ajeno.getId(), "intento en nombre de otro", LocalDateTime.now().plusDays(1))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("CLIENTE puede crear una reserva propia")
    void crearReserva_conPropioClienteId_funciona() {
        autenticarComo(propio, "CLIENTE");

        ReservaResponse response = reservaService.crearReserva(
                new ReservaRequest(propio.getId(), "reserva propia", LocalDateTime.now().plusDays(1)));

        assertThat(response.clienteId()).isEqualTo(propio.getId());
    }

    @Test
    @DisplayName("CLIENTE no puede leer una reserva ajena (404, no 403)")
    void obtenerReserva_ajena_lanza404() {
        autenticarComo(propio, "CLIENTE");

        assertThatThrownBy(() -> reservaService.obtenerReserva(reservaAjena.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("CLIENTE no puede cancelar una reserva ajena (404, no 403)")
    void cancelarReserva_ajena_lanza404() {
        autenticarComo(propio, "CLIENTE");

        assertThatThrownBy(() -> reservaService.cancelarReserva(reservaAjena.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // La reserva ajena no debe haber sido tocada
        assertThat(reservaRepository.findById(reservaAjena.getId()).orElseThrow().getEstado())
                .isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    @DisplayName("ADMIN conserva acceso completo: puede leer y cancelar la reserva de cualquier cliente")
    void admin_accedeYCancelaReservaDeCualquierCliente() {
        autenticarComo(propio, "ADMIN");

        ReservaResponse leida = reservaService.obtenerReserva(reservaAjena.getId());
        assertThat(leida.id()).isEqualTo(reservaAjena.getId());

        ReservaResponse cancelada = reservaService.cancelarReserva(reservaAjena.getId());
        assertThat(cancelada.estado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    private void autenticarComo(Cliente cliente, String rol) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        cliente.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }
}
