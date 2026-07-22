# ---------------------------------------------------------------
# Stage 1: Build
# ---------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Copiar wrapper y definición del build primero para cachear dependencias
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

# Descargar dependencias (cacheable si el build.gradle.kts no cambia)
RUN ./gradlew dependencies --no-daemon --quiet

# Copiar el código fuente y compilar
COPY src ./src
RUN ./gradlew bootJar --no-daemon --quiet -x test

# ---------------------------------------------------------------
# Stage 2: Runtime
# ---------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Usuario no-root para reducir superficie de ataque
RUN groupadd --system clubcore && useradd --system --gid clubcore clubcore
USER clubcore

# Copiar el JAR del stage de build
COPY --from=builder /app/build/libs/clubcore-*.jar app.jar

# Healthcheck sobre el endpoint /actuator/health
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
