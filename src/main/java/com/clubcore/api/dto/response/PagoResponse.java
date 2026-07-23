package com.clubcore.api.dto.response;

import com.clubcore.api.entity.EstadoPago;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PagoResponse(
        Long id,
        Long membresiaId,
        BigDecimal monto,
        LocalDateTime fechaPago,
        EstadoPago estado,
        String referencia
) {}
