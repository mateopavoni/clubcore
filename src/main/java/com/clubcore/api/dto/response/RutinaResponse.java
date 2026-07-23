package com.clubcore.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RutinaResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        String nombre,
        String descripcion,
        LocalDate fechaCreacion,
        LocalDateTime createdAt
) {}
