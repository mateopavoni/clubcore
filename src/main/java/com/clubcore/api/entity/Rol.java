package com.clubcore.api.entity;

/**
 * Roles del sistema. Se usa tanto en la entidad Usuario como en las
 * anotaciones @PreAuthorize de los controllers.
 */
public enum Rol {
    ADMIN,
    ENTRENADOR,
    CLIENTE
}
