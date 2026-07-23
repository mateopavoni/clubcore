package com.clubcore.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Membresía de un cliente a un plan.
 *
 * <p><b>Motor de estados:</b> el campo {@code estadoCacheado} es una copia derivada
 * calculada por {@code MembresiaStateService}. Nunca se escribe directamente desde
 * un controller — solo el service lo actualiza vía
 * {@code MembresiaStateService.actualizarEstadoCacheado()}.</p>
 *
 * <p>El estado real siempre se evalúa on-demand antes de tomar decisiones críticas
 * (check-in, nueva reserva).</p>
 */
@Entity
@Table(name = "membresias")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Membresia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaVencimiento;

    /**
     * Estado cacheado: se actualiza por {@code MembresiaStateService}, nunca a mano.
     * Útil para queries de listado sin recalcular para cada fila.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoMembresia estadoCacheado = EstadoMembresia.ACTIVA;

    /**
     * Suspensión manual por un ADMIN.
     * Cuando es true, el estado es SUSPENDIDA sin importar los demás factores.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean suspendida = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
