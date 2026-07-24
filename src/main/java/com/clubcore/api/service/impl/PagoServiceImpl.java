package com.clubcore.api.service.impl;

import com.clubcore.api.dto.request.PagoRequest;
import com.clubcore.api.dto.request.WebhookPagoRequest;
import com.clubcore.api.dto.response.PagoResponse;
import com.clubcore.api.entity.EstadoPago;
import com.clubcore.api.entity.Membresia;
import com.clubcore.api.entity.Pago;
import com.clubcore.api.exception.ResourceNotFoundException;
import com.clubcore.api.mapper.PagoMapper;
import com.clubcore.api.repository.MembresiaRepository;
import com.clubcore.api.repository.PagoRepository;
import com.clubcore.api.service.MembresiaStateService;
import com.clubcore.api.service.PagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final MembresiaRepository membresiaRepository;
    private final PagoMapper pagoMapper;
    private final MembresiaStateService membresiaStateService;

    @Override
    @Transactional
    public PagoResponse crearPago(PagoRequest request) {
        Membresia membresia = membresiaRepository.findById(request.membresiaId())
                .orElseThrow(() -> new ResourceNotFoundException("Membresía", "id", request.membresiaId()));

        Pago pago = Pago.builder()
                .membresia(membresia)
                .monto(request.monto())
                .fechaPago(LocalDateTime.now())
                .estado(request.estado())
                .referencia(request.referencia())
                .build();

        Pago guardado = pagoRepository.save(pago);

        // Después de crear un pago, el estado de la membresía puede cambiar
        membresiaStateService.actualizarEstadoCacheado(membresia);

        return pagoMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse obtenerPago(Long id) {
        return pagoMapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PagoResponse> listarPagos(Pageable pageable) {
        return pagoRepository.findAll(pageable).map(pagoMapper::toResponse);
    }

    /**
     * Procesa la confirmación de pago recibida por webhook inbound.
     *
     * <p>Si ya existe un pago con la misma referencia, actualiza su estado.
     * Si no existe, crea uno nuevo. En ambos casos, recalcula el estado
     * de la membresía asociada.</p>
     */
    @Override
    @Transactional
    public PagoResponse procesarWebhookPago(WebhookPagoRequest request) {
        Membresia membresia = membresiaRepository.findById(request.membresiaId())
                .orElseThrow(() -> new ResourceNotFoundException("Membresía", "id", request.membresiaId()));

        Pago pago = pagoRepository.findByReferencia(request.referencia())
                .orElse(Pago.builder()
                        .membresia(membresia)
                        .monto(request.monto())
                        .fechaPago(LocalDateTime.now())
                        .referencia(request.referencia())
                        .build());

        pago.setEstado(request.estado());

        Pago guardado = pagoRepository.save(pago);

        // Si el pago fue confirmado, el estado de la membresía puede pasar de DEUDA a ACTIVA
        if (request.estado() == EstadoPago.PAGADO) {
            membresiaStateService.actualizarEstadoCacheado(membresia);
        }

        return pagoMapper.toResponse(guardado);
    }

    private Pago findOrThrow(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", "id", id));
    }
}
