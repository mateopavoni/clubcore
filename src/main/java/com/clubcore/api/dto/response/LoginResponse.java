package com.clubcore.api.dto.response;

public record LoginResponse(
        String token,
        String tipo,
        String rol,
        Long usuarioId
) {}
