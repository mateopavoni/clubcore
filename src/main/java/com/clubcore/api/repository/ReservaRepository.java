package com.clubcore.api.repository;

import com.clubcore.api.entity.Cliente;
import com.clubcore.api.entity.EstadoReserva;
import com.clubcore.api.entity.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Page<Reserva> findByClienteId(Long clienteId, Pageable pageable);

    /**
     * Cuenta las reservas de un cliente en un rango de tiempo, filtrando por estado.
     * Usada por {@code MembresiaStateService} para verificar el cupo mensual del plan.
     *
     * <p>Debe invocarse con los estados que efectivamente ocupan cupo (PENDIENTE, CONFIRMADA).
     * Una reserva CANCELADA nunca debe contar contra el cupo — {@code cancelarReserva} solo
     * cambia el estado de la fila, no la elimina, asi que sin este filtro una cancelacion
     * quemaba el cupo del cliente para siempre.</p>
     */
    long countByClienteAndFechaReservaBetweenAndEstadoIn(
            Cliente cliente, LocalDateTime inicio, LocalDateTime fin, Collection<EstadoReserva> estados);
}
