package com.clubcore.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record AccesoRequest(
        @NotNull(message = "El ID del cliente es obligatorio")
        Long clienteId
) {}
