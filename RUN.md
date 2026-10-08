# Como levantar ClubCore

## Requisitos

- Docker y Docker Compose v2. **Es lo único que hace falta**: ni JDK ni MySQL en el host.
- Puerto `8080` libre (o `APP_PORT=8090 docker compose up --build`).
- Internet la primera vez (imágenes base y dependencias de Gradle).

## Levantar todo (MySQL + API)

```bash
docker compose up --build
```

- API: http://localhost:8080 — Swagger UI: http://localhost:8080/swagger-ui.html
- No hace falta `.env`: el compose trae valores por defecto **solo para uso local** (contraseñas de la base,
  `JWT_SECRET`, `WEBHOOK_SIGNING_SECRET`). Si definís alguna en un `.env` (copiá `.env.example`), tiene prioridad.
- Corre el perfil `dev`: Flyway aplica `db/migration` (schema) y `db/seed` (datos demo).
- La base **no publica puertos** y la API solo escucha en `127.0.0.1`.
- `docker compose ps` muestra `healthy` cuando está lista. `docker compose down -v` borra también la base.

Para correr la API desde Gradle (necesita JDK 21 y un MySQL en `localhost:3306`): `./gradlew bootRun`
con `DB_PASSWORD` y `JWT_SECRET` en el entorno.

### Variables útiles
| Variable | Para qué |
|---|---|
| `APP_PORT` | Puerto del host (por defecto 8080) |
| `WEBHOOK_SIGNING_SECRET` | Secreto del webhook inbound de pagos |
| `FORWARD_HEADERS_STRATEGY=native` | Solo detrás de un reverse proxy propio: el rate limit usa `X-Forwarded-For` únicamente si la conexión viene de un proxy interno |
| `SWAGGER_ENABLED=true` | Solo en `prod`: vuelve a habilitar Swagger (apagado por defecto) |
| `CORS_ALLOWED_ORIGINS` | Obligatoria en `prod` |

### Webhook de pagos (cómo firmarlo)
`POST /api/v1/webhooks/pagos` exige dos headers: `X-Webhook-Timestamp` (epoch en segundos, ±5 min) y
`X-Webhook-Signature` = HMAC-SHA256 hex de `"<timestamp>.<body>"` con `WEBHOOK_SIGNING_SECRET`.
Body máximo: 64 KB.

```bash
TS=$(date +%s); BODY='{"referencia":"EVT-1","membresiaId":1,"monto":15000,"estado":"PAGADO"}'
SIG=$(printf '%s.%s' "$TS" "$BODY" | openssl dgst -sha256 -hmac "$WEBHOOK_SIGNING_SECRET" -hex | sed 's/^.* //')
curl -X POST http://localhost:8080/api/v1/webhooks/pagos -H 'Content-Type: application/json' \
  -H "X-Webhook-Timestamp: $TS" -H "X-Webhook-Signature: $SIG" -d "$BODY"
```

## Tests

```bash
# Tests rápidos — perfil 'test' con H2 en memoria (sin Docker, sin MySQL)
./gradlew test
```

### Tests de integración (Testcontainers)

`MembresiaIntegrationTest` levanta un contenedor MySQL 8.4 real y verifica Flyway + motor
de estados. Requiere que Docker esté disponible en el host donde corren los tests.

**Con JDK local y Docker en el host:**
```bash
./gradlew test
```
Testcontainers detecta el socket de Docker automáticamente (`/var/run/docker.sock`).

**Sin JDK local — build y tests en contenedor (Docker outside of Docker):**
```bash
docker run --rm \
  -v "$PWD":/app \
  -w /app \
  -v /var/run/docker.sock:/var/run/docker.sock \
  --group-add $(stat -c '%g' /var/run/docker.sock) \
  -e HOME=/app \
  -e TESTCONTAINERS_RYUK_DISABLED=true \
  --network host \
  eclipse-temurin:21-jdk \
  ./gradlew test --no-daemon
```

Detalles del comando:
- `-v /var/run/docker.sock:/var/run/docker.sock` — monta el socket Docker del host (patrón DooD)
- `--group-add $(stat -c '%g' /var/run/docker.sock)` — agrega el grupo del socket al contenedor builder para que la JVM pueda leerlo
- `-e TESTCONTAINERS_RYUK_DISABLED=true` — deshabilita el contenedor Ryuk de limpieza (Ryuk requiere privilegios que no siempre están disponibles en DooD)
- `--network host` — el contenedor builder usa la red del host; así Testcontainers puede conectarse al puerto mapeado del MySQL container

> **Nota:** si el entorno no tiene Docker disponible (CI sin daemon, sandbox restringido),
> los tests de Testcontainers fallan con `DockerClientException` o timeout. En ese caso
> excluir la suite de integración con `-x integrationTest` o un tag de JUnit 5 separado.

## Build de la imagen suelta

```bash
docker build -t clubcore:latest .
```
Para producción el perfil `prod` exige `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y `CORS_ALLOWED_ORIGINS`;
no carga datos demo y no expone Swagger ni `/actuator/info`.

## Usuarios demo (solo perfil dev: `docker compose up`)

| Email                      | Contrasena  | Rol        |
|----------------------------|-------------|------------|
| admin@clubcore.com         | Admin1234!  | ADMIN      |
| entrenador@clubcore.com    | Admin1234!  | ENTRENADOR |
| cliente1@clubcore.com      | Admin1234!  | CLIENTE    |

Nota: el hash en `db/seed/V2__seed_roles.sql` es BCrypt(10) de "Admin1234!", generado y verificado con `htpasswd`.
