package com.clubcore.api.mapper;

import com.clubcore.api.dto.response.ReservaResponse;
import com.clubcore.api.entity.Reserva;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReservaMapper {

    @Mapping(source = "cliente.id", target = "clienteId")
    @Mapping(target = "clienteNombre",
            expression = "java(reserva.getCliente().getNombre() + \" \" + reserva.getCliente().getApellido())")
    ReservaResponse toResponse(Reserva reserva);
}
