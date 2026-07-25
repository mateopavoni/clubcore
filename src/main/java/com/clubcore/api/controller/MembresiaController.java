package com.clubcore.api.controller;

import com.clubcore.api.dto.request.MembresiaRequest;
import com.clubcore.api.dto.response.MembresiaResponse;
import com.clubcore.api.service.MembresiaService;
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
@RequestMapping("/api/v1/membresias")
@RequiredArgsConstructor
@Tag(name = "Membresias")
@SecurityRequirement(name = "bearerAuth")
public class MembresiaController {

    private final MembresiaService membresiaService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar membresías (paginado)")
    public ResponseEntity<Page<MembresiaResponse>> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(membresiaService.listarMembresias(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    @Operation(summary = "Obtener membresía por ID")
    public ResponseEntity<MembresiaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(membresiaService.obtenerMembresia(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear membresía para un cliente")
    public ResponseEntity<MembresiaResponse> crear(@Valid @RequestBody MembresiaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(membresiaService.crearMembresia(request));
    }

    @PatchMapping("/{id}/suspender")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspender o reactivar una membresía",
            description = "suspendida=true suspende, suspendida=false reactiva")
    public ResponseEntity<MembresiaResponse> suspender(
            @PathVariable Long id,
            @RequestParam boolean suspendida) {
        return ResponseEntity.ok(membresiaService.suspenderMembresia(id, suspendida));
    }
}
