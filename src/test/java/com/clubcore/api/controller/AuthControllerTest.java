package com.clubcore.api.controller;

import com.clubcore.api.config.SecurityConfig;
import com.clubcore.api.entity.Rol;
import com.clubcore.api.entity.Usuario;
import com.clubcore.api.repository.UsuarioRepository;
import com.clubcore.api.security.JwtService;
import com.clubcore.api.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController - Login y JWT")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Login con credenciales validas devuelve 200 con JWT")
    void login_credencialesValidas_devuelve200ConToken() throws Exception {
        // El AuthenticationManager no lanza excepción = autenticación exitosa
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        "admin@clubcore.com", "Admin1234!"));

        // UserDetailsService carga el usuario para generar el token
        var mockUserDetails = User.builder()
                .username("admin@clubcore.com")
                .password("hashed")
                .roles("ADMIN")
                .build();
        when(userDetailsService.loadUserByUsername("admin@clubcore.com"))
                .thenReturn(mockUserDetails);

        when(jwtService.generateToken(any())).thenReturn("eyJhbGciOiJIUzI1NiJ9.mock.token");

        var admin = Usuario.builder()
                .id(1L)
                .email("admin@clubcore.com")
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
        when(usuarioRepository.findByEmail("admin@clubcore.com")).thenReturn(Optional.of(admin));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "admin@clubcore.com", "password": "Admin1234!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("eyJhbGciOiJIUzI1NiJ9.mock.token"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.usuarioId").value(1));
    }

    @Test
    @DisplayName("Login con credenciales invalidas devuelve 401")
    void login_credencialesInvalidas_devuelve401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales invalidas"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "hacker@test.com", "password": "wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Credenciales invalidas"));
    }

    @Test
    @DisplayName("Login sin email devuelve 400 con mensaje de validacion")
    void login_emailBlanco_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "", "password": "Admin1234!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Login sin body devuelve 400")
    void login_sinBody_devuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
