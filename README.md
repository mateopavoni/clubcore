# ClubCore

API REST para la gestión integral de un instituto de salud / complejo deportivo. Cubre membresías con un motor de validación de estados (ACTIVA / INACTIVA / DEUDA / SUSPENDIDA), pagos, reservas de turnos, rutinas personalizadas y control de accesos, con integraciones webhook asíncronas hacia sistemas externos.

Construido como proyecto de portfolio de backend Java — defensible en entrevistas de empresa grande o fintech, demostrando seguridad (JWT + RBAC), arquitectura en capas, lógica de negocio compleja, tests unitarios, y operaciones enterprise (paginación, manejo de errores, Swagger, migraciones con Flyway).

![CI](https://github.com/mateopavoni/clubcore/actions/workflows/ci.yml/badge.svg)

## Stack

- Java 21 LTS + Spring Boot 3.3.x (Gradle Kotlin DSL)
- Spring Security + JWT (JJWT 0.12) — stateless, RBAC con `@PreAuthorize` a nivel de método
- Spring Data JPA + Hibernate + MySQL 8 (Flyway para migraciones versionadas)
- MapStruct — generación en tiempo de compilación del mapeo Entity ↔ DTO
- springdoc-openapi (Swagger UI + OpenAPI spec en `/swagger-ui.html`)
- Docker + Docker Compose para dev local (MySQL 8 en contenedor)
- JUnit 5 + Mockito para tests unitarios

## Funcionalidades

**Autenticación y autorización**
- Login JWT (token sin estado) → `POST /api/v1/auth/login`
- Roles ADMIN / ENTRENADOR / CLIENTE con validación por endpoint vía `@PreAuthorize`
- Contraseñas hasheadas con BCrypt

**Motor de estados de membresía** (pieza central)
- Evaluación on-demand de estado (no cacheado en DB como mutable)
- Estados: `ACTIVA` (todo en orden) | `INACTIVA` (vencida) | `DEUDA` (pagos pendientes) | `SUSPENDIDA` (decisión manual)
- Bloquea check-in y reservas automáticamente si la membresía no es `ACTIVA`
- Recalculado antes de cada decisión crítica (check-in, reserva)

**Gestión de membresías y planes**
- CRUD de clientes (paginado)
- CRUD de planes con límite configurable de reservas/mes
- CRUD de membresías, paginado, con filtro por estado
- Endpoint de suspensión manual: `PATCH /api/v1/membresias/{id}/suspender`

**Pagos y reservas**
- CRUD de pagos (ADMIN only)
- CRUD de reservas con validación de estado antes de crear
- Webhook inbound: `POST /api/v1/webhooks/pagos` (simula confirmación de gateway externo)

**Check-in y control de accesos**
- `POST /api/v1/accesos` registra ingreso/egreso del cliente
- Rechaza automáticamente si la membresía no es válida, registra motivo de rechazo

**Rutinas personalizadas**
- CRUD de rutinas (CLIENTE/ENTRENADOR)

**Integraciones asíncronas**
- Job `@Scheduled` diario (8AM) que detecta membresías venciendo mañana
- Dispara webhook saliente asíncrono a n8n (best-effort, no propaga errores)
- Payload JSON estructurado con datos de membresía y cliente

**Operaciones enterprise**
- Paginación y sorting en endpoints de listado (`?page=0&size=10&sort=id,desc`)
- Manejo global de excepciones → JSON estructurado `{timestamp, status, error, message, path}`, sin stack traces
- Swagger UI completo + spec OpenAPI descargable

## Inicio rápido

```bash
# 1. Clonar y entrar
git clone <repo> clubcore && cd clubcore

# 2. Variables de entorno
cp .env.example .env
# Completar: DB_PASSWORD, DB_ROOT_PASSWORD, JWT_SECRET

# 3. Levantar MySQL
docker-compose up -d

# 4. Correr la app (Flyway aplica migraciones automáticamente)
./gradlew bootRun

# Listo: http://localhost:8080/swagger-ui.html
```

Ver [RUN.md](RUN.md) para más detalles.

## Ejemplos de uso (curl)

### Login y obtener JWT

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@clubcore.com","password":"Admin1234!"}'

# Respuesta:
# {"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."}
```

Guardar el token como variable:
```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Listar clientes (paginado, protegido)

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/clientes?page=0&size=10

# Respuesta: {
#   "content": [{...}],
#   "pageable": {"pageNumber": 0, "pageSize": 10},
#   "totalElements": 3
# }
```

### Crear una reserva (valida estado de membresía)

```bash
curl -X POST http://localhost:8080/api/v1/reservas \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"membresiaId":1,"fecha":"2026-08-15T10:00:00","tipo":"CLASE"}'

# 201 Created si la membresía es ACTIVA
# 403 Forbidden si la membresía es INACTIVA/DEUDA/SUSPENDIDA
# Respuesta: {"id":1,"membresiaId":1,"estado":"CONFIRMADA","fecha":"2026-08-15T10:00:00"}
```

### Registrar check-in (bloquea membresías inválidas)

```bash
curl -X POST http://localhost:8080/api/v1/accesos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"clienteId":3}'

# 201 si está ACTIVA: {"id":1,"clienteId":3,"exitoso":true,"timestamp":"2026-07-27T14:30:00"}
# 403 si no está ACTIVA: {"status":403,"error":"Acceso denegado","message":"MEMBRESIA_SUSPENDIDA",...}
```

### Suspender una membresía (ADMIN only)

```bash
curl -X PATCH http://localhost:8080/api/v1/membresias/1/suspender?suspendida=true \
  -H "Authorization: Bearer $TOKEN"

# 200 OK: {"id":1,"clienteId":3,"estado":"SUSPENDIDA","suspendida":true,...}
```

## Documentación técnica

- [ARCHITECTURE.md](ARCHITECTURE.md) — decisiones de arquitectura, diagrama de capas, motor de estados, trade-offs
- [RUN.md](RUN.md) — arranque local paso a paso (Docker + Gradle)
- [MANUAL-PRUEBAS.md](MANUAL-PRUEBAS.md) — verificación manual de webhooks y estados de membresía
- [Swagger UI](http://localhost:8080/swagger-ui.html) — demo interactiva (con app corriendo)

## Licencia

Copyright (c) 2026 Mateo Pavoni. Todos los derechos reservados. Ver [LICENSE](LICENSE).

---

## Changelog

| Version | Fecha      | Cambio                                |
|---------|------------|---------------------------------------|
| v0.1.0  | 2026-07-27 | FASES 1-4 completas: scaffold, core, tests, pulido. Motor de estados, JWT+RBAC, webhooks async, Swagger. |
