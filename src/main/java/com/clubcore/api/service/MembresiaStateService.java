package com.clubcore.api.service;

import com.clubcore.api.entity.EstadoMembresia;
import com.clubcore.api.entity.EstadoPago;
import com.clubcore.api.entity.EstadoReserva;
import com.clubcore.api.entity.Membresia;
import com.clubcore.api.entity.Plan;
import com.clubcore.api.event.MembresiaPorVencerEvent;
import com.clubcore.api.repository.MembresiaRepository;
import com.clubcore.api.repository.PagoRepository;
import com.clubcore.api.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * Motor de estados de membresía — la pieza central de ClubCore.
 *
 * <h2>¿Por qué on-demand y no persistido?</h2>
 * <p>El estado de una membresía depende de factores que cambian sin que el sistema
 * lo sepa de antemano: el paso del tiempo (vencimiento), la llegada de un pago
 * (DEUDA → ACTIVA), o una decisión admin (SUSPENDIDA). Persistir el estado como campo
 * mutable escrito desde múltiples lugares introduce inconsistencias. La solución:
 * calcular on-demand desde los datos fuente y cachear el resultado.</p>
 *
 * <h2>Orden de evaluación (prioridad decreciente)</h2>
 * <ol>
 *   <li><b>SUSPENDIDA</b> — flag manual activado por un ADMIN. Máxima prioridad.</li>
 *   <li><b>INACTIVA</b>   — fecha de vencimiento pasó.</li>
 *   <li><b>DEUDA</b>      — tiene al menos un pago en estado PENDIENTE.</li>
 *   <li><b>INACTIVA</b>   — cupo mensual del plan agotado.</li>
 *   <li><b>ACTIVA</b>     — ninguna condición anterior aplica.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MembresiaStateService {

    private final PagoRepository pagoRepository;
    private final ReservaRepository reservaRepository;
    private final MembresiaRepository membresiaRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Estados de reserva que efectivamente ocupan cupo mensual. CANCELADA se excluye a
     * proposito: cancelar una reserva debe liberar el cupo, no quemarlo permanentemente.
     */
    private static final EnumSet<EstadoReserva> ESTADOS_QUE_OCUPAN_CUPO =
            EnumSet.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA);

    /**
     * Calcula el estado real de la membresía sin modificar la base de datos.
     * Llamar desde cualquier contexto de lectura.
     *
     * @param membresia la membresía a evaluar (con {@code plan} ya cargado)
     * @return el estado calculado
     */
    public EstadoMembresia evaluarEstado(Membresia membresia) {

        // 1. Suspensión manual: máxima prioridad, bloquea todo lo demás
        if (membresia.isSuspendida()) {
            log.debug("Membresía {} suspendida manualmente", membresia.getId());
            return EstadoMembresia.SUSPENDIDA;
        }

        LocalDate hoy = LocalDate.now();

        // 2. Vencimiento temporal: la fecha de fin ya pasó
        if (membresia.getFechaVencimiento().isBefore(hoy)) {
            log.debug("Membresía {} vencida el {}", membresia.getId(), membresia.getFechaVencimiento());
            return EstadoMembresia.INACTIVA;
        }

        // 3. Deuda: hay al menos un pago pendiente de confirmación
        boolean tieneDeuda = pagoRepository.existsByMembresiaAndEstado(membresia, EstadoPago.PENDIENTE);
        if (tieneDeuda) {
            log.debug("Membresía {} tiene pagos pendientes (DEUDA)", membresia.getId());
            return EstadoMembresia.DEUDA;
        }

        // 4. Cupo del plan agotado: si el plan limita reservas por mes
        Plan plan = membresia.getPlan();
        if (plan.getLimiteReservasMes() != null) {
            LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();
            LocalDateTime finMes = inicioMes.plusMonths(1);

            long reservasEsteMes = reservaRepository.countByClienteAndFechaReservaBetweenAndEstadoIn(
                    membresia.getCliente(), inicioMes, finMes, ESTADOS_QUE_OCUPAN_CUPO
            );

            if (reservasEsteMes >= plan.getLimiteReservasMes()) {
                log.debug("Membresía {} con cupo agotado ({}/{} reservas este mes)",
                        membresia.getId(), reservasEsteMes, plan.getLimiteReservasMes());
                return EstadoMembresia.INACTIVA;
            }
        }

        // 5. Todo OK
        return EstadoMembresia.ACTIVA;
    }

    /**
     * Calcula el estado y lo persiste en {@code estadoCacheado}.
     * Usar solo desde flujos que ya tomaron una decisión de negocio
     * (check-in, creación de reserva, recepción de pago). No llamar desde lectura pura.
     *
     * @param membresia la membresía a evaluar y actualizar
     * @return el estado calculado y ya persistido
     */
    @Transactional
    public EstadoMembresia actualizarEstadoCacheado(Membresia membresia) {
        EstadoMembresia estado = evaluarEstado(membresia);
        membresia.setEstadoCacheado(estado);
        membresiaRepository.save(membresia);
        log.debug("EstadoCacheado de membresía {} actualizado a {}", membresia.getId(), estado);
        return estado;
    }

    /**
     * Job diario que notifica las membresías próximas a vencer (el día siguiente).
     *
     * <p>Corre a las 8:00 AM todos los días. Por cada membresía encontrada, publica un
     * {@link MembresiaPorVencerEvent} que el listener async de
     * {@link NotificationDispatchService} procesa en un thread separado para no bloquear.</p>
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void notificarVencimientosProximos() {
        LocalDate manana = LocalDate.now().plusDays(1);
        List<Membresia> proximas = membresiaRepository.findByFechaVencimientoAndSuspendidaFalse(manana);

        log.info("Job vencimientos: {} membresías vencen mañana ({})", proximas.size(), manana);

        for (Membresia membresia : proximas) {
            eventPublisher.publishEvent(new MembresiaPorVencerEvent(this, membresia));
        }
    }
}
