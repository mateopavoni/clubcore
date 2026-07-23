package com.clubcore.api.mapper;

import com.clubcore.api.dto.response.ClienteResponse;
import com.clubcore.api.entity.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClienteMapper {

    ClienteResponse toResponse(Cliente cliente);
}
