# Como levantar ClubCore

## Requisitos

- Docker y Docker Compose
- Java 21 (JDK — para `./gradlew bootRun`)
- O solo Docker (para el build en contenedor)

## Variables de entorno

```bash
cp .env.example .env
```

Completar en `.env`:
- `DB_PASSWORD` — contrasena de MySQL para el usuario `clubcore_user`
- `DB_ROOT_PASSWORD` — contrasena root de MySQL
- `JWT_SECRET` — string Base64 de 64+ bytes. Generar con: `openssl rand -base64 64`

## Levantar (dev)

```bash
# 1. Levantar MySQL
docker-compose up -d

# 2. Correr la aplicacion (Flyway aplica las migraciones automaticamente)
./gradlew bootRun

# O con el perfil explicitamente:
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

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

## Build Docker

```bash
docker build -t clubcore:latest .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:mysql://... \
  -e DB_USERNAME=clubcore_user \
  -e DB_PASSWORD=... \
  -e JWT_SECRET=... \
  clubcore:latest
```

## Usuarios seed (disponibles con docker-compose up + bootRun)

| Email                      | Contrasena  | Rol        |
|----------------------------|-------------|------------|
| admin@clubcore.com         | Admin1234!  | ADMIN      |
| entrenador@clubcore.com    | Admin1234!  | ENTRENADOR |
| cliente1@clubcore.com      | Admin1234!  | CLIENTE    |

Nota: el hash en V2__seed_roles.sql es BCrypt(10) de "Admin1234!", generado y verificado con `htpasswd`.
