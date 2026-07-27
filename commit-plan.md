# commit-plan.md — ClubCore

Commits atómicos que cuentan la historia del proyecto, en orden. Cada commit es una unidad coherente y testeable. Formato: Conventional Commits. Sin `Co-Authored-By`, sin fechas falsas.

Instrucciones: ejecutar en orden con el script `commit-plan.sh` o manualmente con `git commit` desde el proyecto.

---

## FASE 1: Setup y configuración

### Commit 1: scaffold: configurar Gradle Kotlin DSL + Spring Boot 3.3.x + Java 21

**Archivos:**
- `build.gradle.kts` (dependencias: web, security, data-jpa, mysql, jjwt, mapstruct, springdoc, lombok, h2 para test)
- `.gitignore` (Gradle, IDE, .env local)
- `.env.example` (DB_PASSWORD, DB_ROOT_PASSWORD, JWT_SECRET, N8N_WEBHOOK_URL)
- `settings.gradle.kts`

**Mensaje:**
```
scaffold: configurar Gradle Kotlin DSL + Spring Boot 3.3.x + Java 21

- JDK 21 LTS como target language level
- Spring Boot 3.3.x con plugins (Spring Boot, Kotlin)
- Dependencias: spring-boot-starter-web, -security, -data-jpa, -mysql,
  jjwt-0.12.x, mapstruct-1.5.x, springdoc-openapi, lombok, h2 (test)
- .env.example con todas las vars de entorno necesarias
- .gitignore con patrones de Gradle, IDE, archivos locales
```

---

### Commit 2: config: agregar application-*.yml para dev/prod/test

**Archivos:**
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`
- `src/main/resources/application-test.yml`

**Mensaje:**
```
config: agregar application-*.yml para dev/prod/test

- application-dev.yml: MySQL local, logs DEBUG, Flyway enabled, hibernate ddl-auto=update
- application-prod.yml: MySQL remoto, logs INFO, Flyway enabled, hibernate ddl-auto=validate
- application-test.yml: H2 en memoria, logs DEBUG, Flyway disabled, hibernate ddl-auto=create-drop
- Todas con propiedades de JPA, servlet.encoding, Jackson timezone
```

---

### Commit 3: infra: agregar docker-compose.yml con MySQL 8

**Archivos:**
- `docker-compose.yml`
- `.dockerignore`
- `Dockerfile`

**Mensaje:**
```
infra: agregar docker-compose.yml con MySQL 8 + Dockerfile multi-stage

- docker-compose.yml: MySQL 8.4, volumen persistente, healthcheck
- Dockerfile: two-stage (build con eclipse-temurin:21-jdk, runtime con jre)
- Usuario no-root en contenedor
- HEALTHCHECK en puerto 8080
```

---

### Commit 4: docs: README + ARCHITECTURE + RUN + TASKS + LICENSE

**Archivos:**
- `README.md` (descripcion, stack, funcionalidades, ejemplos curl)
- `ARCHITECTURE.md` (capas, motor de estados, seguridad, decisiones tecnicas)
- `RUN.md` (arranque local paso a paso)
- `TASKS.md` (checklist de fases)
- `LICENSE` (all-rights-reserved, Mateo Pavoni)
- `MANUAL-PRUEBAS.md` (webhook + validacion manual de estados)

**Mensaje:**
```
docs: README + ARCHITECTURE + RUN + TASKS + LICENSE

- README.md: descripción, stack, funcionalidades, ejemplos curl
- ARCHITECTURE.md: N-tier architecture, diagrama ER, motor de estados,
  decisiones técnicas (MapStruct, H2 en tests, JWT stateless)
- RUN.md: instrucciones de arranque local (Docker + Gradle)
- TASKS.md: fases 1-5 con checkpoints
- LICENSE: all-rights-reserved a nombre de Mateo Pavoni
- MANUAL-PRUEBAS.md: pasos para validar webhook y estados de membresía
- .claude/CLAUDE.md: memoria del proyecto (stack, reglas, quirks)
- .claude/commands/cambio.md: template para futuros cambios
```

---

### Commit 5: ci: agregar GitHub Actions workflow (CI/CD)

**Archivos:**
- `.github/workflows/ci.yml`

**Mensaje:**
```
ci: agregar GitHub Actions workflow para compilar + testear

- Trigger: push a main, pull requests
- Matrix JDK: 21 LTS (eclipse-temurin)
- Steps: checkout, setup-java, ./gradlew build
- Artifacts: reportes de test (si fallan)
```

---

## FASE 2: Entidades y capas

### Commit 6: entity: crear entidades JPA (Usuario, Cliente, Plan, Membresia, Pago, Reserva, Acceso, Rutina)

**Archivos:**
- `src/main/java/com/clubcore/api/entity/Usuario.java`
- `src/main/java/com/clubcore/api/entity/Cliente.java`
- `src/main/java/com/clubcore/api/entity/Plan.java`
- `src/main/java/com/clubcore/api/entity/Membresia.java`
- `src/main/java/com/clubcore/api/entity/Pago.java`
- `src/main/java/com/clubcore/api/entity/Reserva.java`
- `src/main/java/com/clubcore/api/entity/Acceso.java`
- `src/main/java/com/clubcore/api/entity/Rutina.java`
- `src/main/java/com/clubcore/api/entity/enums/Rol.java`
- `src/main/java/com/clubcore/api/entity/enums/EstadoMembresia.java`
- `src/main/java/com/clubcore/api/entity/enums/EstadoPago.java`
- `src/main/java/com/clubcore/api/entity/enums/EstadoReserva.java`

**Mensaje:**
```
entity: crear entidades JPA + enums

- Usuario: email, password hash, rol, clienteAsociado (relacion 1:1)
- Cliente: nombre, teléfono, usuario asociado
- Plan: nombre, precio, límite de reservas/mes
- Membresia: cliente, plan, fecha_inicio, fecha_vencimiento, suspendida, estadoCacheado
- Pago: membresia, monto, estado (PENDIENTE/PAGADO), fecha de pago
- Reserva: membresia, fecha, tipo, estado
- Acceso: cliente, timestamp, exitoso, motivo_rechazo
- Rutina: cliente, entrenador, descripción, plan semanal
- Enums: Rol (ADMIN/ENTRENADOR/CLIENTE), EstadoMembresia (ACTIVA/INACTIVA/DEUDA/SUSPENDIDA),
  EstadoPago (PENDIENTE/PAGADO), EstadoReserva (CONFIRMADA/CANCELADA)
```

---

### Commit 7: db: crear migraciones Flyway (schema + seed)

**Archivos:**
- `src/main/resources/db/migration/V1__init_schema.sql`
- `src/main/resources/db/migration/V2__seed_roles.sql`

**Mensaje:**
```
db: crear migraciones Flyway (V1: schema, V2: seed)

- V1__init_schema.sql:
  * Crea tablas: usuarios, clientes, planes, membresias, pagos, reservas, accesos, rutinas
  * Indices en email, membresias.cliente_id, pagos.membresia_id, etc.
  * Foreign keys con ON DELETE CASCADE/SET NULL segun corresponda
- V2__seed_roles.sql:
  * 3 usuarios seed: admin@clubcore.com, entrenador@clubcore.com, cliente1@clubcore.com
  * Password: BCrypt(10) de "Admin1234!" (hash: $2a$10/...)
  * 3 planes seed: Mensual Basico (12 reservas/mes), Trimestral (20), Anual (unlimited)
```

---

### Commit 8: dto: crear DTOs (request + response)

**Archivos:**
- `src/main/java/com/clubcore/api/dto/request/CreateClienteRequest.java`
- `src/main/java/com/clubcore/api/dto/request/CreateMembresiaRequest.java`
- `src/main/java/com/clubcore/api/dto/request/CreatePagoRequest.java`
- `src/main/java/com/clubcore/api/dto/request/CreateReservaRequest.java`
- `src/main/java/com/clubcore/api/dto/request/CreateAccesoRequest.java`
- `src/main/java/com/clubcore/api/dto/request/CreateRutinaRequest.java`
- `src/main/java/com/clubcore/api/dto/request/LoginRequest.java`
- `src/main/java/com/clubcore/api/dto/response/ClienteResponse.java`
- `src/main/java/com/clubcore/api/dto/response/MembresiaResponse.java`
- (... otros response DTOs)

**Mensaje:**
```
dto: crear DTOs (request + response) con validaciones

- Request DTOs (records): LoginRequest, CreateClienteRequest, CreateMembresiaRequest,
  CreatePagoRequest, CreateReservaRequest, CreateAccesoRequest, CreateRutinaRequest
- Validaciones: @NotNull, @Email, @NotBlank, @Positive, etc.
- Response DTOs (records): ClienteResponse, MembresiaResponse, PagoResponse, etc.
- DTOs nunca exponen entidades directamente — encapsulación de datos
```

---

### Commit 9: mapper: crear MapStruct mappers (Entity ↔ DTO)

**Archivos:**
- `src/main/java/com/clubcore/api/mapper/ClienteMapper.java`
- `src/main/java/com/clubcore/api/mapper/MembresiaMapper.java`
- `src/main/java/com/clubcore/api/mapper/PagoMapper.java`
- `src/main/java/com/clubcore/api/mapper/ReservaMapper.java`
- `src/main/java/com/clubcore/api/mapper/AccesoMapper.java`
- `src/main/java/com/clubcore/api/mapper/RutinaMapper.java`
- `src/main/java/com/clubcore/api/mapper/PlanMapper.java`

**Mensaje:**
```
mapper: crear MapStruct mappers (Entity ↔ DTO)

- ClienteMapper: toDTO, toEntity
- MembresiaMapper: toDTO (incluir estado recalculado), toEntity
- PagoMapper, ReservaMapper, AccesoMapper, RutinaMapper, PlanMapper: toDTO, toEntity
- Mapeos personalizados donde sea necesario (@Mapping)
- Generación en compile-time (no reflection)
```

---

### Commit 10: repository: crear JpaRepositories

**Archivos:**
- `src/main/java/com/clubcore/api/repository/UsuarioRepository.java`
- `src/main/java/com/clubcore/api/repository/ClienteRepository.java`
- `src/main/java/com/clubcore/api/repository/PlanRepository.java`
- `src/main/java/com/clubcore/api/repository/MembresiaRepository.java`
- `src/main/java/com/clubcore/api/repository/PagoRepository.java`
- `src/main/java/com/clubcore/api/repository/ReservaRepository.java`
- `src/main/java/com/clubcore/api/repository/AccesoRepository.java`
- `src/main/java/com/clubcore/api/repository/RutinaRepository.java`

**Mensaje:**
```
repository: crear JpaRepositories con queries derivadas

- UsuarioRepository: findByEmail, existsByEmail
- MembresiaRepository: findByClienteId, findByEstadoCacheado, countByMembresiaAndMesVencimiento
- PagoRepository: findByMembresiaId, findByEstado
- ReservaRepository: findByMembresiaId, countByMembresiaAndMesByFecha
- AccesoRepository: findByClienteId, findByClienteIdAndFecha
- Todas extienden JpaRepository<Entity, Long>
```

---

## FASE 2.5: Lógica de negocio (servicios)

### Commit 11: service: crear MembresiaStateService (motor de estados)

**Archivos:**
- `src/main/java/com/clubcore/api/service/MembresiaStateService.java`
- `src/main/java/com/clubcore/api/service/impl/MembresiaStateServiceImpl.java`

**Mensaje:**
```
service: crear MembresiaStateService (motor de estados)

- evaluarEstado(Membresia): recalcula estado on-demand basado en reglas de prioridad:
  1. isSuspendida() → SUSPENDIDA
  2. fechaVencimiento < hoy → INACTIVA
  3. tiene pagos PENDIENTE → DEUDA
  4. reservasMes >= limiteReservasMes → INACTIVA
  5. ninguna → ACTIVA
- actualizarEstadoCacheado(Membresia): actualiza el campo derivado después de operaciones
- Job @Scheduled diario a las 8AM: notificarVencimientosProximos()
- Publicador de eventos MembresiaPorVencerEvent
```

---

### Commit 12: service: crear servicios de dominio (Cliente, Membresia, Pago, Reserva, Acceso, Plan, Rutina)

**Archivos:**
- `src/main/java/com/clubcore/api/service/ClienteService.java`
- `src/main/java/com/clubcore/api/service/impl/ClienteServiceImpl.java`
- `src/main/java/com/clubcore/api/service/MembresiaService.java`
- `src/main/java/com/clubcore/api/service/impl/MembresiaServiceImpl.java`
- (... PagoService, ReservaService, AccesoService, PlanService, RutinaService)

**Mensaje:**
```
service: crear servicios de dominio

- ClienteService: CRUD paginado, validaciones
- MembresiaService: CRUD + suspend, delega evaluarEstado a MembresiaStateService
- PagoService: CRUD, valida FK, actualiza estado de membresía
- ReservaService: CRUD + validación de estado de membresia antes de crear
- AccesoService: check-in + loguea rechazo con motivo
- PlanService: CRUD
- RutinaService: CRUD
- Todas con transaccionalidad (@Transactional)
- Manejo de excepciones: ResourceNotFoundException, MembresiaInvalidaException
```

---

### Commit 13: event: crear MembresiaPorVencerEvent + NotificationDispatchService

**Archivos:**
- `src/main/java/com/clubcore/api/event/MembresiaPorVencerEvent.java`
- `src/main/java/com/clubcore/api/service/NotificationDispatchService.java`

**Mensaje:**
```
event: crear evento de membresía + dispatcher async

- MembresiaPorVencerEvent: contiene membresiaId, clienteId, email, nombre, fechaVencimiento, plan
- NotificationDispatchService:
  * Método @Async @EventListener(MembresiaPorVencerEvent)
  * Arma payload JSON y hace POST a N8N_WEBHOOK_URL
  * Best-effort: captura excepciones, loguea, no propaga fallos
- Configuración de ThreadPoolTaskExecutor en AsyncConfig
```

---

## FASE 2.5 (cont.): Seguridad

### Commit 14: security: crear JwtService + JwtFilter + UserDetailsService

**Archivos:**
- `src/main/java/com/clubcore/api/security/JwtService.java`
- `src/main/java/com/clubcore/api/security/JwtFilter.java`
- `src/main/java/com/clubcore/api/security/UserDetailsServiceImpl.java`

**Mensaje:**
```
security: crear JWT service + filter + UserDetailsService

- JwtService:
  * generateToken(Usuario): arma JWT con subject=email, claims(roles)
  * validateToken(token): valida firma + expiracion
  * extractEmail(token): extrae subject
  * Usa JJWT 0.12 (HS256)
- JwtFilter: OncePerRequestFilter que:
  * Extrae Bearer token del header Authorization
  * Valida con JwtService
  * Establece SecurityContext (userId, roles)
- UserDetailsServiceImpl: loadUserByUsername busca en DB y retorna UserDetails con roles
```

---

### Commit 15: config: agregar SecurityConfig + AsyncConfig + SwaggerConfig

**Archivos:**
- `src/main/java/com/clubcore/api/config/SecurityConfig.java`
- `src/main/java/com/clubcore/api/config/AsyncConfig.java`
- `src/main/java/com/clubcore/api/config/SwaggerConfig.java`

**Mensaje:**
```
config: agregar SecurityConfig + AsyncConfig + SwaggerConfig

- SecurityConfig:
  * Stateless (no sesiones)
  * CORS enabled
  * JWT filter en chain
  * @EnableMethodSecurity para @PreAuthorize
  * PasswordEncoder: BCryptPasswordEncoder(10)
- AsyncConfig: ThreadPoolTaskExecutor (core=2, max=5, queue=100)
- SwaggerConfig: Swagger/OpenAPI en /swagger-ui.html, schema OpenAPI descargable
```

---

## FASE 3: Controllers

### Commit 16: controller: agregar AuthController (login)

**Archivos:**
- `src/main/java/com/clubcore/api/controller/AuthController.java`

**Mensaje:**
```
controller: agregar AuthController

- POST /api/v1/auth/login: valida credenciales, retorna JWT
- Respuesta: {"token": "eyJhbGciOiJIUzI1NiIs..."}
- Validación: @Valid sobre LoginRequest
- Manejo de excepciones: credenciales inválidas → 401
```

---

### Commit 17: controller: agregar ClienteController (CRUD paginado)

**Archivos:**
- `src/main/java/com/clubcore/api/controller/ClienteController.java`

**Mensaje:**
```
controller: agregar ClienteController

- GET /api/v1/clientes (paginado): @PreAuthorize ADMIN
- GET /api/v1/clientes/{id}: @PreAuthorize ADMIN
- POST /api/v1/clientes: @PreAuthorize ADMIN
- PUT /api/v1/clientes/{id}: @PreAuthorize ADMIN
- DELETE /api/v1/clientes/{id}: @PreAuthorize ADMIN
- Paginación: ?page=0&size=10&sort=id,desc
- Respuestas: Page<ClienteResponse>, ClienteResponse
```

---

### Commit 18: controller: agregar MembresiaController (CRUD + suspend)

**Archivos:**
- `src/main/java/com/clubcore/api/controller/MembresiaController.java`

**Mensaje:**
```
controller: agregar MembresiaController

- GET /api/v1/membresias (paginado, filtro por estado): @PreAuthorize ADMIN
- GET /api/v1/membresias/{id}: @PreAuthorize ADMIN
- POST /api/v1/membresias: @PreAuthorize ADMIN, valida cliente + plan
- PUT /api/v1/membresias/{id}: @PreAuthorize ADMIN
- DELETE /api/v1/membresias/{id}: @PreAuthorize ADMIN
- PATCH /api/v1/membresias/{id}/suspender?suspendida=true: @PreAuthorize ADMIN
- Respuesta incluye estado recalculado via MembresiaStateService
- Manejo: ResourceNotFoundException (404), MembresiaInvalidaException (400)
```

---

### Commit 19: controller: agregar PagoController (CRUD)

**Archivos:**
- `src/main/java/com/clubcore/api/controller/PagoController.java`

**Mensaje:**
```
controller: agregar PagoController

- GET /api/v1/pagos (paginado): @PreAuthorize ADMIN
- GET /api/v1/pagos/{id}: @PreAuthorize ADMIN
- POST /api/v1/pagos: @PreAuthorize ADMIN, valida membresia
- PUT /api/v1/pagos/{id}: @PreAuthorize ADMIN, actualiza estado y recalcula membresia
- DELETE /api/v1/pagos/{id}: @PreAuthorize ADMIN
- Respuestas: Page<PagoResponse>, PagoResponse
```

---

### Commit 20: controller: agregar ReservaController (CRUD + validación de estado)

**Archivos:**
- `src/main/java/com/clubcore/api/controller/ReservaController.java`

**Mensaje:**
```
controller: agregar ReservaController

- GET /api/v1/reservas (paginado): @PreAuthorize CLIENTE/ENTRENADOR/ADMIN
- POST /api/v1/reservas: @PreAuthorize CLIENTE/ENTRENADOR, valida membresia ACTIVA
  * Si estado != ACTIVA → 403 MembresiaInvalidaException
  * Si cupo agotado → 403
- PATCH /api/v1/reservas/{id}/cancelar: @PreAuthorize CLIENTE/ENTRENADOR
- Respuestas: Page<ReservaResponse>, ReservaResponse
```

---

### Commit 21: controller: agregar AccesoController (check-in)

**Archivos:**
- `src/main/java/com/clubcore/api/controller/AccesoController.java`

**Mensaje:**
```
controller: agregar AccesoController

- POST /api/v1/accesos: check-in, valida membresia ACTIVA
  * Si ACTIVA → 201 Created + registro exitoso
  * Si no ACTIVA → 403 + registro con motivo_rechazo (SUSPENDIDA/VENCIDA/DEUDA/CUPO_AGOTADO)
- GET /api/v1/accesos (paginado): @PreAuthorize ENTRENADOR/ADMIN
- Respuestas: AccesoResponse con timestamp, exitoso, motivo (si aplica)
```

---

### Commit 22: controller: agregar PlanController + RutinaController (CRUD)

**Archivos:**
- `src/main/java/com/clubcore/api/controller/PlanController.java`
- `src/main/java/com/clubcore/api/controller/RutinaController.java`

**Mensaje:**
```
controller: agregar PlanController + RutinaController

- PlanController:
  * GET /api/v1/planes (paginado): público
  * GET /api/v1/planes/{id}: público
  * POST /api/v1/planes: @PreAuthorize ADMIN
  * PUT/DELETE: @PreAuthorize ADMIN
- RutinaController:
  * GET /api/v1/rutinas (paginado): @PreAuthorize CLIENTE/ENTRENADOR
  * POST /api/v1/rutinas: @PreAuthorize CLIENTE/ENTRENADOR
  * PUT/DELETE: @PreAuthorize CLIENTE/ENTRENADOR (validar propiedad)
```

---

### Commit 23: controller: agregar WebhookController (inbound)

**Archivos:**
- `src/main/java/com/clubcore/api/controller/WebhookController.java`

**Mensaje:**
```
controller: agregar WebhookController (inbound)

- POST /api/v1/webhooks/pagos: public (simula gateway externo)
  * Body: {"membresiaId": 1, "estado": "PAGADO", "timestamp": "2026-07-27T..."}
  * Busca Pago asociado, actualiza estado a PAGADO
  * Recalcula estado de Membresia
  * Retorna 200 OK
- Nota: sin JWT (simula llamada de proveedor externo)
```

---

## FASE 4: Excepciones y manejo de errores

### Commit 24: exception: crear GlobalExceptionHandler

**Archivos:**
- `src/main/java/com/clubcore/api/exception/ResourceNotFoundException.java`
- `src/main/java/com/clubcore/api/exception/MembresiaInvalidaException.java`
- `src/main/java/com/clubcore/api/exception/GlobalExceptionHandler.java`

**Mensaje:**
```
exception: crear GlobalExceptionHandler + excepciones custom

- ResourceNotFoundException: 404 (entidad no encontrada)
- MembresiaInvalidaException: 403/400 (estado invalido, cupo agotado, etc.)
- GlobalExceptionHandler (@RestControllerAdvice):
  * Maneja excepciones custom + MethodArgumentNotValidException (validaciones)
  * Respuesta uniforme: {"timestamp", "status", "error", "message", "path"}
  * Sin stack traces (produccion-ready)
  * HTTP status: 400, 403, 404, 500
```

---

## FASE 5: Tests

### Commit 25: test: crear MembresiaStateServiceTest (10 casos)

**Archivos:**
- `src/test/java/com/clubcore/api/service/MembresiaStateServiceTest.java`

**Mensaje:**
```
test: crear MembresiaStateServiceTest (10 casos)

- Test 1-4: Cada estado (ACTIVA, SUSPENDIDA, INACTIVA, DEUDA)
- Test 5-6: Bordes (cupo mensual exacto, día de vencimiento)
- Test 7-10: Combinaciones (suspendida + vencida, deuda + cupo agotado, etc.)
- Mockito para repositories
- Profile 'test' con H2 en memoria
- Flyway deshabilitado en test
```

---

### Commit 26: test: crear AuthControllerTest + PagoControllerTest

**Archivos:**
- `src/test/java/com/clubcore/api/controller/AuthControllerTest.java`
- `src/test/java/com/clubcore/api/controller/PagoControllerTest.java`

**Mensaje:**
```
test: crear AuthControllerTest + PagoControllerTest

- AuthControllerTest (@WebMvcTest):
  * Test login OK
  * Test credenciales inválidas (401)
  * Test body inválido (400)
- PagoControllerTest (@WebMvcTest):
  * Test RBAC: ADMIN=200, CLIENTE=403, ENTRENADOR=403, sin auth=401
  * Mockear PagoService
- Total: 19 tests, 0 failures
```

---

## FASE 6: Docker y CI

### Commit 27: docker: build multi-stage + HEALTHCHECK

**Archivos:**
- `Dockerfile` (actualizado)
- `.dockerignore`

**Mensaje:**
```
docker: build multi-stage + HEALTHCHECK

- Stage 1: build con eclipse-temurin:21-jdk, ./gradlew bootJar
- Stage 2: runtime con eclipse-temurin:21-jre, COPY JAR, usuario no-root
- HEALTHCHECK: GET http://localhost:8080/actuator/health cada 30s
- Entry point: java -jar app.jar
- Verificado: docker build + docker run contra MySQL real
```

---

### Commit 28: ci: GitHub Actions workflow CI

**Archivos:**
- `.github/workflows/ci.yml` (actualizado con todas las fases)

**Mensaje:**
```
ci: GitHub Actions workflow compilar + testear

- Trigger: push a main + PRs
- JDK 21 (eclipse-temurin)
- Steps: checkout → setup-java → ./gradlew build (corre tests)
- Artifacts: test reports si fallan
- Status badge en README
```

---

## FASE 7: Documentación de portfolio

### Commit 29: portfolio: agregar PORTFOLIO.md (LinkedIn + talking points + logo)

**Archivos:**
- `PORTFOLIO.md`

**Mensaje:**
```
portfolio: agregar PORTFOLIO.md

- Borrador de post LinkedIn (primera persona, honesto sobre alcance)
- Talking points de entrevista (8 preguntas probables + respuestas)
- Ideas de logo (3 conceptos visuales distintos)
- Documento de defensa técnica del proyecto
```

---

### Commit 30: portfolio: agregar commit-plan.md + commit-plan.sh

**Archivos:**
- `commit-plan.md`
- `commit-plan.sh`

**Mensaje:**
```
portfolio: agregar commit-plan + script de aplicación

- commit-plan.md: 30 commits atómicos, Conventional Commits, sin Co-Authored-By
- commit-plan.sh: bash script que aplica commits en orden (--from/--to/--dry-run support)
- Verificación: es un repo git, no hace push
```

---

## FASE 8: Mejoras post-deploy (verificación real + resiliencia)

> Estos commits salieron de deployar clubcore a la VPS y probarlo de punta a punta contra la
> API real (no solo tests locales) — cada uno corrige algo que solo apareció bajo uso real.

### Commit 31: fix(observability): agregar Actuator para healthcheck real

**Archivos:**
- `build.gradle.kts`
- `src/main/resources/application.yml`

**Mensaje:**
```
fix(observability): agregar Actuator para healthcheck real

El Dockerfile ya apuntaba su HEALTHCHECK a /actuator/health, pero nunca se
había agregado spring-boot-starter-actuator — el container nunca pasaba el
healthcheck. Se agrega la dependencia y se expone /actuator/health (público,
lo necesita Docker/Dokku).
```

---

### Commit 32: fix(errors): manejar MissingServletRequestParameterException y DataIntegrityViolationException

**Archivos:**
- `src/main/java/com/clubcore/api/exception/GlobalExceptionHandler.java`

**Mensaje:**
```
fix(errors): status codes correctos para params faltantes y conflictos de FK

Dos excepciones caían en el catch-all genérico y devolvían 500 en vez de un
código apropiado:
- MissingServletRequestParameterException (ej. PATCH /membresias/{id}/suspender
  sin el query param ?suspendida=) -> ahora 400.
- DataIntegrityViolationException (ej. DELETE /clientes/{id} con membresías/pagos
  asociados) -> ahora 409, con mensaje claro en vez de "Error interno del servidor".
```

---

### Commit 33: fix(security): accessDeniedHandler explícito para reglas a nivel de filtro

**Archivos:**
- `src/main/java/com/clubcore/api/config/SecurityConfig.java`

**Mensaje:**
```
fix(security): 403 real para denegaciones por rol en reglas de filtro

Un usuario autenticado pero sin el rol requerido en una regla
.requestMatchers(...).hasRole(...) (ej. /actuator/prometheus, /actuator/metrics)
caía en el authenticationEntryPoint (401 "no autenticado") en vez de un 403 -
a diferencia de un @PreAuthorize de método, que sí resuelve bien porque esa
excepción llega al GlobalExceptionHandler. Se agrega accessDeniedHandler
explícito con el mismo shape ApiErrorResponse.
```

---

### Commit 34: feat(resilience): Resilience4j retry + circuit breaker en el webhook a n8n

**Archivos:**
- `build.gradle.kts`
- `src/main/java/com/clubcore/api/service/WebhookDispatcher.java`
- `src/main/java/com/clubcore/api/service/NotificationDispatchService.java`
- `src/main/resources/application.yml`
- `src/test/java/com/clubcore/api/service/WebhookDispatcherResilienceTest.java`

**Mensaje:**
```
feat(resilience): retry + circuit breaker en el webhook saliente a n8n

WebhookDispatcher (bean separado para que el proxy de Spring AOP intercepte
las anotaciones - self-invocation) queda decorado con @Retry (3 intentos,
backoff exponencial 500ms->1s->2s) y @CircuitBreaker (ventana de 10 llamadas,
mínimo 5 para evaluar tasa de fallos, abre a partir de 50% de fallos, 30s en
OPEN). El fallbackMethod vive en @Retry, no en @CircuitBreaker: si se pone en
el CircuitBreaker, su fallback intercepta la excepción ni bien falla el primer
intento y el Retry nunca ve un fallo que reintentar.

Test de integración incluido: verifica que el Retry reintenta 3 veces de
verdad y que el circuito abre y hace fail-fast tras superar el umbral - no
alcanza con que las anotaciones "estén", hay que probar que efectivamente
actúan (encontrado con este mismo test: el fallback mal puesto y el
minimum-number-of-calls sin configurar dejaban ambas piezas inertes).
```

---

### Commit 35: feat(security): rate limiting con Bucket4j en login y webhooks

**Archivos:**
- `build.gradle.kts`
- `src/main/java/com/clubcore/api/security/RateLimitFilter.java`
- `src/main/java/com/clubcore/api/config/SecurityConfig.java`

**Mensaje:**
```
feat(security): rate limiting en endpoints públicos sin auth

RateLimitFilter (OncePerRequestFilter, mismo patrón que JwtFilter): 5
solicitudes/minuto por IP en POST /api/v1/auth/login, 20/minuto en
/api/v1/webhooks/**. Buckets en memoria (ConcurrentHashMap) - ponytail:
si se escala a múltiples instancias hace falta un backend compartido
(Redis ProxyManager de Bucket4j) en vez de memoria local. Al superar el
límite, 429 con el mismo shape ApiErrorResponse que el resto de la API.
```

---

### Commit 36: feat(observability): métricas Prometheus vía Micrometer

**Archivos:**
- `build.gradle.kts`
- `src/main/resources/application.yml`
- `src/main/java/com/clubcore/api/config/SecurityConfig.java`

**Mensaje:**
```
feat(observability): exponer métricas Prometheus (solo ADMIN)

/actuator/prometheus y /actuator/metrics/** requieren rol ADMIN explícito en
SecurityConfig (no van en PUBLIC_ENDPOINTS) - endpoints de métricas/admin no
deben quedar abiertos sin auth. /actuator/health se mantiene público (lo
necesita el healthcheck de Docker/Dokku).
```

---

### Commit 37: test(integration): Testcontainers contra MySQL real

**Archivos:**
- `build.gradle.kts`
- `src/test/java/com/clubcore/api/integration/MembresiaIntegrationTest.java`
- `RUN.md`

**Mensaje:**
```
test(integration): tests de integración con Testcontainers (MySQL real)

Complementa (no reemplaza) los tests rápidos con H2: 5 tests con
@Testcontainers + MySQLContainer real (misma imagen que producción, 8.4),
verificando que Flyway migra correctamente sobre MySQL real y el motor de
estados funciona igual que en H2. Se pinea testcontainers 1.21.4 explícito
(el BOM de Spring Boot 3.3.x trae 1.19.8, incompatible con Docker Engine
27+/29+ que exige API >=1.44).
```

---

### Commit 38: fix(config): eliminar application.yml de test que tapaba la config real

**Archivos:**
- `src/main/resources/application.yml`
- `src/test/resources/application.yml` (eliminado)

**Mensaje:**
```
fix(config): eliminar src/test/resources/application.yml (shadowing)

Ese archivo (solo spring.profiles.active: test) shadowaba por completo a
src/main/resources/application.yml en el classpath de test - Gradle prioriza
src/test/resources sobre src/main/resources para el mismo path. Resultado:
springdoc/management/resilience4j nunca se cargaban en ningún @SpringBootTest,
enmascarado hasta que un test dependió de esas propiedades para pasar.
@ActiveProfiles("test") en las clases de test ya activa el perfil sin
necesitar este archivo. De paso se saca el bloque spring.profiles de
application.yml (spring.profiles.active con placeholder ahí mismo no aporta:
SPRING_PROFILES_ACTIVE ya lo lee Spring Boot nativamente vía env var).
```

---

### Commit 39: fix(concurrencia): cerrar carrera en el cupo mensual de reservas

**Archivos:**
- `src/main/java/com/clubcore/api/service/impl/ReservaServiceImpl.java`

**Mensaje:**
```
fix(concurrencia): lock por cliente para el chequeo de cupo mensual

Detectado con requests paralelas reales: contra un plan con
limiteReservasMes=1, 5-10 solicitudes simultáneas de reserva para el mismo
cliente pasaban todas el chequeo de cupo y se persistían todas — un
check-then-insert clásico sin ninguna serialización.

Se probó primero con @Transactional + SELECT ... FOR UPDATE sobre la
membresía: no cerró la carrera (verificado, seguían persistiéndose todas).
La solución que sí funciona (verificada con 10 requests paralelas -> 1
única reserva persistida): un lock en memoria por clienteId envolviendo
el chequeo de cupo + el insert, usando TransactionTemplate en vez de
@Transactional — con la anotación, el proxy de Spring commitea DESPUÉS
de que el método retorna, es decir después de liberar el lock si viviera
dentro del cuerpo del método, dejando la misma ventana abierta.
TransactionTemplate commitea de forma síncrona dentro de execute(),
todavía sosteniendo el lock.

ponytail: lock en memoria, válido para una única instancia (la que corre
hoy en la VPS). Si se escala a múltiples réplicas hace falta un lock
distribuido (Redis) o mover el conteo de cupo a un UPDATE atómico
condicionado en SQL.
```

---

## Resumen

Total: **39 commits atómicos** que cuentan la historia completa del proyecto.

- **FASE 1** (5 commits): Setup, config, infra, docs, CI
- **FASE 2** (5 commits): Entidades, DTOs, Mappers, Repositories
- **FASE 2.5** (9 commits): Servicios, motor de estados, eventos async, seguridad (JWT, config)
- **FASE 3** (8 commits): Controllers (Auth, CRUD de todas las entidades, Webhook)
- **FASE 4** (1 commit): Excepciones y manejo de errores
- **FASE 5** (2 commits): Tests unitarios e integración
- **FASE 6** (2 commits): Docker y CI/CD
- **FASE 7** (2 commits): Portfolio (docs + logo + talking points)
- **FASE 8** (9 commits): Mejoras post-deploy — Actuator, manejo de errores, accessDeniedHandler, Resilience4j, rate limiting, Prometheus, Testcontainers, fix de config, fix de carrera en cupo mensual

Cada commit es testeable y coherente. Ninguno "quema" tokens con cambios masivos. La historia es clara para future maintainers (o Mateo mismo revisando en 6 meses).

