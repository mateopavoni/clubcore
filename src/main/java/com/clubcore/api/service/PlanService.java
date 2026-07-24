package com.clubcore.api.service;

import com.clubcore.api.dto.request.PlanRequest;
import com.clubcore.api.dto.response.PlanResponse;

import java.util.List;

public interface PlanService {

    PlanResponse crearPlan(PlanRequest request);

    PlanResponse obtenerPlan(Long id);

    List<PlanResponse> listarPlanesActivos();

    PlanResponse actualizarPlan(Long id, PlanRequest request);
}
