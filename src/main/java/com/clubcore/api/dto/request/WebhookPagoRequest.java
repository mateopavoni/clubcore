package com.clubcore.api.dto.request;

import com.clubcore.api.entity.EstadoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Payload que envía un gateway de pago externo al endpoint inbound
 * {@code POST /api/v1/webhooks/pagos}.
 */
public record WebhookPagoRequest(
        @NotBlank(message = "La referencia es obligatoria")
        String referencia,

        @NotNull(message = "El ID de la membresía es obligatorio")
        Long membresiaId,

        @NotNull(message = "El monto es obligatorio")
        @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
        BigDecimal monto,

        @NotNull(message = "El estado es obligatorio")
        EstadoPago estado
) {}
