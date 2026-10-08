package com.clubcore.api.security;

import com.clubcore.api.dto.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;

/**
 * Verifica la firma HMAC-SHA256 del webhook inbound de pagos ({@code POST /api/v1/webhooks/pagos}).
 *
 * <p>El endpoint no requiere JWT — simula la llamada de un gateway de pago externo
 * (MercadoPago/Stripe) que obviamente no tiene un token nuestro. Pero eso NO significa que
 * cualquiera pueda invocarlo: antes de este fix, un atacante podía hacer
 * {@code POST /api/v1/webhooks/pagos} con {@code estado=PAGADO} para cualquier
 * {@code membresiaId} y activarla gratis, sin autenticación de ningún tipo.</p>
 *
 * <p>Ahora el caller debe conocer el secreto compartido {@code WEBHOOK_SIGNING_SECRET} y enviar
 * el header {@code X-Webhook-Signature} con el HMAC-SHA256 (hex) del cuerpo crudo de la request,
 * calculado con ese secreto. La comparación usa {@link MessageDigest#isEqual} (constant-time)
 * para no filtrar información por timing.</p>
 *
 * <p>Para poder leer el body crudo (firma) y que Jackson lo pueda deserializar después
 * ({@code WebhookPagoRequest}), la request se envuelve en {@link CachedBodyHttpServletRequest},
 * que bufferea los bytes una sola vez y permite leerlos más de una vez.</p>
 */
@Component
@Slf4j
public class WebhookSignatureFilter extends OncePerRequestFilter {

    private static final String WEBHOOK_PATH = "/api/v1/webhooks/pagos";
    private static final String SIGNATURE_HEADER = "X-Webhook-Signature";
    private static final String TIMESTAMP_HEADER = "X-Webhook-Timestamp";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Ventana de validez de una request firmada. Sin timestamp en lo firmado, una request capturada
     * (ej. un PAGADO viejo) se podía reenviar para siempre y revertía el estado de un pago que
     * después pasó a FALLIDO.
     */
    private static final long TOLERANCE_SECONDS = 300;

    /** Tope del body, aplicado antes de leerlo entero: el endpoint es público y el body se bufferea. */
    private static final int MAX_BODY_BYTES = 64 * 1024;

    private final ObjectMapper objectMapper;
    private final String signingSecret;

    public WebhookSignatureFilter(ObjectMapper objectMapper,
                                   @Value("${webhook.signing-secret:}") String signingSecret) {
        this.objectMapper = objectMapper;
        this.signingSecret = signingSecret;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // Solo aplica al endpoint del webhook inbound — el resto de rutas públicas
        // (auth, swagger, actuator/health) no llevan firma HMAC.
        return !WEBHOOK_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (signingSecret == null || signingSecret.isBlank()) {
            log.error("WEBHOOK_SIGNING_SECRET no configurado — rechazando toda request a {}", WEBHOOK_PATH);
            sendUnauthorized(response, request.getRequestURI(), "Webhook no configurado");
            return;
        }

        String providedSignature = request.getHeader(SIGNATURE_HEADER);
        if (providedSignature == null || providedSignature.isBlank()) {
            log.warn("Webhook inbound sin header {} — rechazado", SIGNATURE_HEADER);
            sendUnauthorized(response, request.getRequestURI(), "Falta la firma del webhook");
            return;
        }

        String timestamp = request.getHeader(TIMESTAMP_HEADER);
        if (timestamp == null || timestamp.isBlank()) {
            log.warn("Webhook inbound sin header {} — rechazado", TIMESTAMP_HEADER);
            sendError(response, HttpStatus.UNAUTHORIZED, request.getRequestURI(), "Falta el timestamp del webhook");
            return;
        }
        if (!withinTolerance(timestamp.trim())) {
            log.warn("Webhook inbound con timestamp fuera de la ventana de {}s — rechazado", TOLERANCE_SECONDS);
            sendError(response, HttpStatus.UNAUTHORIZED, request.getRequestURI(), "Timestamp del webhook fuera de la ventana permitida");
            return;
        }

        if (request.getContentLengthLong() > MAX_BODY_BYTES) {
            sendError(response, HttpStatus.PAYLOAD_TOO_LARGE, request.getRequestURI(), "Body demasiado grande");
            return;
        }
        CachedBodyHttpServletRequest cachedRequest;
        try {
            cachedRequest = new CachedBodyHttpServletRequest(request, MAX_BODY_BYTES);
        } catch (BodyTooLargeException e) {
            sendError(response, HttpStatus.PAYLOAD_TOO_LARGE, request.getRequestURI(), "Body demasiado grande");
            return;
        }

        // Se firma "<timestamp>.<body>": el timestamp forma parte de lo firmado, así que no se puede
        // actualizar en una request capturada sin invalidar la firma.
        String expectedSignature = computeHmac(timestamp.trim(), cachedRequest.getCachedBody(), signingSecret);

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.trim().getBytes(StandardCharsets.UTF_8))) {
            log.warn("Firma de webhook inválida en {}", request.getRequestURI());
            sendUnauthorized(response, request.getRequestURI(), "Firma de webhook inválida");
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private static boolean withinTolerance(String timestamp) {
        try {
            long sent = Long.parseLong(timestamp);
            return Math.abs(Instant.now().getEpochSecond() - sent) <= TOLERANCE_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String computeHmac(String timestamp, byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // No debería ocurrir nunca: HmacSHA256 es un algoritmo estándar de la JVM y la
            // key se construye siempre con bytes válidos. Si pasa, es un bug de config real.
            throw new IllegalStateException("No se pudo calcular el HMAC del webhook", e);
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String path, String message) throws IOException {
        sendError(response, HttpStatus.UNAUTHORIZED, path, message);
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String path, String message) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }

    /**
     * Envuelve la request HTTP para bufferear el body crudo una sola vez y permitir leerlo
     * más de una vez: primero acá para calcular el HMAC, después en
     * {@code RequestResponseBodyMethodProcessor} de Spring MVC para deserializar el JSON con
     * Jackson. Sin esto, el segundo intento de lectura del stream original devolvería vacío.
     */
    private static class BodyTooLargeException extends IOException {
    }

    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request, int maxBytes) throws IOException {
            super(request);
            // Se lee como mucho maxBytes + 1: si llega a maxBytes + 1 el body excede el tope y se corta
            // acá, sin bufferear un body arbitrariamente grande (el endpoint es público).
            byte[] bytes = request.getInputStream().readNBytes(maxBytes + 1);
            if (bytes.length > maxBytes) {
                throw new BodyTooLargeException();
            }
            this.cachedBody = bytes;
        }

        byte[] getCachedBody() {
            return cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream buffer = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return buffer.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // no-op: lectura síncrona, no se usa I/O asíncrono en este endpoint
                }

                @Override
                public int read() {
                    return buffer.read();
                }
            };
        }
    }
}
