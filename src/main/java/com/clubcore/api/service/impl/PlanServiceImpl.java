package com.clubcore.api.service.impl;

import com.clubcore.api.dto.request.PlanRequest;
import com.clubcore.api.dto.response.PlanResponse;
import com.clubcore.api.entity.Plan;
import com.clubcore.api.exception.ResourceNotFoundException;
import com.clubcore.api.mapper.PlanMapper;
import com.clubcore.api.repository.PlanRepository;
import com.clubcore.api.service.PlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;
    private final PlanMapper planMapper;

    @Override
    @Transactional
    @CacheEvict(value = "planes", allEntries = true)
    public PlanResponse crearPlan(PlanRequest request) {
        Plan plan = Plan.builder()
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .precio(request.precio())
                .duracionDias(request.duracionDias())
                .limiteReservasMes(request.limiteReservasMes())
                .activo(true)
                .build();
        return planMapper.toResponse(planRepository.save(plan));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "planes", key = "#id")
    public PlanResponse obtenerPlan(Long id) {
        return planMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "planes", key = "'activos'")
    public List<PlanResponse> listarPlanesActivos() {
        return planRepository.findByActivoTrue().stream()
                .map(planMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "planes", allEntries = true)
    public PlanResponse actualizarPlan(Long id, PlanRequest request) {
        Plan plan = findOrThrow(id);
        plan.setNombre(request.nombre());
        plan.setDescripcion(request.descripcion());
        plan.setPrecio(request.precio());
        plan.setDuracionDias(request.duracionDias());
        plan.setLimiteReservasMes(request.limiteReservasMes());
        return planMapper.toResponse(planRepository.save(plan));
    }

    private Plan findOrThrow(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", id));
    }
}
