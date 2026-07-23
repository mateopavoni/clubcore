package com.clubcore.api.dto.response;

import com.clubcore.api.entity.EstadoReserva;

import java.time.LocalDateTime;

public record ReservaResponse(
        Long id,
        Long clienteId,
        String clienteNombre,
        String descripcion,
        LocalDateTime fechaReserva,
        EstadoReserva estado,
        LocalDateTime createdAt
) {}
