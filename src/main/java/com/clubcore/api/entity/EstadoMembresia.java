package com.clubcore.api.entity;

/**
 * Estados posibles de una membresía.
 *
 * <p>El estado real se calcula on-demand por {@code MembresiaStateService.evaluarEstado()};
 * nunca se escribe directamente desde un controller. El campo {@code estadoCacheado}
 * en la entidad {@link Membresia} lo persiste el propio service para consultas rápidas.</p>
 *
 * <ul>
 *   <li>ACTIVA       — vigente, sin deuda, cupo disponible, no suspendida</li>
 *   <li>INACTIVA     — fecha vencida O cupo del mes agotado</li>
 *   <li>DEUDA        — vigente temporalmente pero con pagos pendientes</li>
 *   <li>SUSPENDIDA   — suspensión manual por un ADMIN (máxima prioridad)</li>
 * </ul>
 */
public enum EstadoMembresia {
    ACTIVA,
    INACTIVA,
    DEUDA,
    SUSPENDIDA
}
