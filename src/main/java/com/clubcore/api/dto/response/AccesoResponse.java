package com.clubcore.api.dto.response;

import java.time.LocalDateTime;

public record AccesoResponse(
        Long id,
        Long clienteId,
        Long membresiaId,
        LocalDateTime fechaAcceso,
        boolean exitoso,
        String motivoRechazo
) {}
