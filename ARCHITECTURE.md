# ARCHITECTURE.md — ClubCore

## Proposito

API REST de backend Java para gestión de un instituto de salud / complejo deportivo. Escala `medio` (1 stack, varias features). Sin frontend propio — la demo es Swagger UI.

## Capas (N-tier)

```
HTTP Request
    |
    v
[Controller]      — valida entrada (@Valid), delega a servicio, devuelve DTO
    |
    v
[Service / Impl]  — logica de negocio, transacciones (@Transactional)
    |              — MembresiaStateService: motor de estados (pieza central)
    v
[Repository]      — JpaRepository, queries derivadas del metodo
    |
    v
[Entity / DB]     — entidades JPA, MySQL 8, migraciones Flyway
```

Regla: la logica vive en la capa Service, nunca en Controller ni Repository.

## Motor de estados de membresia

La pieza mas importante para defender en entrevista. `MembresiaStateService.evaluarEstado()` calcula el estado real cruzando cuatro condiciones en orden de prioridad decreciente:

```
1. isSuspendida()                       → SUSPENDIDA  (decision admin, maxima prioridad)
2. fechaVencimiento.isBefore(hoy)       → INACTIVA    (periodo temporal vencido)
3. tiene pagos en PENDIENTE             → DEUDA       (vigente pero con deuda)
4. reservasMes >= plan.limiteReservasMes → INACTIVA   (cupo mensual agotado)
   └─ solo si el plan tiene limite (no null)
5. (ninguna de las anteriores)          → ACTIVA
```

El estado NO se persiste como campo mutable arbitrario: se recalcula on-demand antes de cada decision critica (check-in, reserva). El campo `estadoCacheado` se actualiza por el mismo service despues de operaciones que cambian el estado (nuevo pago, suspension).

## Seguridad

- JWT stateless: `JwtFilter` extrae el Bearer token, valida firma y expiracion, y establece el `SecurityContext`.
- Roles (`ADMIN`, `ENTRENADOR`, `CLIENTE`) via `@PreAuthorize` a nivel de metodo, no solo en `SecurityConfig`. Esto hace el RBAC visible y explicito en el codigo.
- Passwords con BCrypt(10).
- Secrets solo por variable de entorno (`JWT_SECRET`, `DB_PASSWORD`, `WEBHOOK_SIGNING_SECRET`).
- CORS acotado por `CORS_ALLOWED_ORIGINS` (lista explicita, sin wildcard+credentials) — obligatorio en perfil `prod`.

## Integraciones async

```
Job @Scheduled (8AM diario)
    → MembresiaStateService.notificarVencimientosProximos()
        → publishEvent(MembresiaPorVencerEvent)
            → NotificationDispatchService.onMembresiaPorVencer() [@Async]
                → WebhookDispatcher.dispatch()
                    [@Retry: 3 intentos, backoff exponencial 500ms→1s→2s]
                    [@CircuitBreaker: abre si >=50% de 10 llamadas fallan; 30s en OPEN]
                    → POST N8N_WEBHOOK_URL
                    (fallback: loguea y absorbe — sigue siendo best-effort)
```

El webhook inbound (`POST /api/v1/webhooks/pagos`) simula la confirmacion de un gateway externo → actualiza el Pago y recalcula el estado de la Membresia. No lleva JWT (el gateway no tiene uno nuestro), pero exige una firma HMAC-SHA256 valida (header `X-Webhook-Signature`, verificada por `WebhookSignatureFilter` contra `WEBHOOK_SIGNING_SECRET`) — sin firma valida, 401.

## Estructura de paquetes

```
com.clubcore.api/
├── config/         SecurityConfig, AsyncConfig, SwaggerConfig
├── controller/     Un controller por recurso + WebhookController
├── service/        Interfaces de servicio + NotificationDispatchService + WebhookDispatcher
│   └── impl/       Implementaciones concretas
├── repository/     JpaRepository por entidad
├── entity/         Entidades JPA (Usuario, Cliente, Plan, Membresia, Pago, Reserva, Acceso, Rutina)
├── dto/
│   ├── request/    Records de entrada (validados con @Valid)
│   └── response/   Records de salida (nunca expone entidades directamente)
├── mapper/         Interfaces MapStruct (Entity <-> DTO)
├── exception/      ResourceNotFoundException, MembresiaInvalidaException, GlobalExceptionHandler
├── security/       JwtService, JwtFilter, RateLimitFilter, UserDetailsServiceImpl
└── event/          MembresiaPorVencerEvent (listener en NotificationDispatchService)
```

## Base de datos

MySQL 8.4. Migraciones Flyway:
- `V1__init_schema.sql` — crea todas las tablas e indices
- `V2__seed_roles.sql` — inserta usuarios seed (admin/entrenador/cliente) y planes de ejemplo

## Flujo de request HTTP (ejemplo: crear reserva)

```
1. Cliente envia POST /api/v1/reservas
   {
     "membresiaId": 1,
     "fecha": "2026-08-15T10:00:00",
     "tipo": "CLASE"
   }

2. JwtFilter
   - Extrae Bearer token del header
   - Valida firma y expiracion (JwtService.validateToken)
   - Establece SecurityContext con Usuario y sus Roles

3. ReservaController.create(@Valid CreateReservaRequest)
   - Valida el DTO (anotaciones @NotNull, etc.)
   - Delega a ReservaService.crearReserva()

4. ReservaService.crearReserva()
   - Carga la Membresia desde DB
   - Invoca MembresiaStateService.evaluarEstado() — recalcula on-demand
   - Si estado != ACTIVA, lanza MembresiaInvalidaException (403)
   - Si ACTIVA, crea la Reserva, persiste, devuelve DTO

5. GlobalExceptionHandler captura excepciones
   - MembresiaInvalidaException → {status: 403, error: "Forbidden", message: "..."}
   - Sin stack trace (produccion-ready)

6. Respuesta JSON al cliente con status 201 Created
```

## Decisiones tecnicas y trade-offs

**MapStruct para mapeo Entity/DTO**
- Genera el codigo en tiempo de compilacion (no reflection). Mas rapido y tipado que ModelMapper o manual.
- Trade-off evitado: si usabamos ModelMapper (reflection), seria mas lento en listados paginados grandes.

**H2 en tests + Testcontainers en paralelo**
- Suite H2 (perfil `test`, sin Flyway) — tests unitarios y de controller, rápidos, sin Docker.
- `MembresiaIntegrationTest` (Testcontainers, MySQL 8.4 real) — verifica Flyway V1+V2 y el motor de estados contra el mismo engine de producción. Se corre en el mismo `./gradlew test`, requiere Docker disponible.
- Coexistencia: `@DynamicPropertySource` sobreescribe las props H2 del perfil `test` con las del contenedor MySQL. Las dos suites comparten el mismo run pero con contextos independientes.

**@WebMvcTest para controllers**
- Carga solo capa web + mocks de servicios. No instancia toda la app (mas rapido).
- Trade-off: no prueba integracion real (DB, eventos async). Pero combinado con H2 en tests y MembresiaStateServiceTest, cubre lo importante.

**Webhook inbound sin JWT, con firma HMAC**
- `/api/v1/webhooks/pagos` es public a nivel de Spring Security (simula gateway externo, que no tiene un JWT nuestro), pero `WebhookSignatureFilter` exige un header `X-Webhook-Signature` con el HMAC-SHA256 (hex) del body crudo, calculado con el secreto compartido `WEBHOOK_SIGNING_SECRET`. La comparacion es constant-time (`MessageDigest.isEqual`). Sin firma valida (o sin secreto configurado), 401 — fail-closed.
- Rate limit por IP ya cubierto por `RateLimitFilter` (20 req/min en `/api/v1/webhooks/**`).
- Pendiente real: logging y retry logic en Kafka/RabbitMQ si el volumen se vuelve critico.

**Estado cacheado vs recalculado**
- `estadoCacheado` en entidad Membresia: copia derivada para queries de listado rapidas.
- Estado REAL: recalculado on-demand antes de check-in/reserva (10ms de overhead, aceptable).
- Trade-off: evita UPDATE de Membresia cada vez que cambia un pago (que seria costoso), pero introduce campo redundante. Alternativa: cachear en Redis (mas complejo, no justificado en escala media).

**JWT stateless en lugar de sesiones/cookies**
- Tokens auto-contenidos (no requieren sesion en server). Escalable a multiples instancias sin sticky sessions.
- Trade-off: no hay revocacion inmediata (token vive hasta expiracion). En produccion: agregar blacklist en Redis si hay logout critico.

**Job @Scheduled vs Event-driven (Kafka)**
- Job diario a las 8AM detecta membresías venciendo mañana, publica evento, dispara webhook async.
- Trade-off: cron es simple y suficiente para escala media. Si fuesen millones de membresias y necesitabamos reactividad en tiempo real, seria event-driven (Kafka/RabbitMQ).

## Diagrama ER (simplificado)

```
usuarios ──1:1── clientes
                    │
                    ├──1:N── membresias ──N:1── planes
                    │            │
                    │            └──1:N── pagos
                    │
                    ├──1:N── reservas
                    ├──1:N── accesos
                    └──1:N── rutinas
```

## Observabilidad

`/actuator/prometheus` expone métricas Micrometer en formato Prometheus (scraping via Grafana/Prometheus).
Requiere rol `ADMIN` — no es un endpoint público (protegido en `SecurityConfig`).
`/actuator/health` sigue público para el healthcheck de Docker/Dokku.

Métricas disponibles con spring-boot-starter-actuator + micrometer-registry-prometheus:
- JVM (heap, GC, threads)
- HikariCP (pool de conexiones MySQL)
- HTTP server (latencia, conteo de requests por endpoint/status)
- Circuit breaker de n8n (estado CLOSED/OPEN/HALF-OPEN via `register-health-indicator: true`)

## Que mejoraria con mas tiempo

- **Métricas de negocio custom:** `MeterRegistry` para contar check-ins exitosos, membresías activas, tasa de webhook. Hoy solo hay métricas de infraestructura.
- **Caché de lectura:** Redis para listados paginados de clientes/planes.
- **Audit trail:** tabla de eventos (audit log) con quien creo/modifico cada Membresia/Pago (compliance/fintech).
- **Idempotencia en webhooks:** request ID unico para inbound webhook, evitar duplicados si se reintenta.
- **Rate limiting en escala horizontal:** Bucket4j actualmente en memoria por JVM. Migrar a Redis ProxyManager cuando se agreguen réplicas (ver `RateLimitFilter`).
- **Two-factor en login:** para ADMIN critico.
- **Versionado de API:** rutas `/api/v2/...` si hay breaking changes futuros.
