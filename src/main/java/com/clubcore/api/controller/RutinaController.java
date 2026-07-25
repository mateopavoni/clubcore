package com.clubcore.api.controller;

import com.clubcore.api.dto.request.RutinaRequest;
import com.clubcore.api.dto.response.RutinaResponse;
import com.clubcore.api.service.RutinaService;
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
@RequestMapping("/api/v1/rutinas")
@RequiredArgsConstructor
@Tag(name = "Rutinas")
@SecurityRequirement(name = "bearerAuth")
public class RutinaController {

    private final RutinaService rutinaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR', 'CLIENTE')")
    @Operation(summary = "Listar rutinas (paginado)")
    public ResponseEntity<Page<RutinaResponse>> listar(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(rutinaService.listarRutinas(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR', 'CLIENTE')")
    @Operation(summary = "Obtener rutina por ID")
    public ResponseEntity<RutinaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(rutinaService.obtenerRutina(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    @Operation(summary = "Crear rutina para un cliente")
    public ResponseEntity<RutinaResponse> crear(@Valid @RequestBody RutinaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rutinaService.crearRutina(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    @Operation(summary = "Eliminar rutina")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        rutinaService.eliminarRutina(id);
        return ResponseEntity.noContent().build();
    }
}
