package com.clubcore.api.service;

import com.clubcore.api.entity.Membresia;
import com.clubcore.api.event.MembresiaPorVencerEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Dispatcher de notificaciones salientes.
 *
 * <p>Escucha el {@link MembresiaPorVencerEvent} publicado por el job diario y,
 * en un thread async separado ({@code @Async}), delega el POST HTTP a
 * {@link WebhookDispatcher}, que aplica Resilience4j (retry + circuit breaker).</p>
 *
 * <p>El webhook destino (típicamente n8n) puede entonces enviar un WhatsApp / email /
 * notificación push al cliente. El payload es JSON con los datos de la membresía.</p>
 *
 * <p>Si {@code N8N_WEBHOOK_URL} no está configurada, el evento se loguea y se descarta
 * sin lanzar excepción — el disparo sigue siendo best-effort.</p>
 *
 * <p><b>Separación de responsabilidades:</b> este servicio construye el payload y
 * coordina el flujo async. {@link WebhookDispatcher} tiene la responsabilidad de
 * la resiliencia HTTP (retry, circuit breaker, fallback). La separación es
 * necesaria porque Spring AOP no intercepta llamadas desde el mismo bean
 * (self-invocation problem).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatchService {

    @Value("${n8n.webhook-url:}")
    private String n8nWebhookUrl;

    private final WebhookDispatcher webhookDispatcher;

    @Async("taskExecutor")
    @EventListener
    public void onMembresiaPorVencer(MembresiaPorVencerEvent event) {
        Membresia membresia = event.getMembresia();

        if (n8nWebhookUrl == null || n8nWebhookUrl.isBlank()) {
            log.warn("N8N_WEBHOOK_URL no configurada. Evento de vencimiento para membresía {} omitido.",
                    membresia.getId());
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("evento", "MEMBRESIA_POR_VENCER");
        payload.put("membresiaId", membresia.getId());
        payload.put("clienteId", membresia.getCliente().getId());
        payload.put("clienteEmail", membresia.getCliente().getEmail());
        payload.put("clienteNombre",
                membresia.getCliente().getNombre() + " " + membresia.getCliente().getApellido());
        payload.put("fechaVencimiento", membresia.getFechaVencimiento().toString());
        payload.put("plan", membresia.getPlan().getNombre());

        // La resiliencia (retry + circuit breaker + fallback) vive en WebhookDispatcher.
        // Desde aquí solo se dispara — si falla, el fallback loguea y absorbe la excepción.
        webhookDispatcher.dispatch(n8nWebhookUrl, payload);
    }
}
