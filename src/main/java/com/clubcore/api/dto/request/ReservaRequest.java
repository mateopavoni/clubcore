package com.clubcore.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ReservaRequest(
        @NotNull(message = "El ID del cliente es obligatorio")
        Long clienteId,

        @NotBlank(message = "La descripcion es obligatoria")
        String descripcion,

        @NotNull(message = "La fecha de reserva es obligatoria")
        LocalDateTime fechaReserva
) {}
