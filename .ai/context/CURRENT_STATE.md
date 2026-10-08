# CURRENT_STATE.md

**Última verificación:** 2026-10-08. Separa lo que se **ejecutó y observó** de lo que no.

## Verificado corriendo (FACT)
| Qué | Resultado |
|---|---|
| Línea base antes de tocar nada (Spring Boot 3.3.12) | 42 tests, 0 fallas; el `Dockerfile` construye y la imagen sí trae `curl` |
| Suite completa tras los arreglos (`./gradlew clean test` en contenedor JDK 21 + Testcontainers) | **53 tests, 0 fallas** (+11 nuevos: rate limit, webhook, alcance del seed/Swagger/actuator) |
| `docker compose up --build` desde cero (volumen borrado) | `mysql` y `app` quedan `healthy` en ~20 s |
| **Recorrido de la API real contra el compose** (script con 18 chequeos) | **18/18 PASS**: login de los 3 roles; permisos por rol (403/200); IDOR bloqueado (404); crear cliente/membresía/reserva y cancelar; webhook sin firma → 401, firmado → 200, **replay de un evento viejo → 401 y el pago no cambia**, timestamp alterado → 401, body de 70 KB → 413; 8 logins con `X-Forwarded-For` rotado → aparece 429; Swagger disponible en dev |
| Snippet de firma del webhook de `RUN.md` | 200 con firma correcta, 401 con una incorrecta |
| Escaneo de secretos en las 79 revisiones (gitleaks) | 0 secretos reales (ver `KNOWN_ISSUES.md`) |

| **Escaneo de la imagen (Trivy)** | Antes: 8 CRITICAL + 27 HIGH en librerías Java. Tras Boot 3.5.16 + Tomcat 10.1.60 + Jackson 2.21.7: **1 CRITICAL** (XsltView, no alcanzable; ver `KNOWN_ISSUES.md`). El recorrido de la API (18/18) se repitió sobre ese build final |

## NO verificado
- **El perfil `prod` arrancando de verdad** (con `DB_URL`, sin seed, Swagger apagado): lo cubren tests de configuración (`SeedScopeTest`) pero no se levantó.
- Comportamiento con **más de una instancia** (lock de reservas y rate limit son por JVM).
- `FORWARD_HEADERS_STRATEGY=native` detrás de un proxy real.
- Que el test de concurrencia no sea intermitente en otras máquinas (pasó las veces que se corrió).
- Estado del repo en GitHub: Secrets, ramas `dependabot/*`, branch protection.

## Qué se hizo en la auditoría del 2026-10-08
- **Deploy:** se quitaron `docker-build` y `deploy-dokku` del CI; README archivado.
- **Local:** `docker-compose.yml` ahora levanta MySQL **y** la API (antes solo MySQL, con puerto 3306 publicado y falla sin `.env`).
- **Seguridad con test:** seed demo solo en `dev`; rate limit no evadible con `X-Forwarded-For` y con memoria acotada; replay del webhook de pagos; Swagger y `/actuator/info` apagados en `prod`.
- **Docs:** README, `RUN.md`, `ARCHITECTURE.md`, `.env.example` y esta carpeta.
- `main` local ya estaba al día con `origin/main`; las 11 ramas `dependabot/*` no se tocaron.

## Meta local desactualizada (gitignored)
`PORTFOLIO.md`, `.claude/CLAUDE.md` y `TASKS.md` pueden seguir diciendo "deployado en Dokku", "27 tests" o describir el seed en `V2`. No se publican; corregirlos antes de reusar su texto.
