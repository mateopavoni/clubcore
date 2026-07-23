package com.clubcore.api.repository;

import com.clubcore.api.entity.Membresia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MembresiaRepository extends JpaRepository<Membresia, Long> {

    Page<Membresia> findByClienteId(Long clienteId, Pageable pageable);

    /**
     * Devuelve las membresías que vencen exactamente en la fecha indicada y no están suspendidas.
     * Usada por el job diario de notificaciones.
     */
    List<Membresia> findByFechaVencimientoAndSuspendidaFalse(LocalDate fechaVencimiento);

    Optional<Membresia> findFirstByClienteIdOrderByFechaVencimientoDesc(Long clienteId);
}
