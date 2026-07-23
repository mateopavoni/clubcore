package com.clubcore.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RutinaRequest(
        @NotNull(message = "El ID del cliente es obligatorio")
        Long clienteId,

        @NotBlank(message = "El nombre de la rutina es obligatorio")
        String nombre,

        String descripcion,

        @NotNull(message = "La fecha de creacion es obligatoria")
        LocalDate fechaCreacion
) {}
