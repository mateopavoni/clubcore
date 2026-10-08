# Índice — `.ai/context/`

Generado por una auditoría de context engineering el 2026-10-08, sobre un proyecto **archivado** (su deploy en Dokku ya no existe). **FACT** = verificado ejecutando código, tests, la API o herramientas ese día. **INFERENCIA** = leído en el código sin ejecutarlo; verificalo antes de apoyarte en eso. Si algo de esta carpeta contradice el código, gana el código.

| Si tu tarea toca… | Leé |
|---|---|
| Qué es el proyecto, para quién, en qué estado quedó | `PROJECT.md` |
| Capas, perfiles Spring, seguridad, webhooks, cómo se corre y se deployaba | `ARCHITECTURE.md` |
| Entidades, estados de membresía, roles y permisos, reglas de negocio | `DOMAIN.md` |
| Escribir código nuevo siguiendo el patrón existente | `CONVENTIONS.md` |
| Entender *por qué* algo se hizo así | `DECISIONS.md` |
| Qué se verificó funcionando y qué no | `CURRENT_STATE.md` |
| Seguridad, bugs confirmados y descartados, deuda — **leer antes de re-deployar** | `KNOWN_ISSUES.md` |
| Decisiones que requieren al dueño | `OPEN_QUESTIONS.md` |
| Mejoras posibles, priorizadas | `ROADMAP.md` |

## Cómo correrlo
```bash
docker compose up --build        # MySQL 8.4 + API en http://localhost:8080 (Swagger en /swagger-ui.html)
./gradlew test                   # necesita Docker (Testcontainers); sin JDK local, ver RUN.md
```
Detalle en `RUN.md` (raíz). No hace falta `.env`.

## Regla de este repo
No hay producción: el proyecto está archivado. Antes de volver a exponerlo a internet, leer `KNOWN_ISSUES.md` (sección *Abierto*) y `ROADMAP.md`. **Nunca** deployar el perfil `dev`: trae usuarios con contraseña pública (`Admin1234!`).

`.claude/`, `TASKS.md`, `PORTFOLIO.md`, `MANUAL-PRUEBAS.md`, `commit-plan.*` son meta local, están en `.gitignore` y no se publican. Esta carpeta `.ai/` sí es parte del repo.
