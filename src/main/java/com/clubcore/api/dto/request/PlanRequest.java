package com.clubcore.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlanRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
        BigDecimal precio,

        @NotNull(message = "La duracion en dias es obligatoria")
        @Min(value = 1, message = "La duracion debe ser al menos 1 dia")
        Integer duracionDias,

        /** Null = sin limite de reservas por mes. */
        Integer limiteReservasMes
) {}
