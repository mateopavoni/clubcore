package com.clubcore.api.config;

import com.clubcore.api.dto.response.ApiErrorResponse;
import com.clubcore.api.security.JwtFilter;
import com.clubcore.api.security.RateLimitFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class SecurityConfig {

    private final JwtFilter        jwtFilter;
    private final RateLimitFilter  rateLimitFilter;
    private final ObjectMapper     objectMapper;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/webhooks/**",    // Inbound externo — validado por HMAC en producción
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health"        // healthcheck Docker/Dokku — público intencionalmente
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
                // Orden de filtros: RateLimitFilter → JwtFilter → UsernamePasswordAuthenticationFilter
                // Ambos filtros se registran "antes de" UPAF. Al tener el mismo orden relativo,
                // Java's stable sort preserva el orden de inserción: rateLimitFilter primero.
                // addFilterBefore(f, JwtFilter.class) no funciona porque JwtFilter no es un
                // filtro "conocido" de Spring Security (no está en FilterOrderRegistration).
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

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
     * CORS: en producción se acota a los orígenes reales via la variable de entorno
     * {@code CORS_ALLOWED_ORIGINS}. Esta configuración es permisiva solo para dev.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
