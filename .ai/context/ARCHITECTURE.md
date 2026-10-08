# ARCHITECTURE.md

**Última verificación:** 2026-10-08. Complementa el `ARCHITECTURE.md` de la raíz (diseño y "por qué" del dominio); acá lo operativo.

## Capas (FACT, `src/main/java/com/clubcore/api/`)
`controller` (9) → `service` / `service.impl` → `repository` (Spring Data JPA, 8) → MySQL. `entity` (12), `dto/request` + `dto/response`, `mapper` (MapStruct), `security`, `config`, `event`, `exception`.

## Perfiles Spring (importante)
| Perfil | Base de datos | Flyway | Swagger | `/actuator/info` | CORS |
|---|---|---|---|---|---|
| `dev` | `localhost:3306` (el compose lo pisa con `SPRING_DATASOURCE_URL`) | `db/migration` **+ `db/seed`** | sí | sí | `*` si no se define |
| `prod` | `DB_URL` (obligatoria) | solo `db/migration` | **apagado** salvo `SWAGGER_ENABLED=true` | apagado | `CORS_ALLOWED_ORIGINS` obligatoria |
| `test` | H2 (Testcontainers MySQL en el test de integración) | desactivado / ver test | — | — | — |

`db/seed/V2__seed_roles.sql` crea el ADMIN, el ENTRENADOR, un CLIENTE y planes demo con contraseña pública `Admin1234!`. **Solo `dev` lo carga**; `SeedScopeTest` falla si `prod` lo incluye.

## Pipeline de seguridad
`RateLimitFilter` (login 5/min, webhooks 20/min, por IP de la conexión) → `WebhookSignatureFilter` (solo `/api/v1/webhooks/pagos`) → `JwtFilter` (carga el usuario de la base en cada request; el JWT no lleva el rol) → `@PreAuthorize` por método. Sesión stateless. Públicos: `/auth/login`, `/webhooks/**` (con HMAC), Swagger (si está habilitado), `/actuator/health`.

## Webhook inbound de pagos (FACT, probado)
Headers `X-Webhook-Timestamp` (epoch, ±5 min) y `X-Webhook-Signature` = HMAC-SHA256 hex de `"<timestamp>.<body>"`. Body máximo 64 KB (413). Sin `WEBHOOK_SIGNING_SECRET` rechaza todo (fail-closed). El procesamiento es idempotente por `referencia` (no duplica pagos); el timestamp existe para impedir que un evento viejo capturado se reenvíe y pise un estado más nuevo.

## Webhook saliente
`MembresiaPorVencerEvent` (job `@Scheduled` 08:00) → `NotificationDispatchService` (`@Async`) → `WebhookDispatcher` → n8n, con retry + backoff exponencial y circuit breaker (Resilience4j). Se desactiva dejando `N8N_WEBHOOK_URL` vacío.

## Concurrencia de reservas
El cupo mensual se cierra con un lock por cliente (`ConcurrentHashMap<Long, Lock>` en `ReservaServiceImpl`) + `TransactionTemplate`. **Solo serializa dentro de una JVM**: con 2+ réplicas deja de garantizar el cupo.

## Cómo se corre
`docker-compose.yml`: `mysql` (8.4, healthcheck, **sin puerto publicado**) + `app` (build del `Dockerfile`, perfil `dev`, `127.0.0.1:8080`). Secretos por defecto solo-locales, pisables con `.env`.

## Historial de deploy
Producción fue un Dokku en una VPS propia: el job `deploy-dokku` de GitHub Actions hacía push forzado, y `docker-build` publicaba la imagen en GHCR. **Ambos jobs se borraron el 2026-10-08**; `ci.yml` solo corre build + tests. `DeployInfoContributor` (lee `GIT_REV`) quedó como código inocuo.
