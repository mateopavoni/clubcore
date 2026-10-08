# CONVENTIONS.md

**Última verificación:** 2026-10-08. Patrones observados; seguilos al agregar código.

- **Español** en mensajes de error, Javadoc y comentarios; identificadores del dominio en español (`Membresia`, `Reserva`), infraestructura en inglés. Los comentarios explican el *por qué* y suelen nombrar el bug que motivó el código (ver `RateLimitFilter`, `WebhookSignatureFilter`).
- **Capas:** controller delgado con `@PreAuthorize` por método → service (interfaz + `impl`) → repository. Entidad ↔ DTO con MapStruct; las entidades no salen por la API.
- **Seguridad por defecto:** todo endpoint nuevo es autenticado; si es público hay que justificarlo en `SecurityConfig`. Chequeo de rol con `@PreAuthorize`; chequeo de **propiedad** del objeto en el service (404, no 403).
- **Errores:** `GlobalExceptionHandler` devuelve `ApiErrorResponse` (mismo shape también en los filtros, que no pasan por `@ControllerAdvice`), sin stack traces.
- **Config:** secretos solo por variable de entorno, sin defaults en `dev`/`prod`; los defaults del `docker-compose.yml` son solo-locales. Datos demo **solo** en `src/main/resources/db/seed` (perfil `dev`), nunca en `db/migration`.
- **Migraciones Flyway:** `V<n>__descripcion.sql`, inmutables una vez aplicadas (cambiar el contenido o el nombre rompe el checksum de las bases existentes).
- **Tests:** un test por bug corregido, escrito primero para verlo fallar. Los que tocan MySQL real usan Testcontainers; los de filtros y controllers van con MockMvc/mocks. Los tests de config leen el YAML del perfil (ver `SeedScopeTest`).
- **Correr tests sin JDK local:** `docker run … eclipse-temurin:21-jdk ./gradlew test` con el socket de Docker montado y `--network host` (Testcontainers); ver `RUN.md`.
- **Commits:** Conventional Commits (`feat:`, `fix(security):`, `chore:`, `docs:`). Autor: Mateo Pavoni, sin coautores.
