package com.clubcore.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Formato de email invalido")
        String email,

        @NotBlank(message = "La contrasena es obligatoria")
        String password
) {}
