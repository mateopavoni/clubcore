package com.clubcore.api.controller;

import com.clubcore.api.dto.request.LoginRequest;
import com.clubcore.api.dto.response.LoginResponse;
import com.clubcore.api.entity.Usuario;
import com.clubcore.api.repository.UsuarioRepository;
import com.clubcore.api.security.JwtService;
import com.clubcore.api.security.UserDetailsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion", description = "Login y obtención de JWT")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final UsuarioRepository usuarioRepository;

    @PostMapping("/login")
    @Operation(summary = "Login", description = "Devuelve un Bearer JWT para usar en endpoints protegidos")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Lanza BadCredentialsException si las credenciales son incorrectas,
        // que el GlobalExceptionHandler mapea a 401.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);

        Usuario usuario = usuarioRepository.findByEmail(request.email()).orElseThrow();

        return ResponseEntity.ok(new LoginResponse(token, "Bearer", usuario.getRol().name(), usuario.getId()));
    }
}
