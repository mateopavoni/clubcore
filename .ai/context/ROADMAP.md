# ROADMAP.md — mejoras posibles

**Última verificación:** 2026-10-08. Sugerencias priorizadas por valor/esfuerzo. Ninguna está hecha salvo que lo diga.

## Para que sea más sólido como proyecto de portfolio (esfuerzo chico/mediano)
1. **Frontend mínimo** (React/Next o HTMX) para reservas y check-in: hoy la "demo" era solo Swagger; un cliente visual hace el proyecto mucho más fácil de mostrar. *(mediano)*
2. **GIF/capturas en el README** del flujo de reserva con cupo agotado y de la demo de concurrencia. *(chico)*
3. **`docker compose --profile demo`** que además levante n8n con un flujo de ejemplo para el webhook saliente. *(chico)*
4. **Datos demo más realistas** (más clientes, membresías vencidas y en deuda) para lucir el motor de estados. *(chico)*
5. **Cobertura con JaCoCo** + badge en el README; Spotless/Checkstyle como formateador. *(chico)*

## Robustez (esfuerzo mediano)
6. **Escalar a más de una instancia:** reemplazar el lock `ConcurrentHashMap` por lock pesimista de base (`SELECT … FOR UPDATE` con transacción bien delimitada) o `GET_LOCK` de MySQL, y el rate limit por `ProxyManager` de Bucket4j sobre Redis. Es la limitación de diseño más importante. *(mediano)*
7. **Webhook inbound con idempotency key y tabla de eventos** (hoy solo hay upsert por `referencia` + ventana de timestamp): permite auditar y reintentar. *(mediano)*
8. **Paginación obligatoria** (`Pageable` con `size` máximo) en todos los listados. *(chico)*
9. **Refresh tokens / revocación**: hoy el JWT dura 24 h y no se puede revocar (el rol sí se relee de la base en cada request). *(mediano)*
10. **Auditoría** (quién cambió qué y cuándo): Spring Data Envers o una tabla de eventos. *(mediano)*

## Mantenimiento (esfuerzo mediano)
11. **Migrar a Spring Boot 4** (Spring Framework 7, Jackson 3): cierra el único aviso crítico abierto y vuelve a un release con soporte. Hacerlo en una rama, con la suite de 53 tests + el script de la API como red de seguridad. *(mediano/grande)*
12. **Dependency lock** (`gradle.lockfile`) y escaneo de dependencias/imagen (Trivy o OSV) en el CI. *(chico)*
13. **Quitar `DeployInfoContributor`** y el `GIT_REV` si no se vuelve a desplegar en Dokku. *(chico)*
