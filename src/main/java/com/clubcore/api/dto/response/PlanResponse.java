package com.clubcore.api.dto.response;

import java.math.BigDecimal;

public record PlanResponse(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        int duracionDias,
        Integer limiteReservasMes,
        boolean activo
) {}
