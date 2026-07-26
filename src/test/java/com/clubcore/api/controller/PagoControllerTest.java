package com.clubcore.api.controller;

import com.clubcore.api.config.SecurityConfig;
import com.clubcore.api.security.JwtService;
import com.clubcore.api.security.UserDetailsServiceImpl;
import com.clubcore.api.service.PagoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de RBAC en {@link PagoController}.
 *
 * <p>Verifica que el endpoint {@code GET /api/v1/pagos} solo sea accesible por ADMIN,
 * rechazando con 403 a CLIENTE y con 401 a requests sin autenticación.</p>
 *
 * <p>{@code @WithMockUser} establece el SecurityContext antes de la request,
 * sin pasar por el JwtFilter (no hay header Authorization en los tests).</p>
 */
@WebMvcTest(PagoController.class)
@Import(SecurityConfig.class)
@DisplayName("PagoController - RBAC (solo ADMIN)")
class PagoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PagoService pagoService;

    // Dependencias del JwtFilter cargado por @WebMvcTest
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN puede listar pagos (200)")
    void listarPagos_comoAdmin_devuelve200() throws Exception {
        when(pagoService.listarPagos(any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/v1/pagos"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    @DisplayName("CLIENTE no puede listar pagos (403)")
    void listarPagos_comoCliente_devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/pagos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ENTRENADOR")
    @DisplayName("ENTRENADOR no puede listar pagos (403)")
    void listarPagos_comoEntrenador_devuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/pagos"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Sin autenticacion devuelve 401")
    void listarPagos_sinAuth_devuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/pagos"))
                .andExpect(status().isUnauthorized());
    }
}
