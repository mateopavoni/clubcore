package com.clubcore.api.mapper;

import com.clubcore.api.dto.response.PlanResponse;
import com.clubcore.api.entity.Plan;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PlanMapper {

    PlanResponse toResponse(Plan plan);
}
