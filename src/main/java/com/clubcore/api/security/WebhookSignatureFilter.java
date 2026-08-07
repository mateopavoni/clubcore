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
    private static final String HMAC_ALGORITHM = "HmacSHA256";

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

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String expectedSignature = computeHmac(cachedRequest.getCachedBody(), signingSecret);

        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.trim().getBytes(StandardCharsets.UTF_8))) {
            log.warn("Firma de webhook inválida en {}", request.getRequestURI());
            sendUnauthorized(response, request.getRequestURI(), "Firma de webhook inválida");
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private String computeHmac(byte[] body, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // No debería ocurrir nunca: HmacSHA256 es un algoritmo estándar de la JVM y la
            // key se construye siempre con bytes válidos. Si pasa, es un bug de config real.
            throw new IllegalStateException("No se pudo calcular el HMAC del webhook", e);
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String path, String message) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                path
        );
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
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
    private static class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
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
