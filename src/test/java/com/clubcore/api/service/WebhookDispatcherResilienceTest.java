package com.clubcore.api.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifica que el Retry y el CircuitBreaker de Resilience4j realmente se ejecutan
 * (no solo que las anotaciones estén presentes en el código).
 */
@SpringBootTest
@ActiveProfiles("test")
class WebhookDispatcherResilienceTest {

    @Autowired
    private WebhookDispatcher webhookDispatcher;

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("n8nWebhook").reset();
    }

    @Test
    void retryReintenta3VecesAnteFallosTransientes() {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RestClientException("timeout simulado"));

        webhookDispatcher.dispatch("http://fake-n8n.local/webhook", Map.of("membresiaId", 1));

        // max-attempts=3 -> el RestTemplate se invoca 3 veces dentro de un solo dispatch()
        verify(restTemplate, times(3)).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    void circuitBreakerAbreTrasSuperarElUmbralDeFallos() {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RestClientException("timeout simulado"));

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("n8nWebhook");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // sliding-window-size=10, minimum-number-of-calls=5, failure-rate-threshold=50 ->
        // 10 dispatch() fallidos (cada uno ya agotó sus 3 reintentos) alcanzan de sobra el umbral.
        for (int i = 0; i < 10; i++) {
            webhookDispatcher.dispatch("http://fake-n8n.local/webhook", Map.of("membresiaId", i));
        }

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Con el circuito abierto, dispatch() debe hacer fail-fast: ni siquiera llama a restTemplate.
        int invocacionesAntes = mockingDetails(restTemplate).getInvocations().size();
        webhookDispatcher.dispatch("http://fake-n8n.local/webhook", Map.of("membresiaId", 999));
        int invocacionesDespues = mockingDetails(restTemplate).getInvocations().size();

        assertThat(invocacionesDespues).isEqualTo(invocacionesAntes);
    }
}
