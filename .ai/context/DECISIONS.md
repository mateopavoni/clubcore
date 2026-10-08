# DECISIONS.md

**Última verificación:** 2026-10-08. **(DECISION)** la tomó el dueño y está en el código/commits; **(AUDITORIA)** se tomó el 2026-10-08 y es reversible.

## Fase 3 — ¿RAG o subagente de mantenimiento?
**No.** Repo chico (~5.000 LOC), estable y archivado. Reconsiderar solo si se reactiva y crece.

## Dominio
**(DECISION) El estado de la membresía no se cachea.** Se recalcula en cada decisión crítica; `estado_cacheado` es solo para listados.
**(DECISION) Lock de JVM por cliente + `TransactionTemplate` para el cupo mensual.** El primer intento (`@Transactional` + `SELECT … FOR UPDATE`) dejaba pasar las N requests paralelas porque el commit de Spring ocurre después de retornar el método. Queda como test de regresión. Costo asumido: solo una instancia.
**(DECISION) El fallback del webhook saliente vive en `@CircuitBreaker`, no en `@Retry`** (el `@Retry` agota intentos y propaga la excepción sin fallback).
**(DECISION) Propiedad de objetos devuelve 404** en vez de 403.
**(DECISION) Webhook inbound fail-closed:** sin secreto configurado rechaza todo.

## Baja del proyecto y auditoría (2026-10-08)
**(AUDITORIA) Archivar y quitar el deploy.** Se borraron del CI los jobs `docker-build` (GHCR) y `deploy-dokku`; queda build + tests. Dependabot ignora saltos de versión mayor (Spring Boot 4, springdoc 3, Testcontainers 2 son migraciones, no actualizaciones).
**(AUDITORIA) Datos demo fuera de `db/migration`.** `V2__seed_roles.sql` se movió a `db/seed` (se conservó el nombre para no alterar el checksum de bases ya migradas) y solo el perfil `dev` lo carga. Antes corría en todos los perfiles: una base nueva de `prod` nacía con un ADMIN de contraseña pública.
**(AUDITORIA) Compose con el perfil `dev`, no `prod`.** `prod` exige `DB_URL`, `CORS_ALLOWED_ORIGINS` y no carga el seed, así que no habría con qué iniciar sesión. `dev` + `SPRING_DATASOURCE_URL` apuntando al servicio `mysql` es lo más simple; la base no publica puertos y la API escucha solo en loopback.
**(AUDITORIA) Rate limit por IP de la conexión**, no por `X-Forwarded-For` (lo controla el atacante). Detrás de un proxy propio: `FORWARD_HEADERS_STRATEGY=native`. Buckets en Caffeine con expiración y tope.
**(AUDITORIA) Webhook firmado con timestamp** (±5 min) y body acotado a 64 KB. Cambia el contrato con el gateway (la firma pasó de `body` a `timestamp.body`); no hay gateway real que migrar.
**(AUDITORIA) Swagger y `/actuator/info` apagados en `prod`** por defecto.
**(AUDITORIA) Spring Boot 3.3.12 → 3.5.16 (no 4.x).** Trivy mostró 8 críticas + 27 altas en las librerías; 3.3.x ya no recibe parches. Se eligió el salto menor (misma API, 53 tests verdes sin cambios de código) y se fijaron Tomcat 10.1.60 y Jackson 2.21.7 sobre el BOM. Boot 4 se dejó como migración aparte (`ROADMAP.md`).
