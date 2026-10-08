package com.clubcore.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RateLimitFilterTest {

    private final RateLimitFilter filter =
            new RateLimitFilter(new ObjectMapper().registerModule(new JavaTimeModule()));
    private final FilterChain chain = mock(FilterChain.class);

    private int login(String remoteAddr, String xForwardedFor) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        req.setRequestURI("/api/v1/auth/login");
        req.setRemoteAddr(remoteAddr);
        if (xForwardedFor != null) {
            req.addHeader("X-Forwarded-For", xForwardedFor);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, chain);
        return res.getStatus();
    }

    @Test
    void sextoLoginDesdeLaMismaIpDevuelve429() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertThat(login("10.0.0.1", null)).isEqualTo(200);
        }
        assertThat(login("10.0.0.1", null)).isEqualTo(429);
    }

    @Test
    void rotarXForwardedForNoEvadeElLimite() throws Exception {
        // Un atacante manda un X-Forwarded-For distinto en cada intento: antes cada valor
        // caía en un bucket nuevo y el límite de fuerza bruta no se aplicaba nunca.
        for (int i = 0; i < 5; i++) {
            login("10.0.0.2", "203.0.113." + i);
        }
        assertThat(login("10.0.0.2", "203.0.113.99")).isEqualTo(429);
    }

    @Test
    void otraIpTieneSuPropioLimite() throws Exception {
        for (int i = 0; i < 6; i++) {
            login("10.0.0.3", null);
        }
        assertThat(login("10.0.0.4", null)).isEqualTo(200);
    }
}
