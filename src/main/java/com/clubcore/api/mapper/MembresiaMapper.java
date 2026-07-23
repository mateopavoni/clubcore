package com.clubcore.api.mapper;

import com.clubcore.api.dto.response.MembresiaResponse;
import com.clubcore.api.entity.Membresia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MembresiaMapper {

    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(target = "clienteNombre",
            expression = "java(membresia.getCliente().getNombre() + \" \" + membresia.getCliente().getApellido())")
    @Mapping(source = "plan.id", target = "planId")
    @Mapping(source = "plan.nombre", target = "planNombre")
    @Mapping(source = "estadoCacheado", target = "estado")
    MembresiaResponse toResponse(Membresia membresia);
}
