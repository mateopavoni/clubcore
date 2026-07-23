package com.clubcore.api.repository;

import com.clubcore.api.entity.Rutina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RutinaRepository extends JpaRepository<Rutina, Long> {

    Page<Rutina> findByClienteId(Long clienteId, Pageable pageable);
}
