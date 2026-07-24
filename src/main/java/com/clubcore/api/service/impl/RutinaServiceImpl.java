package com.clubcore.api.service.impl;

import com.clubcore.api.dto.request.RutinaRequest;
import com.clubcore.api.dto.response.RutinaResponse;
import com.clubcore.api.entity.Cliente;
import com.clubcore.api.entity.Rutina;
import com.clubcore.api.exception.ResourceNotFoundException;
import com.clubcore.api.mapper.RutinaMapper;
import com.clubcore.api.repository.ClienteRepository;
import com.clubcore.api.repository.RutinaRepository;
import com.clubcore.api.service.RutinaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RutinaServiceImpl implements RutinaService {

    private final RutinaRepository rutinaRepository;
    private final ClienteRepository clienteRepository;
    private final RutinaMapper rutinaMapper;

    @Override
    @Transactional
    public RutinaResponse crearRutina(RutinaRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.clienteId()));

        Rutina rutina = Rutina.builder()
                .cliente(cliente)
                .nombre(request.nombre())
                .descripcion(request.descripcion())
                .fechaCreacion(request.fechaCreacion())
                .build();

        return rutinaMapper.toResponse(rutinaRepository.save(rutina));
    }

    @Override
    @Transactional(readOnly = true)
    public RutinaResponse obtenerRutina(Long id) {
        return rutinaMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RutinaResponse> listarRutinas(Pageable pageable) {
        return rutinaRepository.findAll(pageable).map(rutinaMapper::toResponse);
    }

    @Override
    @Transactional
    public void eliminarRutina(Long id) {
        if (!rutinaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Rutina", "id", id);
        }
        rutinaRepository.deleteById(id);
    }

    private Rutina findOrThrow(Long id) {
        return rutinaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rutina", "id", id));
    }
}
