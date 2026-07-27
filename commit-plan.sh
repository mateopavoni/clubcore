#!/bin/bash

# commit-plan.sh — Script de aplicación de commits atómicos
# Uso: ./commit-plan.sh [--from <n>] [--to <n>] [--dry-run]
# Ejemplo: ./commit-plan.sh --from 1 --to 5 (commits 1 a 5)
#          ./commit-plan.sh --dry-run (prueba sin hacer commits)
#
# Convenciones:
# - Cada commit es una unidad lógica y testeable
# - Formato: Conventional Commits (feat, fix, config, etc.)
# - Sin Co-Authored-By (autoría a Mateo únicamente)
# - Sin fechas falsas (usa timestamp actual)
# - Sin --force o rebase destructivo

set -e

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

DRY_RUN=false
FROM_COMMIT=1
TO_COMMIT=39  # Total de commits

# Parsear argumentos
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --from)
            FROM_COMMIT="$2"
            shift 2
            ;;
        --to)
            TO_COMMIT="$2"
            shift 2
            ;;
        --help|-h)
            echo "Uso: $0 [--from <n>] [--to <n>] [--dry-run]"
            echo ""
            echo "Opciones:"
            echo "  --from <n>    Comenzar desde commit n (default: 1)"
            echo "  --to <n>      Terminar en commit n (default: 30)"
            echo "  --dry-run     Mostrar qué haría sin hacer commits"
            echo "  --help        Mostrar esta ayuda"
            exit 0
            ;;
        *)
            echo -e "${RED}Opción desconocida: $1${NC}"
            exit 1
            ;;
    esac
done

# ===== VERIFICACIONES =====

# 1. Verificar que es un repo git
if [ ! -d ".git" ]; then
    echo -e "${RED}Error: no es un repositorio git. Ejecutar 'git init' primero.${NC}"
    exit 1
fi

# 2. Verificar que el working tree está limpio (sin cambios sin stagear)
if ! git diff-index --quiet HEAD --; then
    echo -e "${RED}Error: working tree tiene cambios sin stagear. Ejecutar 'git status' primero.${NC}"
    exit 1
fi

# 3. Validar rango de commits
if [ "$FROM_COMMIT" -lt 1 ] || [ "$FROM_COMMIT" -gt 39 ]; then
    echo -e "${RED}Error: FROM debe estar entre 1 y 39${NC}"
    exit 1
fi

if [ "$TO_COMMIT" -lt 1 ] || [ "$TO_COMMIT" -gt 39 ]; then
    echo -e "${RED}Error: TO debe estar entre 1 y 39${NC}"
    exit 1
fi

if [ "$FROM_COMMIT" -gt "$TO_COMMIT" ]; then
    echo -e "${RED}Error: FROM ($FROM_COMMIT) debe ser <= TO ($TO_COMMIT)${NC}"
    exit 1
fi

# ===== DATOS DE COMMITS =====
# Estructura: numero;tipo;titulo;descripcion;archivos
# Archivos separados por "|"

declare -A COMMITS
COMMITS[1]="1;scaffold;configurar Gradle Kotlin DSL + Spring Boot 3.3.x + Java 21;JDK 21 LTS, Spring Boot 3.3.x, dependencias core;build.gradle.kts|.gitignore|.env.example|settings.gradle.kts"

COMMITS[2]="2;config;agregar application-*.yml para dev/prod/test;Perfiles Spring para cada entorno;src/main/resources/application.yml|src/main/resources/application-dev.yml|src/main/resources/application-prod.yml|src/main/resources/application-test.yml"

COMMITS[3]="3;infra;agregar docker-compose.yml con MySQL 8;Docker Compose con MySQL 8.4, Dockerfile multi-stage;docker-compose.yml|.dockerignore|Dockerfile"

COMMITS[4]="4;docs;README + ARCHITECTURE + RUN + TASKS + LICENSE;Documentación completa del proyecto;README.md|ARCHITECTURE.md|RUN.md|TASKS.md|LICENSE|MANUAL-PRUEBAS.md|.claude/CLAUDE.md|.claude/commands/cambio.md"

COMMITS[5]="5;ci;agregar GitHub Actions workflow (CI/CD);Workflow compilar + testear;.github/workflows/ci.yml"

COMMITS[6]="6;entity;crear entidades JPA (Usuario, Cliente, Plan, Membresia, Pago, Reserva, Acceso, Rutina);Entidades y enums del dominio;src/main/java/com/clubcore/api/entity/Usuario.java|src/main/java/com/clubcore/api/entity/Cliente.java|src/main/java/com/clubcore/api/entity/Plan.java|src/main/java/com/clubcore/api/entity/Membresia.java|src/main/java/com/clubcore/api/entity/Pago.java|src/main/java/com/clubcore/api/entity/Reserva.java|src/main/java/com/clubcore/api/entity/Acceso.java|src/main/java/com/clubcore/api/entity/Rutina.java|src/main/java/com/clubcore/api/entity/enums/Rol.java|src/main/java/com/clubcore/api/entity/enums/EstadoMembresia.java|src/main/java/com/clubcore/api/entity/enums/EstadoPago.java|src/main/java/com/clubcore/api/entity/enums/EstadoReserva.java"

COMMITS[7]="7;db;crear migraciones Flyway (schema + seed);V1 schema completo, V2 seed de usuarios y planes;src/main/resources/db/migration/V1__init_schema.sql|src/main/resources/db/migration/V2__seed_roles.sql"

COMMITS[8]="8;dto;crear DTOs (request + response);Request/Response DTOs con validaciones;src/main/java/com/clubcore/api/dto/request/CreateClienteRequest.java|src/main/java/com/clubcore/api/dto/request/CreateMembresiaRequest.java|src/main/java/com/clubcore/api/dto/request/CreatePagoRequest.java|src/main/java/com/clubcore/api/dto/request/CreateReservaRequest.java|src/main/java/com/clubcore/api/dto/request/CreateAccesoRequest.java|src/main/java/com/clubcore/api/dto/request/CreateRutinaRequest.java|src/main/java/com/clubcore/api/dto/request/LoginRequest.java|src/main/java/com/clubcore/api/dto/response/ClienteResponse.java|src/main/java/com/clubcore/api/dto/response/MembresiaResponse.java|src/main/java/com/clubcore/api/dto/response/PagoResponse.java|src/main/java/com/clubcore/api/dto/response/ReservaResponse.java|src/main/java/com/clubcore/api/dto/response/AccesoResponse.java|src/main/java/com/clubcore/api/dto/response/RutinaResponse.java|src/main/java/com/clubcore/api/dto/response/PlanResponse.java"

COMMITS[9]="9;mapper;crear MapStruct mappers (Entity ↔ DTO);Mappers Entity-DTO generados en compile-time;src/main/java/com/clubcore/api/mapper/ClienteMapper.java|src/main/java/com/clubcore/api/mapper/MembresiaMapper.java|src/main/java/com/clubcore/api/mapper/PagoMapper.java|src/main/java/com/clubcore/api/mapper/ReservaMapper.java|src/main/java/com/clubcore/api/mapper/AccesoMapper.java|src/main/java/com/clubcore/api/mapper/RutinaMapper.java|src/main/java/com/clubcore/api/mapper/PlanMapper.java"

COMMITS[10]="10;repository;crear JpaRepositories;JpaRepository con queries derivadas;src/main/java/com/clubcore/api/repository/UsuarioRepository.java|src/main/java/com/clubcore/api/repository/ClienteRepository.java|src/main/java/com/clubcore/api/repository/PlanRepository.java|src/main/java/com/clubcore/api/repository/MembresiaRepository.java|src/main/java/com/clubcore/api/repository/PagoRepository.java|src/main/java/com/clubcore/api/repository/ReservaRepository.java|src/main/java/com/clubcore/api/repository/AccesoRepository.java|src/main/java/com/clubcore/api/repository/RutinaRepository.java"

COMMITS[11]="11;service;crear MembresiaStateService (motor de estados);Motor de validación de estados on-demand + job @Scheduled;src/main/java/com/clubcore/api/service/MembresiaStateService.java|src/main/java/com/clubcore/api/service/impl/MembresiaStateServiceImpl.java"

COMMITS[12]="12;service;crear servicios de dominio (Cliente, Membresia, Pago, Reserva, Acceso, Plan, Rutina);Servicios con lógica de negocio y transaccionalidad;src/main/java/com/clubcore/api/service/ClienteService.java|src/main/java/com/clubcore/api/service/impl/ClienteServiceImpl.java|src/main/java/com/clubcore/api/service/MembresiaService.java|src/main/java/com/clubcore/api/service/impl/MembresiaServiceImpl.java|src/main/java/com/clubcore/api/service/PagoService.java|src/main/java/com/clubcore/api/service/impl/PagoServiceImpl.java|src/main/java/com/clubcore/api/service/ReservaService.java|src/main/java/com/clubcore/api/service/impl/ReservaServiceImpl.java|src/main/java/com/clubcore/api/service/AccesoService.java|src/main/java/com/clubcore/api/service/impl/AccesoServiceImpl.java|src/main/java/com/clubcore/api/service/PlanService.java|src/main/java/com/clubcore/api/service/impl/PlanServiceImpl.java|src/main/java/com/clubcore/api/service/RutinaService.java|src/main/java/com/clubcore/api/service/impl/RutinaServiceImpl.java"

COMMITS[13]="13;event;crear MembresiaPorVencerEvent + NotificationDispatchService;Evento de membresía + dispatcher async a n8n;src/main/java/com/clubcore/api/event/MembresiaPorVencerEvent.java|src/main/java/com/clubcore/api/service/NotificationDispatchService.java"

COMMITS[14]="14;security;crear JwtService + JwtFilter + UserDetailsService;JWT stateless + extracción y validación de tokens;src/main/java/com/clubcore/api/security/JwtService.java|src/main/java/com/clubcore/api/security/JwtFilter.java|src/main/java/com/clubcore/api/security/UserDetailsServiceImpl.java"

COMMITS[15]="15;config;agregar SecurityConfig + AsyncConfig + SwaggerConfig;Configuración de seguridad, async y documentación API;src/main/java/com/clubcore/api/config/SecurityConfig.java|src/main/java/com/clubcore/api/config/AsyncConfig.java|src/main/java/com/clubcore/api/config/SwaggerConfig.java"

COMMITS[16]="16;controller;agregar AuthController (login);Endpoint de autenticación POST /api/v1/auth/login;src/main/java/com/clubcore/api/controller/AuthController.java"

COMMITS[17]="17;controller;agregar ClienteController (CRUD paginado);CRUD de clientes con paginación y RBAC;src/main/java/com/clubcore/api/controller/ClienteController.java"

COMMITS[18]="18;controller;agregar MembresiaController (CRUD + suspend);CRUD de membresías con suspensión manual;src/main/java/com/clubcore/api/controller/MembresiaController.java"

COMMITS[19]="19;controller;agregar PagoController (CRUD);CRUD de pagos con validación de estado de membresía;src/main/java/com/clubcore/api/controller/PagoController.java"

COMMITS[20]="20;controller;agregar ReservaController (CRUD + validación de estado);Crear reservas validando estado de membresía;src/main/java/com/clubcore/api/controller/ReservaController.java"

COMMITS[21]="21;controller;agregar AccesoController (check-in);Check-in con validación de estado y registro de rechazo;src/main/java/com/clubcore/api/controller/AccesoController.java"

COMMITS[22]="22;controller;agregar PlanController + RutinaController (CRUD);CRUD de planes y rutinas;src/main/java/com/clubcore/api/controller/PlanController.java|src/main/java/com/clubcore/api/controller/RutinaController.java"

COMMITS[23]="23;controller;agregar WebhookController (inbound);Webhook público para confirmación de pagos externos;src/main/java/com/clubcore/api/controller/WebhookController.java"

COMMITS[24]="24;exception;crear GlobalExceptionHandler;Manejo global de excepciones con respuestas JSON;src/main/java/com/clubcore/api/exception/ResourceNotFoundException.java|src/main/java/com/clubcore/api/exception/MembresiaInvalidaException.java|src/main/java/com/clubcore/api/exception/GlobalExceptionHandler.java"

COMMITS[25]="25;test;crear MembresiaStateServiceTest (10 casos);Unit tests del motor de estados con Mockito;src/test/java/com/clubcore/api/service/MembresiaStateServiceTest.java"

COMMITS[26]="26;test;crear AuthControllerTest + PagoControllerTest;Tests de controllers con @WebMvcTest;src/test/java/com/clubcore/api/controller/AuthControllerTest.java|src/test/java/com/clubcore/api/controller/PagoControllerTest.java"

COMMITS[27]="27;docker;build multi-stage + HEALTHCHECK;Dockerfile con two-stage build y health check;Dockerfile"

COMMITS[28]="28;ci;GitHub Actions workflow compilar + testear;Workflow CI para build + tests en cada push;.github/workflows/ci.yml"

COMMITS[29]="29;portfolio;agregar PORTFOLIO.md (LinkedIn + talking points + logo);Defensa del proyecto: post, entrevista, logo;PORTFOLIO.md"

COMMITS[30]="30;portfolio;agregar commit-plan.md + commit-plan.sh;Plan de commits + script de aplicación;commit-plan.md|commit-plan.sh"

# FASE 8 — mejoras post-deploy, salieron de probar clubcore en vivo contra la VPS real
COMMITS[31]="31;fix;agregar Actuator para healthcheck real;Dockerfile apuntaba a /actuator/health sin la dependencia agregada;build.gradle.kts|src/main/resources/application.yml"

COMMITS[32]="32;fix;status codes correctos en GlobalExceptionHandler;MissingServletRequestParameterException (400) y DataIntegrityViolationException (409) en vez de 500;src/main/java/com/clubcore/api/exception/GlobalExceptionHandler.java"

COMMITS[33]="33;fix;accessDeniedHandler explícito en SecurityConfig;401 en vez de 403 en denegaciones de rol a nivel de filtro (ej. /actuator/prometheus);src/main/java/com/clubcore/api/config/SecurityConfig.java"

COMMITS[34]="34;feat;Resilience4j retry + circuit breaker en webhook a n8n;WebhookDispatcher con fallback en @Retry, no en @CircuitBreaker;build.gradle.kts|src/main/java/com/clubcore/api/service/WebhookDispatcher.java|src/main/java/com/clubcore/api/service/NotificationDispatchService.java|src/main/resources/application.yml|src/test/java/com/clubcore/api/service/WebhookDispatcherResilienceTest.java"

COMMITS[35]="35;feat;rate limiting con Bucket4j en login y webhooks;5 req/min login, 20 req/min webhooks, en memoria por IP;build.gradle.kts|src/main/java/com/clubcore/api/security/RateLimitFilter.java|src/main/java/com/clubcore/api/config/SecurityConfig.java"

COMMITS[36]="36;feat;métricas Prometheus vía Micrometer (solo ADMIN);/actuator/prometheus y /metrics protegidos, /health público;build.gradle.kts|src/main/resources/application.yml|src/main/java/com/clubcore/api/config/SecurityConfig.java"

COMMITS[37]="37;test;Testcontainers contra MySQL real;Complementa los tests H2 con 5 tests de integración contra MySQL 8.4 real;build.gradle.kts|src/test/java/com/clubcore/api/integration/MembresiaIntegrationTest.java|RUN.md"

COMMITS[38]="38;fix;eliminar application.yml de test que tapaba la config real;src/test/resources shadowea a src/main/resources en el classpath de test;src/main/resources/application.yml|src/test/resources/application.yml"

COMMITS[39]="39;fix;lock por cliente para el cupo mensual de reservas;check-then-insert sin serializar - SELECT FOR UPDATE no alcanzó, se usa lock en memoria + TransactionTemplate;src/main/java/com/clubcore/api/service/impl/ReservaServiceImpl.java"

# ===== FUNCIONES =====

apply_commit() {
    local n=$1
    local commit_data="${COMMITS[$n]}"

    IFS=';' read -r num tipo titulo descripcion archivos_str <<< "$commit_data"

    # Parsear archivos
    IFS='|' read -ra archivos <<< "$archivos_str"

    # Construir mensaje
    local message="${tipo}: ${titulo}"

    if [ "$DRY_RUN" = true ]; then
        echo -e "${YELLOW}[DRY-RUN] Commit $n: ${message}${NC}"
        echo "  Archivos: ${archivos[@]}"
        return 0
    fi

    echo -e "${GREEN}Commit $n: ${message}${NC}"

    # Stage files
    git add -- "${archivos[@]}"

    # Commit
    git commit -m "${message}"

    echo -e "${GREEN}  ✓ Aplicado${NC}"
}

# ===== MAIN =====

echo ""
echo "========================================="
echo "ClubCore — commit-plan.sh"
echo "========================================="
echo ""

if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}MODO DRY-RUN (sin hacer commits)${NC}"
    echo ""
fi

echo "Commits a aplicar: $FROM_COMMIT a $TO_COMMIT"
echo "Total: $((TO_COMMIT - FROM_COMMIT + 1))"
echo ""

# Loop sobre rango
for i in $(seq "$FROM_COMMIT" "$TO_COMMIT"); do
    if [ -z "${COMMITS[$i]}" ]; then
        echo -e "${RED}Error: Commit $i no definido${NC}"
        exit 1
    fi

    apply_commit "$i"

    # Small delay para evitar saturation
    sleep 0.1
done

echo ""
if [ "$DRY_RUN" = true ]; then
    echo -e "${YELLOW}Fin del dry-run. Sin cambios hechos.${NC}"
else
    echo -e "${GREEN}Todos los commits aplicados exitosamente.${NC}"
    echo ""
    echo "Estado actual:"
    git log --oneline -5
fi

echo ""
echo "========================================="
echo "Importante: NO hacer 'git push' aquí."
echo "Usar 'git push' manualmente cuando esté"
echo "seguro de los cambios."
echo "========================================="
echo ""
