package com.clubcore.api.controller;

import com.clubcore.api.dto.request.ReservaRequest;
import com.clubcore.api.dto.response.ReservaResponse;
import com.clubcore.api.service.ReservaService;
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
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas")
@SecurityRequirement(name = "bearerAuth")
public class ReservaController {

    private final ReservaService reservaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    @Operation(summary = "Listar reservas (paginado)")
    public ResponseEntity<Page<ReservaResponse>> listar(
            @PageableDefault(size = 20, sort = "fechaReserva") Pageable pageable) {
        return ResponseEntity.ok(reservaService.listarReservas(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR', 'CLIENTE')")
    @Operation(summary = "Obtener reserva por ID")
    public ResponseEntity<ReservaResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.obtenerReserva(id));
    }

    /**
     * Crea una reserva. El motor de estados verifica que la membresía esté ACTIVA;
     * si no lo está, devuelve 403.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @Operation(summary = "Crear reserva (403 si la membresía no está ACTIVA o cupo agotado)")
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody ReservaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.crearReserva(request));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLIENTE')")
    @Operation(summary = "Cancelar reserva")
    public ResponseEntity<ReservaResponse> cancelar(@PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelarReserva(id));
    }
}
