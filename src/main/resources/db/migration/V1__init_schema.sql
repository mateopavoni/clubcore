-- V1: Schema inicial de ClubCore
-- Orden de creación respeta dependencias de clave foránea.

CREATE TABLE usuarios (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    email    VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol      VARCHAR(20)  NOT NULL,
    activo   TINYINT(1)   NOT NULL DEFAULT 1
);

CREATE TABLE clientes (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    apellido         VARCHAR(100) NOT NULL,
    email            VARCHAR(150) NOT NULL UNIQUE,
    telefono         VARCHAR(30),
    fecha_nacimiento DATE,
    usuario_id       BIGINT UNIQUE,
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    CONSTRAINT fk_clientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE TABLE planes (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre               VARCHAR(100)   NOT NULL,
    descripcion          VARCHAR(500),
    precio               DECIMAL(10, 2) NOT NULL,
    duracion_dias        INT            NOT NULL,
    limite_reservas_mes  INT,               -- NULL = sin límite
    activo               TINYINT(1)     NOT NULL DEFAULT 1
);

CREATE TABLE membresias (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id       BIGINT      NOT NULL,
    plan_id          BIGINT      NOT NULL,
    fecha_inicio     DATE        NOT NULL,
    fecha_vencimiento DATE       NOT NULL,
    estado_cacheado  VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    suspendida       TINYINT(1)  NOT NULL DEFAULT 0,
    created_at       DATETIME(6),
    updated_at       DATETIME(6),
    CONSTRAINT fk_membresias_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_membresias_plan    FOREIGN KEY (plan_id)    REFERENCES planes (id)
);

CREATE TABLE pagos (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    membresia_id BIGINT         NOT NULL,
    monto        DECIMAL(10, 2) NOT NULL,
    fecha_pago   DATETIME(6)    NOT NULL,
    estado       VARCHAR(20)    NOT NULL,
    referencia   VARCHAR(200),
    CONSTRAINT fk_pagos_membresia FOREIGN KEY (membresia_id) REFERENCES membresias (id)
);

CREATE TABLE reservas (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id    BIGINT       NOT NULL,
    descripcion   VARCHAR(300) NOT NULL,
    fecha_reserva DATETIME(6)  NOT NULL,
    estado        VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    created_at    DATETIME(6),
    CONSTRAINT fk_reservas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id)
);

CREATE TABLE accesos (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id      BIGINT      NOT NULL,
    membresia_id    BIGINT,
    fecha_acceso    DATETIME(6) NOT NULL,
    exitoso         TINYINT(1)  NOT NULL,
    motivo_rechazo  VARCHAR(200),
    CONSTRAINT fk_accesos_cliente   FOREIGN KEY (cliente_id)   REFERENCES clientes (id),
    CONSTRAINT fk_accesos_membresia FOREIGN KEY (membresia_id) REFERENCES membresias (id)
);

CREATE TABLE rutinas (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id     BIGINT       NOT NULL,
    nombre         VARCHAR(200) NOT NULL,
    descripcion    TEXT,
    fecha_creacion DATE         NOT NULL,
    created_at     DATETIME(6),
    updated_at     DATETIME(6),
    CONSTRAINT fk_rutinas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id)
);

-- Índices para queries frecuentes
CREATE INDEX idx_membresias_cliente   ON membresias (cliente_id);
CREATE INDEX idx_membresias_vencimiento ON membresias (fecha_vencimiento);
CREATE INDEX idx_pagos_membresia      ON pagos (membresia_id);
CREATE INDEX idx_pagos_estado         ON pagos (estado);
CREATE INDEX idx_reservas_cliente     ON reservas (cliente_id);
CREATE INDEX idx_reservas_fecha       ON reservas (fecha_reserva);
CREATE INDEX idx_accesos_cliente      ON accesos (cliente_id);
