package com.clubcore.api.controller;

import com.clubcore.api.config.SecurityConfig;
import com.clubcore.api.dto.response.PagoResponse;
import com.clubcore.api.entity.EstadoPago;
import com.clubcore.api.security.JwtService;
import com.clubcore.api.security.UserDetailsServiceImpl;
import com.clubcore.api.service.PagoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fix 1 (webhook inbound sin autenticación): verifica que
 * {@code POST /api/v1/webhooks/pagos} exija una firma HMAC-SHA256 válida en el header
 * {@code X-Webhook-Signature}, calculada sobre el body crudo con
 * {@code webhook.signing-secret} (ver {@code application-test.yml}).
 *
 * <p>Antes del fix, cualquiera podía hacer POST con {@code estado=PAGADO} para cualquier
 * {@code membresiaId} sin ningún tipo de autenticación.</p>
 */
@WebMvcTest(WebhookController.class)
@Import(SecurityConfig.class)
@DisplayName("WebhookController - firma HMAC del webhook inbound")
class WebhookControllerTest {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String BODY = """
            {"referencia":"ref-1","membresiaId":1,"monto":100.00,"estado":"PAGADO"}""";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagoService pagoService;

    // Dependencias del JwtFilter/RateLimitFilter/WebhookSignatureFilter cargados por @WebMvcTest
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Value("${webhook.signing-secret}")
    private String signingSecret;

    @Test
    @DisplayName("Con firma HMAC válida, procesa el webhook (200)")
    void firmaValida_procesaWebhook() throws Exception {
        when(pagoService.procesarWebhookPago(any())).thenReturn(
                new PagoResponse(1L, 1L, BigDecimal.valueOf(100.00), null, EstadoPago.PAGADO, "ref-1"));

        mockMvc.perform(post("/api/v1/webhooks/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", firmar(BODY))
                        .content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Sin header de firma, rechaza con 401")
    void sinHeaderDeFirma_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con firma inválida, rechaza con 401")
    void firmaInvalida_devuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/webhooks/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", "0".repeat(64))
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Con firma calculada sobre un body distinto (tampering), rechaza con 401")
    void firmaDeOtroBody_devuelve401() throws Exception {
        String otroBody = """
                {"referencia":"ref-1","membresiaId":1,"monto":999999.00,"estado":"PAGADO"}""";

        mockMvc.perform(post("/api/v1/webhooks/pagos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Signature", firmar(otroBody))
                        .content(BODY))
                .andExpect(status().isUnauthorized());
    }

    private String firmar(String body) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
