package com.clubcore.api.service;

import com.clubcore.api.dto.request.PagoRequest;
import com.clubcore.api.dto.request.WebhookPagoRequest;
import com.clubcore.api.dto.response.PagoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PagoService {

    PagoResponse crearPago(PagoRequest request);

    PagoResponse obtenerPago(Long id);

    Page<PagoResponse> listarPagos(Pageable pageable);

    /** Procesa la confirmación inbound de un gateway de pago externo. */
    PagoResponse procesarWebhookPago(WebhookPagoRequest request);
}
