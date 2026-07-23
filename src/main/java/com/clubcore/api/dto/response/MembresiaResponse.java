package com.clubcore.api.dto.response;

import com.clubcore.api.entity.EstadoMembresia;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MembresiaResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        Long planId,
        String planNombre,
        LocalDate fechaInicio,
        LocalDate fechaVencimiento,
        EstadoMembresia estado,
        boolean suspendida,
        LocalDateTime createdAt
) {}
