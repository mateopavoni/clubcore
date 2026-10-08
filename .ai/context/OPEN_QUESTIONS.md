# OPEN_QUESTIONS.md

**Última verificación:** 2026-10-08. Decisiones que requieren al dueño (Mateo Pavoni).

1. **Secrets de GitHub `DOKKU_SSH_KEY`, `DOKKU_HOST`, `DOKKU_APP`.** El workflow que los usaba se borró; los secrets probablemente siguen en *Settings → Secrets*, y la clave pública puede seguir autorizada en la VPS. ¿Se borran? (acción sobre la cuenta: no se hizo sin autorización.)
2. **Ramas `origin/dependabot/*` (11).** Siguen abiertas; varias proponen saltos mayores (Spring Boot 4.1, springdoc 3, Testcontainers 2). Con la nueva regla `ignore` de `dependabot.yml` no se regenerarán. ¿Se cierran/borran los PR viejos?
3. **¿Reactivar alguna vez?** Si sí: resolver `ROADMAP.md` (sobre todo lock distribuido y rate limit con Redis si hay más de una instancia, y actualizar Spring Boot) y revisar `KNOWN_ISSUES.md`.
4. **DNS en Cloudflare:** el registro `clubcore-api.mateopavoni.com.ar` ya no apunta a nada vivo.
5. **`LICENSE` vs badge `proprietary`:** verificar que coincidan (el agente de exploración no lo comprobó a fondo).
6. **Meta local (`PORTFOLIO.md`, `.claude/CLAUDE.md`, `TASKS.md`)** desactualizada: corregir antes de reusar.
