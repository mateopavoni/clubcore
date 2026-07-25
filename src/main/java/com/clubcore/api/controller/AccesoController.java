package com.clubcore.api.controller;

import com.clubcore.api.dto.request.AccesoRequest;
import com.clubcore.api.dto.response.AccesoResponse;
import com.clubcore.api.exception.MembresiaInvalidaException;
import com.clubcore.api.service.AccesoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accesos")
@RequiredArgsConstructor
@Tag(name = "Accesos", description = "Check-in al complejo")
@SecurityRequirement(name = "bearerAuth")
public class AccesoController {

    private final AccesoService accesoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    @Operation(summary = "Listar accesos (paginado)")
    public ResponseEntity<Page<AccesoResponse>> listar(
            @PageableDefault(size = 20, sort = "fechaAcceso") Pageable pageable) {
        return ResponseEntity.ok(accesoService.listarAccesos(pageable));
    }

    /**
     * Registra un check-in. Si la membresía no está ACTIVA, devuelve 403
     * para que el torniquete/sistema físico sepa que debe negar el acceso.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    @Operation(summary = "Check-in (403 si la membresía no está ACTIVA)")
    public ResponseEntity<AccesoResponse> registrar(@Valid @RequestBody AccesoRequest request) {
        AccesoResponse respuesta = accesoService.registrarAcceso(request);

        // El acceso se registra siempre (para auditoría).
        // Si fue rechazado, respondemos 403 al llamador (torniquete/recepción).
        if (!respuesta.exitoso()) {
            throw new MembresiaInvalidaException(
                    "Acceso denegado: " + respuesta.motivoRechazo()
            );
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
