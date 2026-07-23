package com.clubcore.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Plan de membresía (ej: "Mensual Básico", "Trimestral Premium").
 * Si {@code limiteReservasMes} es null, el plan tiene reservas ilimitadas por mes.
 */
@Entity
@Table(name = "planes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    /** Duración en días (30, 90, 365, etc.). */
    @Column(nullable = false)
    private int duracionDias;

    /** Null = sin límite. Cuando se agota, la membresía pasa a INACTIVA para nuevas reservas. */
    private Integer limiteReservasMes;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
