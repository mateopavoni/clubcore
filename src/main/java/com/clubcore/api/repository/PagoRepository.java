package com.clubcore.api.repository;

import com.clubcore.api.entity.EstadoPago;
import com.clubcore.api.entity.Membresia;
import com.clubcore.api.entity.Pago;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Page<Pago> findAll(Pageable pageable);

    Page<Pago> findByMembresiaClienteId(Long clienteId, Pageable pageable);

    /**
     * Verifica si existe algún pago pendiente para la membresía.
     * Usado por {@code MembresiaStateService.evaluarEstado()} para detectar DEUDA.
     */
    boolean existsByMembresiaAndEstado(Membresia membresia, EstadoPago estado);

    Optional<Pago> findByReferencia(String referencia);
}
