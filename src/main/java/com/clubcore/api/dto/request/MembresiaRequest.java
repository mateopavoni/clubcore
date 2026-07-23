package com.clubcore.api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record MembresiaRequest(
        @NotNull(message = "El ID del cliente es obligatorio")
        Long clienteId,

        @NotNull(message = "El ID del plan es obligatorio")
        Long planId,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate fechaInicio
) {}
