package com.clubcore.api.controller;

import com.clubcore.api.dto.request.PlanRequest;
import com.clubcore.api.dto.response.PlanResponse;
import com.clubcore.api.service.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planes")
@RequiredArgsConstructor
@Tag(name = "Planes")
@SecurityRequirement(name = "bearerAuth")
public class PlanController {

    private final PlanService planService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR', 'CLIENTE')")
    @Operation(summary = "Listar planes activos")
    public ResponseEntity<List<PlanResponse>> listar() {
        return ResponseEntity.ok(planService.listarPlanesActivos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR', 'CLIENTE')")
    @Operation(summary = "Obtener plan por ID")
    public ResponseEntity<PlanResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(planService.obtenerPlan(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear plan (solo ADMIN)")
    public ResponseEntity<PlanResponse> crear(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.crearPlan(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar plan (solo ADMIN)")
    public ResponseEntity<PlanResponse> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.actualizarPlan(id, request));
    }
}
