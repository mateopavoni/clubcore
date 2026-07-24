package com.clubcore.api.service.impl;

import com.clubcore.api.dto.request.MembresiaRequest;
import com.clubcore.api.dto.response.MembresiaResponse;
import com.clubcore.api.entity.Cliente;
import com.clubcore.api.entity.Membresia;
import com.clubcore.api.entity.Plan;
import com.clubcore.api.exception.ResourceNotFoundException;
import com.clubcore.api.mapper.MembresiaMapper;
import com.clubcore.api.repository.ClienteRepository;
import com.clubcore.api.repository.MembresiaRepository;
import com.clubcore.api.repository.PlanRepository;
import com.clubcore.api.service.MembresiaService;
import com.clubcore.api.service.MembresiaStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MembresiaServiceImpl implements MembresiaService {

    private final MembresiaRepository membresiaRepository;
    private final ClienteRepository clienteRepository;
    private final PlanRepository planRepository;
    private final MembresiaMapper membresiaMapper;
    private final MembresiaStateService membresiaStateService;

    @Override
    @Transactional
    public MembresiaResponse crearMembresia(MembresiaRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.clienteId()));

        Plan plan = planRepository.findById(request.planId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan", "id", request.planId()));

        LocalDate fechaInicio = request.fechaInicio();
        LocalDate fechaVencimiento = fechaInicio.plusDays(plan.getDuracionDias());

        Membresia membresia = Membresia.builder()
                .cliente(cliente)
                .plan(plan)
                .fechaInicio(fechaInicio)
                .fechaVencimiento(fechaVencimiento)
                .suspendida(false)
                .build();

        Membresia guardada = membresiaRepository.save(membresia);

        // Calcular y cachear el estado inicial
        membresiaStateService.actualizarEstadoCacheado(guardada);

        return membresiaMapper.toResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public MembresiaResponse obtenerMembresia(Long id) {
        return membresiaMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MembresiaResponse> listarMembresias(Pageable pageable) {
        return membresiaRepository.findAll(pageable).map(membresiaMapper::toResponse);
    }

    @Override
    @Transactional
    public MembresiaResponse suspenderMembresia(Long id, boolean suspendida) {
        Membresia membresia = findOrThrow(id);
        membresia.setSuspendida(suspendida);
        membresiaStateService.actualizarEstadoCacheado(membresia);
        return membresiaMapper.toResponse(membresia);
    }

    private Membresia findOrThrow(Long id) {
        return membresiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membresía", "id", id));
    }
}
