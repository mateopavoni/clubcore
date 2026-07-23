package com.clubcore.api.mapper;

import com.clubcore.api.dto.response.PagoResponse;
import com.clubcore.api.entity.Pago;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PagoMapper {

    @Mapping(source = "membresia.id", target = "membresiaId")
    PagoResponse toResponse(Pago pago);
}
