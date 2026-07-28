package com.clubcore.api.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Cliente HTTP para el webhook saliente a n8n, decorado con Resilience4j.
 *
 * <p>Vive en un bean separado de {@link NotificationDispatchService} porque Spring AOP
 * intercepta proxies, no llamadas dentro del mismo bean (self-invocation problem).
 * Al delegar la llamada HTTP aquí, las anotaciones de Resilience4j son efectivas.</p>
 *
 * <h2>Orden de decoración (externo → interno)</h2>
 * <pre>
 *   CircuitBreaker (order=4) → Retry (order=5) → dispatch()
 * </pre>
 * <p>El Retry intenta hasta 3 veces con backoff exponencial. Si todos los reintentos
 * fallan, la excepción sube al CircuitBreaker, que la contabiliza en su ventana deslizante.
 * Cuando el failure-rate supera el umbral (50%), el circuito abre y las llamadas
 * siguientes van directo al fallback sin invocar el método real, evitando saturar
 * un servicio ya degradado.</p>
 *
 * <p>El fallback loguea el fallo y no relanza la excepción — preserva la semántica
 * best-effort: un n8n caído no afecta el flujo principal del negocio.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatcher {

    private final RestTemplate restTemplate;

    /**
     * Envía el {@code payload} al {@code webhookUrl} destino.
     *
     * <p>El {@code fallbackMethod} vive en {@code @Retry} (la anotación más externa), no en
     * {@code @CircuitBreaker}: si se pone en el CircuitBreaker, su fallback intercepta la
     * excepción ni bien falla el primer intento, y el Retry nunca llega a ver un fallo que
     * reintentar (bug real detectado con un test de integración — {@code postForEntity} se
     * invocaba una sola vez en vez de 3). Con el fallback en Retry: cada intento pasa primero
     * por el CircuitBreaker (que sí acumula el fallo en su ventana deslizante o corta rápido
     * si ya está abierto), y solo cuando se agotan los 3 reintentos (o el circuito ya está
     * abierto) el Retry invoca {@link #dispatchFallback}.</p>
     *
     * @param webhookUrl URL del webhook destino (n8n u otro)
     * @param payload    mapa de datos a enviar como JSON
     */
    @Retry(name = "n8nWebhook", fallbackMethod = "dispatchFallback")
    @CircuitBreaker(name = "n8nWebhook")
    public void dispatch(String webhookUrl, Map<String, Object> payload) {
        restTemplate.postForEntity(webhookUrl, payload, String.class);
        log.info("Webhook n8n OK — membresiaId={}", payload.get("membresiaId"));
    }

    /**
     * Fallback invocado cuando el circuito está abierto o todos los reintentos se agotan.
     * Loguea y retorna sin propagar la excepción.
     */
    @SuppressWarnings("unused") // referenciado por nombre en @CircuitBreaker
    void dispatchFallback(String webhookUrl, Map<String, Object> payload, Throwable cause) {
        log.error("Webhook n8n no disponible (fallback). membresiaId={}, causa: {}",
                payload.get("membresiaId"), cause.getMessage());
    }
}
