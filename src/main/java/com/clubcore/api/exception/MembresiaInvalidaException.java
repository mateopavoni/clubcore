package com.clubcore.api.exception;

import com.clubcore.api.entity.EstadoMembresia;

/**
 * Lanzada cuando se intenta realizar una operación (check-in, reserva) y la membresía
 * no está en estado ACTIVA.
 * Mapeada a HTTP 403 por el {@link GlobalExceptionHandler}.
 */
public class MembresiaInvalidaException extends RuntimeException {

    public MembresiaInvalidaException(EstadoMembresia estadoActual, String operacion) {
        super(String.format(
                "No se puede realizar '%s': la membresía está en estado %s",
                operacion, estadoActual
        ));
    }

    public MembresiaInvalidaException(String message) {
        super(message);
    }
}
