-- V2: Datos de seed para desarrollo y demos
-- Contraseña de todos los usuarios seed: Admin1234!
-- Hash generado con BCryptPasswordEncoder(strength=10)
-- Para regenerar: new BCryptPasswordEncoder(10).encode("Admin1234!")

INSERT INTO usuarios (email, password, rol, activo)
VALUES
    ('admin@clubcore.com',
     '$2y$10$aIEPrzq.S4okzlAyH/pFfeg6KaPLUvs/0iyZ9OBzbizpltWzqS2LS',
     'ADMIN', 1),
    ('entrenador@clubcore.com',
     '$2y$10$aIEPrzq.S4okzlAyH/pFfeg6KaPLUvs/0iyZ9OBzbizpltWzqS2LS',
     'ENTRENADOR', 1),
    ('cliente1@clubcore.com',
     '$2y$10$aIEPrzq.S4okzlAyH/pFfeg6KaPLUvs/0iyZ9OBzbizpltWzqS2LS',
     'CLIENTE', 1);

INSERT INTO clientes (nombre, apellido, email, telefono, usuario_id)
VALUES
    ('Admin', 'Sistema', 'admin@clubcore.com', '3516000001',
     (SELECT id FROM usuarios WHERE email = 'admin@clubcore.com')),
    ('Carlos', 'Entrenador', 'entrenador@clubcore.com', '3516000002',
     (SELECT id FROM usuarios WHERE email = 'entrenador@clubcore.com')),
    ('Maria', 'Gomez', 'cliente1@clubcore.com', '3516000003',
     (SELECT id FROM usuarios WHERE email = 'cliente1@clubcore.com'));

INSERT INTO planes (nombre, descripcion, precio, duracion_dias, limite_reservas_mes)
VALUES
    ('Mensual Básico',   'Acceso al complejo 3 veces por semana. Limite de 12 reservas/mes.',
     12000.00, 30, 12),
    ('Mensual Premium',  'Acceso ilimitado al complejo.',
     18000.00, 30, NULL),
    ('Trimestral Ahorro','3 meses de acceso ilimitado con precio especial.',
     45000.00, 90, NULL);
