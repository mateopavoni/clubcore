package com.clubcore.api.exception;

/**
 * Lanzada cuando un recurso no existe en la base de datos.
 * Mapeada a HTTP 404 por el {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String recurso, String campo, Object valor) {
        super(String.format("%s no encontrado con %s: '%s'", recurso, campo, valor));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
