package com.clubcore.api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClienteResponse(
        Long id,
        String nombre,
        String apellido,
        String email,
        String telefono,
        LocalDate fechaNacimiento,
        LocalDateTime createdAt
) {}
