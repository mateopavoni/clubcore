package com.clubcore.api.repository;

import com.clubcore.api.entity.Acceso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccesoRepository extends JpaRepository<Acceso, Long> {

    Page<Acceso> findByClienteId(Long clienteId, Pageable pageable);
}
