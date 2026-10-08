# PROJECT.md

**Última verificación:** 2026-10-08.

## Qué es
API REST para gestionar un instituto de salud / complejo deportivo: clientes, planes, membresías, pagos, reservas, accesos (check-in) y rutinas. Sin frontend: la "demo" era Swagger UI. Su tesis técnica (README): **el estado de una membresía nunca es una columna cacheada**, se recalcula on-demand en cada decisión crítica (reserva, check-in).

## Para quién
Proyecto de portfolio de **Mateo Pavoni** (Córdoba, Argentina); no hay cliente real. Licencia propietaria, publicado solo para evaluación (`LICENSE`).

## Stack (FACT)
- Java 21, Spring Boot **3.5.16** (era 3.3.12 hasta la auditoría) (Web, Data JPA, Security, Validation, Actuator, AOP), Gradle 8.10 con wrapper.
- MySQL 8.4 + Flyway; MapStruct, Lombok; jjwt 0.12.6 (JWT HS256); springdoc 2.8.17.
- Resilience4j (retry + circuit breaker del webhook saliente a n8n), Bucket4j (rate limit), Caffeine (caché de planes y buckets), Micrometer/Prometheus, logging JSON en `prod`.
- Tests: JUnit 5, Spring Boot Test, H2, Testcontainers (MySQL real).

## Estado
**Archivado.** El deploy (Dokku en una VPS propia, `clubcore-api.mateopavoni.com.ar`) está dado de baja y el CI ya no despliega. Se corre completo en local con `docker compose up --build`. Ver `CURRENT_STATE.md`.

## Tamaño
~120 archivos versionados, 79 commits (68 de Mateo Pavoni + 11 de dependabot), 2 migraciones, 9 controllers, **53 tests**.

## Lo que NO es
- No es multi-instancia: el lock de reservas, el rate limit y los buckets son locales a la JVM.
- No tiene registro público de usuarios (`POST /auth/login` es lo único público de auth; los usuarios los crea un ADMIN).
- No tiene frontend ni subida de archivos.
