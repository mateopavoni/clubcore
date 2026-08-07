package com.clubcore.api.config;

import com.clubcore.api.dto.response.ApiErrorResponse;
import com.clubcore.api.security.JwtFilter;
import com.clubcore.api.security.RateLimitFilter;
import com.clubcore.api.security.WebhookSignatureFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuración central de Spring Security.
 *
 * <ul>
 *   <li>JWT stateless (sin sesiones)</li>
 *   <li>CORS acotado — configurable por entorno</li>
 *   <li>CSRF deshabilitado (API REST, no HTML forms)</li>
 *   <li>RBAC a nivel de método con {@code @PreAuthorize} (habilitado con {@code @EnableMethodSecurity})</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtFilter                jwtFilter;
    private final RateLimitFilter          rateLimitFilter;
    private final WebhookSignatureFilter   webhookSignatureFilter;
    private final ObjectMapper             objectMapper;
    private final Environment              environment;

    /** {@code CORS_ALLOWED_ORIGINS}, coma-separado. Ver {@link #corsConfigurationSource()}. */
    @Value("${cors.allowed-origins:}")
    private String corsAllowedOriginsRaw;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/webhooks/**",    // Inbound externo — sin JWT, pero exige firma HMAC (ver WebhookSignatureFilter)
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health",       // healthcheck Docker/Dokku — público intencionalmente
            "/actuator/info"          // commit/build info — no expone datos sensibles
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // /actuator/prometheus y /actuator/metrics requieren ADMIN.
                        // NO van en PUBLIC_ENDPOINTS — exponen métricas internas sensibles.
                        .requestMatchers("/actuator/prometheus", "/actuator/metrics/**")
                                .hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // 401 para requests sin autenticacion (por default Spring usa 403 sin entry point)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authException) ->
                                writeError(res, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado", req.getRequestURI()))
                        // Sin esto, un usuario autenticado pero sin el rol requerido en una regla
                        // .requestMatchers(...).hasRole(...) a nivel de filtro (ej. /actuator/**) cae
                        // en el authenticationEntryPoint de arriba (401) en vez de un 403 — a diferencia
                        // de un @PreAuthorize de método, que sí llega al GlobalExceptionHandler porque
                        // esa excepción se lanza dentro del DispatcherServlet, no en la cadena de filtros.
                        .accessDeniedHandler((req, res, deniedException) ->
                                writeError(res, HttpServletResponse.SC_FORBIDDEN, "Acceso denegado", req.getRequestURI())))
                // Orden de filtros: RateLimitFilter → JwtFilter → WebhookSignatureFilter →
                // UsernamePasswordAuthenticationFilter. Los tres se registran "antes de" UPAF.
                // Al tener el mismo orden relativo, Java's stable sort preserva el orden de
                // inserción de este bloque. addFilterBefore(f, JwtFilter.class) no funciona
                // porque JwtFilter no es un filtro "conocido" de Spring Security (no está en
                // FilterOrderRegistration).
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(webhookSignatureFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Mismo shape {@link ApiErrorResponse} que usa {@code GlobalExceptionHandler}, para que
     * un 401/403 originado en la cadena de filtros (fuera del alcance del @ControllerAdvice)
     * sea indistinguible del que devuelve un @PreAuthorize a nivel de método.
     */
    private void writeError(HttpServletResponse res, int status, String message, String path) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        ApiErrorResponse body = new ApiErrorResponse(
                java.time.LocalDateTime.now(), status,
                org.springframework.http.HttpStatus.valueOf(status).getReasonPhrase(),
                message, path);
        objectMapper.writeValue(res.getWriter(), body);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS: los orígenes permitidos vienen de la variable de entorno {@code CORS_ALLOWED_ORIGINS}
     * (lista separada por comas), usando {@code setAllowedOrigins} (lista explícita) — nunca
     * {@code setAllowedOriginPatterns(List.of("*"))}, que es justamente lo que le permite a
     * Spring Security saltarse su rechazo normal de "wildcard origin + credentials".
     *
     * <p>Si la variable no está seteada:</p>
     * <ul>
     *   <li>Perfil {@code prod}: falla rápido en el arranque (bean no se puede construir) —
     *       mejor un deploy roto y visible que CORS abierto en producción sin que nadie lo note.</li>
     *   <li>Cualquier otro perfil (dev/test): cae a {@code "*"} vía {@code setAllowedOriginPatterns}
     *       (única combinación válida con credentials + wildcard) y loguea un warning.</li>
     * </ul>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedOrigins = parseAllowedOrigins(corsAllowedOriginsRaw);

        if (allowedOrigins.isEmpty()) {
            if (environment.matchesProfiles("prod")) {
                throw new IllegalStateException(
                        "CORS_ALLOWED_ORIGINS no está configurado. En el perfil 'prod' es obligatorio " +
                        "definir explícitamente los orígenes permitidos (no se admite '*' con credenciales).");
            }
            log.warn("CORS_ALLOWED_ORIGINS no configurado — usando '*' solo por no estar en perfil 'prod'");
            config.setAllowedOriginPatterns(List.of("*"));
        } else {
            config.setAllowedOrigins(allowedOrigins);
        }

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private List<String> parseAllowedOrigins(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(origen -> !origen.isEmpty())
                .toList();
    }
}
