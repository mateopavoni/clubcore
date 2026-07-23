package com.clubcore.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de check-in al complejo.
 * Si el check-in fue rechazado (exitoso=false), se guarda el motivo para auditoría.
 */
@Entity
@Table(name = "accesos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Acceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    /** La membresía activa en el momento del intento (puede ser null si no tenía ninguna). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_id")
    private Membresia membresia;

    @Column(nullable = false)
    private LocalDateTime fechaAcceso;

    @Column(nullable = false)
    private boolean exitoso;

    /** Razón del rechazo si exitoso=false (ej: "MEMBRESIA_SUSPENDIDA", "MEMBRESIA_VENCIDA"). */
    @Column(length = 200)
    private String motivoRechazo;
}
