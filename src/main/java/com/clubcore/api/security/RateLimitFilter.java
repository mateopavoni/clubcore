package com.clubcore.api.security;

import com.clubcore.api.dto.response.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Filtro de rate limiting aplicado sobre los endpoints públicos sin autenticación.
 *
 * <p>Implementado con el algoritmo token bucket de Bucket4j:</p>
 * <ul>
 *   <li>{@code POST /api/v1/auth/login} — 5 solicitudes/minuto por IP (protección brute-force)</li>
 *   <li>{@code /api/v1/webhooks/**}     — 20 solicitudes/minuto por IP (abuso del webhook inbound)</li>
 * </ul>
 *
 * <p>Al superar el límite, responde HTTP 429 con el mismo shape {@link ApiErrorResponse}
 * que usa {@code GlobalExceptionHandler}, garantizando una interfaz de error uniforme
 * para el cliente (el filtro no pasa por {@code @ControllerAdvice}).</p>
 *
 * <p>Extiende {@link OncePerRequestFilter} — garantiza ejecución única por request
 * incluso si Spring registra el filtro en más de un lugar de la cadena.</p>
 *
 * <p><b>Limitación conocida / upgrade path:</b></p>
 * <p>Los buckets son locales a la JVM ({@link ConcurrentHashMap} en memoria). Si la aplicación
 * escala a múltiples instancias, cada instancia tiene su propio contador y el límite efectivo
 * se multiplica por el número de réplicas. Para escala horizontal, reemplazar el mapa local
 * por un {@code ProxyManager} de Bucket4j respaldado por Redis.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    /** Tokens por minuto para el endpoint de login. */
    private static final int LOGIN_CAPACITY   = 5;
    /** Tokens por minuto para los webhooks inbound. */
    private static final int WEBHOOK_CAPACITY = 20;

    private final ObjectMapper objectMapper;

    /** Tope de IPs distintas recordadas por endpoint: acota la memoria ante un barrido de IPs. */
    private static final long MAX_TRACKED_IPS = 100_000;

    // Un bucket por IP, un caché por endpoint. A diferencia de un ConcurrentHashMap, el caché
    // expira las entradas inactivas (el bucket de una IP que no vuelve ya está recargado) y tiene
    // tope de tamaño, así que no crece sin límite. Escala a una sola instancia; para múltiples
    // réplicas ver el upgrade path en el javadoc de la clase.
    private final Cache<String, Bucket> loginBuckets   = newBucketCache();
    private final Cache<String, Bucket> webhookBuckets = newBucketCache();

    private static Cache<String, Bucket> newBucketCache() {
        return Caffeine.newBuilder()
                .maximumSize(MAX_TRACKED_IPS)
                .expireAfterAccess(Duration.ofMinutes(10))
                .build();
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path     = request.getRequestURI();
        String clientIp = extractClientIp(request);

        Bucket bucket = resolveBucket(path, clientIp);

        if (bucket == null) {
            // Ruta no sujeta a rate limiting
            filterChain.doFilter(request, response);
            return;
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit superado — ip={} path={}", clientIp, path);
            sendTooManyRequests(response, path);
        }
    }

    /**
     * Resuelve el bucket correspondiente al path, o {@code null} si la ruta
     * no está bajo rate limiting.
     */
    private Bucket resolveBucket(String path, String clientIp) {
        if ("/api/v1/auth/login".equals(path)) {
            return loginBuckets.get(clientIp, ip -> buildBucket(LOGIN_CAPACITY));
        }
        if (path.startsWith("/api/v1/webhooks/")) {
            return webhookBuckets.get(clientIp, ip -> buildBucket(WEBHOOK_CAPACITY));
        }
        return null;
    }

    /**
     * Crea un bucket con recarga por intervalo completo (batch refill).
     * Al comienzo de cada minuto se recargan todos los tokens de golpe,
     * lo que es correcto para "N requests por minuto".
     */
    private Bucket buildBucket(int capacityPerMinute) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacityPerMinute)
                .refillIntervally(capacityPerMinute, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * IP del cliente tal como la ve el contenedor de servlets ({@code getRemoteAddr()}).
     *
     * <p>Antes se tomaba el primer valor de {@code X-Forwarded-For}, pero ese header lo controla
     * quien hace la request: mandando uno distinto en cada intento cada valor caía en un bucket
     * nuevo y el límite de fuerza bruta del login no se aplicaba nunca. Detrás de un reverse proxy
     * propio, la IP real se resuelve de forma segura configurando
     * {@code server.forward-headers-strategy} (ver {@code application.yml}), que solo confía en
     * el header cuando la conexión viene de un proxy interno.</p>
     */
    private String extractClientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void sendTooManyRequests(HttpServletResponse response, String path) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                "Demasiadas solicitudes. Intentá de nuevo en un momento.",
                path
        );
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), body);
    }
}
