package com.clubcore.api.mapper;

import com.clubcore.api.dto.response.RutinaResponse;
import com.clubcore.api.entity.Rutina;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RutinaMapper {

    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(target = "clienteNombre",
            expression = "java(rutina.getCliente().getNombre() + \" \" + rutina.getCliente().getApellido())")
    RutinaResponse toResponse(Rutina rutina);
}
