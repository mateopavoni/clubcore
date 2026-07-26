package com.clubcore.api.dto.response;

import java.time.LocalDateTime;

/**
 * Estructura de error uniforme devuelta por el {@code GlobalExceptionHandler}.
 * El cliente nunca recibe stack traces ni detalles internos.
 */
public record ApiErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {}
