package com.clubcore.api.mapper;

import com.clubcore.api.dto.response.AccesoResponse;
import com.clubcore.api.entity.Acceso;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccesoMapper {

    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(source = "membresia.id", target = "membresiaId")
    AccesoResponse toResponse(Acceso acceso);
}
