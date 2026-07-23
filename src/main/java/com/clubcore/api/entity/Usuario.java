package com.clubcore.api.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Credenciales de acceso al sistema. Un Cliente tiene exactamente un Usuario.
 * La contraseña siempre se persiste hasheada con BCrypt.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    /** BCrypt hash — nunca texto plano. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
