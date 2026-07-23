package com.clubcore.api.repository;

import com.clubcore.api.entity.Cliente;
import com.clubcore.api.entity.Reserva;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    Page<Reserva> findByClienteId(Long clienteId, Pageable pageable);

    /**
     * Cuenta las reservas de un cliente en un rango de tiempo.
     * Usada por {@code MembresiaStateService} para verificar el cupo mensual del plan.
     */
    long countByClienteAndFechaReservaBetween(Cliente cliente, LocalDateTime inicio, LocalDateTime fin);
}
