# KNOWN_ISSUES.md

**Última verificación:** 2026-10-08. Severidad explícita. Lo marcado **[ARREGLADO]** tiene test; lo marcado **[DESCARTADO]** se investigó y no era un bug.

## ✅ Arreglados en la auditoría (con test)

| Sev. | Problema | Evidencia previa (FACT) | Arreglo |
|---|---|---|---|
| 🔴 CRÍTICA | **Admin con contraseña pública en cualquier entorno**: `V2__seed_roles.sql` corría en todos los perfiles, así que una base nueva de `prod` nacía con `admin@clubcore.com` / `Admin1234!` (documentada en el README) | lectura de la migración + `application-prod.yml` | El seed pasó a `db/seed`, solo el perfil `dev` lo carga. `SeedScopeTest` falla si `prod` lo incluye |
| 🟠 ALTA | **Rate limit de login evadible**: se confiaba en el primer valor de `X-Forwarded-For` (lo controla el atacante); rotarlo daba un bucket nuevo por intento | `RateLimitFilterTest.rotarXForwardedForNoEvadeElLimite` **falló** sobre el código viejo; en la API real 8 logins con header rotado no daban 429 | `getRemoteAddr()`; `FORWARD_HEADERS_STRATEGY=native` para proxies propios |
| 🟠 ALTA | **Replay del webhook de pagos**: la firma cubría solo el body. Probado en la API real: PAGADO → llega FALLIDO → el atacante reenvía el PAGADO viejo → el pago vuelve a PAGADO | script contra el compose | Firma sobre `timestamp.body`, ventana ±5 min; tests de request vieja, timestamp alterado y sin timestamp |
| 🟡 MEDIA | **Memoria sin tope**: mapas de buckets por IP sin expiración; el webhook (público) leía el body completo antes de validar la firma | lectura de código + test (413 con 70 KB) | Caffeine con expiración y tope de 100.000 IPs; body ≤ 64 KB (413) |
| 🟡 MEDIA | **Swagger con try-it-out y `/actuator/info` (commit/build) públicos** en producción | `application.yml` | Apagados en `prod` salvo `SWAGGER_ENABLED=true` (`SeedScopeTest`) |
| 🟠 ALTA | **Dependencias vulnerables**: Trivy sobre la imagen reportó **8 críticas + 27 altas** en librerías Java (Spring Framework 6.1.x, Tomcat 10.1.41, Jackson, Spring Security…) | `trivy image` | Spring Boot 3.3.12 → **3.5.16**, springdoc 2.6 → 2.8.17, y Tomcat 10.1.60 / Jackson 2.21.7 fijados sobre el BOM. Quedan **0 críticas/altas**, salvo la de abajo |
| 🟡 BAJA | Datos de CI/compose: `docker-compose.yml` publicaba MySQL en el 3306 y fallaba sin `.env` | lectura | La base ya no publica puertos; secretos por defecto solo-locales |

## 🟢 Aceptado — 1 aviso CRÍTICO sin parche en esta línea
**CVE-2026-47884, `spring-webmvc` 6.2.19: RCE en `XsltView`.** Lo arregla solo Spring Framework 7.0.9 (= Spring Boot 4, migración mayor, fuera de alcance). **No alcanzable aquí (INFERENCIA razonada):** es una API REST pura, sin vistas ni ModelAndView/XSLT (0 archivos que los usen). Reevaluar al migrar a Boot 4.

## ❎ Descartados tras investigar (el agente de exploración los marcó como sospechas)
- *"Borrar un cliente deja el usuario huérfano"*: **falso.** Probado: tras `DELETE /clientes/{id}` el login da 401 y el email se puede reutilizar.
- *"`cancelarReserva` no valida el estado"*: **no hay estado `COMPLETADA`** (solo `PENDIENTE|CONFIRMADA|CANCELADA`) y cancelar es **idempotente** (200 `CANCELADA` siempre). No hay nada que proteger.
- *"El `HEALTHCHECK` con `curl` fallaría"*: **falso.** La imagen `eclipse-temurin:21-jre-jammy` trae `curl`; el `Dockerfile` construye y el contenedor queda `healthy`.
- *"El `COPY clubcore-*.jar` puede tomar un `-plain.jar`"*: no ocurre (la imagen construye y arranca).
- *"IDOR en reservas"*: **bloqueado**, probado (404 al reservar a nombre de otro).

## 🟡 Abierto (bajo / de diseño)
- **Solo una instancia:** el lock por cliente de `ReservaServiceImpl` (`ConcurrentHashMap<Long, Lock>`, nunca se limpia, crece con la cantidad de clientes) y los buckets son locales a la JVM. Con 2+ réplicas el cupo mensual deja de estar garantizado y el rate limit se multiplica. Ver `ROADMAP.md`.
- **Spring Boot 3.5.x ya no tiene soporte OSS** (INFERENCIA: 3.5 salió en mayo de 2025 y las ramas OSS duran ~13 meses). La migración a 4.x es el camino; Dependabot ya no abre PRs de saltos mayores.
- `JwtFilter` captura `Exception` genérica y la registra solo a nivel debug (puede ocultar errores reales).
- Sin paginación ni tope de `size` en los listados (INFERENCIA, no se probó con datos grandes).
- `@MockBean` está obsoleto desde Boot 3.4 (warnings en `WebhookControllerTest`); migrar a `@MockitoBean`.
- `estado_cacheado` y `actualizarEstadoCacheado`: posible código muerto (no se verificó quién lo lee).
- CORS cae a `*` con credenciales en perfiles distintos de `prod` si no se define `CORS_ALLOWED_ORIGINS` (el compose lo fija a `http://localhost:8080`).

## ✅ Secretos — historia limpia (FACT)
`gitleaks` sobre las **79 revisiones** (todas las ramas, incluidas las 11 de dependabot): 5 candidatos, **0 secretos reales** — 3 son el encabezado de un JWT truncado con `...` en el README y `commit-plan.md` (no hay ningún JWT completo en toda la historia) y 2 son los secretos dummy de test (`application-test.yml`, `ci.yml`), públicos por diseño. `.env` nunca estuvo versionado; `.env.example` solo trae placeholders. Los hashes bcrypt del seed corresponden a la contraseña pública `Admin1234!` (demo, solo `dev`). No hay nada que purgar de la historia.
