package com.clubcore.api.controller;

import com.clubcore.api.dto.request.WebhookPagoRequest;
import com.clubcore.api.dto.response.PagoResponse;
import com.clubcore.api.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoint inbound para webhooks de sistemas externos.
 *
 * <p>Simula la recepción de la confirmación de un gateway de pago externo
 * (ej: MercadoPago, Stripe). Cuando el gateway confirma un pago, hace un POST
 * a este endpoint con la referencia y el estado, y ClubCore actualiza el registro
 * de pago y recalcula el estado de la membresía.</p>
 *
 * <p>Este endpoint NO requiere JWT para simular la llamada externa.
 * En producción se validaría con HMAC signature del gateway.</p>
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Endpoints inbound para integraciones externas")
public class WebhookController {

    private final PagoService pagoService;

    @PostMapping("/pagos")
    @Operation(
            summary = "Confirmación de pago inbound",
            description = "Recibe la notificación de un gateway de pago externo y actualiza " +
                    "el estado del pago y de la membresía asociada."
    )
    public ResponseEntity<PagoResponse> recibirConfirmacionPago(
            @Valid @RequestBody WebhookPagoRequest request) {
        return ResponseEntity.ok(pagoService.procesarWebhookPago(request));
    }
}
