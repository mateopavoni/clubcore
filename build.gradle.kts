plugins {
    java
    id("org.springframework.boot") version "3.3.12"
    id("io.spring.dependency-management") version "1.1.7"
    // Expone commit/branch/build-time en /actuator/info
    id("com.gorylenko.gradle-git-properties") version "2.4.2"
}

group = "com.clubcore"
version = "0.1.0"

configure<com.gorylenko.GitPropertiesPluginExtension> {
    // No romper el build si se compila desde un tarball sin .git (ej. algunos entornos de deploy)
    failOnNoGitDirectory = false
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Database
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // Lombok (compileOnly + annotationProcessor)
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // MapStruct — el binding de Lombok debe ir ANTES del processor de MapStruct
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // Resilience4j — retry + circuit breaker en webhook saliente a n8n
    // Versión 2.2.x es la compatible con Spring Boot 3.2.x+
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    // AOP es requisito de las anotaciones @Retry/@CircuitBreaker de Resilience4j
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Bucket4j — rate limiting en endpoints públicos (login + webhooks inbound)
    // Artefacto jdk17-core: sin dependencias de Redis/Ignite, solo el núcleo para JDK 17+
    implementation("com.bucket4j:bucket4j_jdk17-core:8.19.0")

    // Micrometer Prometheus — expone /actuator/prometheus para scraping
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Cache — catálogos de lectura pesada y baja escritura (planes)
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")

    // Logging JSON estructurado (perfil prod) — legible por Docker/Loki/ELK
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
    // Lombok en tests (para builders en fixtures)
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    // Testcontainers — tests de integración contra MySQL real
    // Se pina 1.21.4 explícitamente: Spring Boot 3.3.x BOM gestiona 1.19.8 (docker-java 3.3.x)
    // que negocia API v1.32, incompatible con Docker Engine 27+/29+ (requiere API >=1.44).
    // Testcontainers 1.21.x incluye docker-java con soporte para la API moderna.
    testImplementation("org.testcontainers:testcontainers:2.0.5")
    testImplementation("org.testcontainers:mysql:2.0.5")
    testImplementation("org.testcontainers:junit-jupiter:2.0.5")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Perfil de test con H2 y Flyway deshabilitado
    systemProperty("spring.profiles.active", "test")
}
