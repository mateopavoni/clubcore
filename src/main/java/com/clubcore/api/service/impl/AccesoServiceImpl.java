package com.clubcore.api.service.impl;

import com.clubcore.api.dto.request.AccesoRequest;
import com.clubcore.api.dto.response.AccesoResponse;
import com.clubcore.api.entity.*;
import com.clubcore.api.exception.ResourceNotFoundException;
import com.clubcore.api.mapper.AccesoMapper;
import com.clubcore.api.repository.AccesoRepository;
import com.clubcore.api.repository.ClienteRepository;
import com.clubcore.api.repository.MembresiaRepository;
import com.clubcore.api.service.AccesoService;
import com.clubcore.api.service.MembresiaStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccesoServiceImpl implements AccesoService {

    private final AccesoRepository accesoRepository;
    private final ClienteRepository clienteRepository;
    private final MembresiaRepository membresiaRepository;
    private final AccesoMapper accesoMapper;
    private final MembresiaStateService membresiaStateService;

    /**
     * Registra un intento de check-in.
     *
     * <p>Siempre guarda el acceso (exitoso o no) para auditoría. Si la membresía
     * no está ACTIVA, el acceso se registra con {@code exitoso=false} y el
     * {@code motivoRechazo} correspondiente, y se devuelve HTTP 200 al controller
     * que decidirá si devolver 200 o 403 según la respuesta.</p>
     */
    @Override
    @Transactional
    public AccesoResponse registrarAcceso(AccesoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", "id", request.clienteId()));

        Optional<Membresia> membresiaOpt = membresiaRepository
                .findFirstByClienteIdOrderByFechaVencimientoDesc(request.clienteId());

        Acceso acceso;

        if (membresiaOpt.isEmpty()) {
            acceso = Acceso.builder()
                    .cliente(cliente)
                    .membresia(null)
                    .fechaAcceso(LocalDateTime.now())
                    .exitoso(false)
                    .motivoRechazo("SIN_MEMBRESIA")
                    .build();
        } else {
            Membresia membresia = membresiaOpt.get();
            EstadoMembresia estado = membresiaStateService.evaluarEstado(membresia);

            boolean permitido = estado == EstadoMembresia.ACTIVA;
            acceso = Acceso.builder()
                    .cliente(cliente)
                    .membresia(membresia)
                    .fechaAcceso(LocalDateTime.now())
                    .exitoso(permitido)
                    .motivoRechazo(permitido ? null : "MEMBRESIA_" + estado.name())
                    .build();
        }

        return accesoMapper.toResponse(accesoRepository.save(acceso));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AccesoResponse> listarAccesos(Pageable pageable) {
        return accesoRepository.findAll(pageable).map(accesoMapper::toResponse);
    }
}
