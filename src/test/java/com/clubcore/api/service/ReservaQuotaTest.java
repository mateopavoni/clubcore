package com.clubcore.api.service;

import com.clubcore.api.dto.request.ReservaRequest;
import com.clubcore.api.dto.response.ReservaResponse;
import com.clubcore.api.entity.Cliente;
import com.clubcore.api.entity.EstadoMembresia;
import com.clubcore.api.entity.EstadoReserva;
import com.clubcore.api.entity.Membresia;
import com.clubcore.api.entity.Plan;
import com.clubcore.api.repository.ClienteRepository;
import com.clubcore.api.repository.MembresiaRepository;
import com.clubcore.api.repository.PlanRepository;
import com.clubcore.api.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reproduce el bug de la auditoria de seguridad (Fix 5): {@code countByClienteAndFechaReservaBetween}
 * no filtraba por estado, asi que una reserva CANCELADA seguia contando contra el cupo mensual del
 * plan para siempre — {@link com.clubcore.api.service.impl.ReservaServiceImpl#cancelarReserva} solo
 * cambia el estado de la fila, nunca la borra.
 *
 * <p>Sin @Transactional, igual que {@link ReservaConcurrencyTest}: el fixture debe quedar
 * commiteado de verdad para que las llamadas subsiguientes al servicio lo vean.</p>
 */
@SpringBootTest
class ReservaQuotaTest {

    @Autowired
    private ReservaService reservaService;
    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private ClienteRepository clienteRepository;
    @Autowired
    private MembresiaRepository membresiaRepository;
    @Autowired
    private PlanRepository planRepository;

    private Long clienteId;

    @BeforeEach
    void setUp() {
        Plan plan = planRepository.save(Plan.builder()
                .nombre("Plan cupo 1 (quota test)")
                .precio(BigDecimal.valueOf(1000))
                .duracionDias(30)
                .limiteReservasMes(1)
                .activo(true)
                .build());

        Cliente cliente = clienteRepository.save(Cliente.builder()
                .nombre("Cupo")
                .apellido("Test")
                .email("cupo+" + System.nanoTime() + "@test.com")
                .build());
        clienteId = cliente.getId();

        membresiaRepository.save(Membresia.builder()
                .cliente(cliente)
                .plan(plan)
                .fechaInicio(LocalDate.now())
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .suspendida(false)
                .estadoCacheado(EstadoMembresia.ACTIVA)
                .build());
    }

    @Test
    @DisplayName("Reservar, cancelar y volver a reservar en el mismo mes no debe bloquear por cupo")
    void reservarCancelarYReservarDeNuevo_mismoMes_noBloqueaPorCupo() {
        // 1. Primera reserva: consume el cupo (limiteReservasMes = 1)
        ReservaResponse primera = reservaService.crearReserva(new ReservaRequest(
                clienteId, "primera reserva del mes", LocalDateTime.now().plusDays(1)));

        assertThat(primera).isNotNull();

        // 2. Cancelar: la fila sigue existiendo, solo cambia el estado
        reservaService.cancelarReserva(primera.id());
        assertThat(reservaRepository.findById(primera.id()).orElseThrow().getEstado())
                .isEqualTo(EstadoReserva.CANCELADA);

        // 3. Segunda reserva en el mismo mes: antes del fix, esto lanzaba MembresiaInvalidaException
        // porque la cancelada seguia contando contra el cupo=1. Con el fix, debe pasar.
        ReservaResponse segunda = reservaService.crearReserva(new ReservaRequest(
                clienteId, "segunda reserva tras cancelar", LocalDateTime.now().plusDays(2)));

        assertThat(segunda).isNotNull();
        assertThat(segunda.id()).isNotEqualTo(primera.id());

        // Verificacion de datos: 2 filas en la tabla, 1 CANCELADA + 1 CONFIRMADA
        assertThat(reservaRepository.count()).isGreaterThanOrEqualTo(2);
    }
}
