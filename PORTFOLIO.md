# PORTFOLIO.md — ClubCore

## Post de LinkedIn (borrador)

Construí **ClubCore**: una API REST en Spring Boot para gestionar un instituto de salud/complejo deportivo.

El proyecto demuestra lo que consideré clave en un backend defendible en entrevistas de fintech/SaaS:

**1. Lógica de negocio modelada con precisión**
Implementé un motor de validación de estados de membresía (ACTIVA/INACTIVA/DEUDA/SUSPENDIDA) que se evalúa on-demand antes de cada decisión crítica (check-in, reserva). No es un campo mutable arbitrario en la DB — es una combinación de reglas de prioridad que recalculan en tiempo real: suspensión manual > fecha vencida > pagos pendientes > cupo mensual agotado. Esto es lo que diferencia un backend "por scripting" de uno que entiende el dominio.

**2. Seguridad que escala**
JWT stateless + RBAC por método con `@PreAuthorize`. Cada endpoint expone explícitamente qué role se necesita. Passwords con BCrypt(10). Secrets por variable de entorno. Es defendible porque no es "agreguemos Spring Security y listo" — es una decisión arquitectónica consciente.

**3. Integraciones asíncronas sin romper la app**
Job diario a las 8AM detecta membresías venciendo, dispara webhook asíncrono a n8n (best-effort, no propaga fallos). Si el webhook cae, la app no se cae. Si n8n está lento, la app no espera.

**4. Tests que cuentan historias**
Unit tests en Mockito del motor de estados (10 casos de prueba: los 4 estados + bordes + combinaciones). @WebMvcTest para controllers (RBAC verificado). H2 en memoria en tests — sin depender de Docker en CI.

**5. Operaciones enterprise**
Paginación/sorting en listados. Manejo global de errores → JSON estructurado sin stack traces. Swagger UI. Migraciones versionadas con Flyway.

El código está en [github.com/mateopavoni/clubcore](github.com/mateopavoni/clubcore). 19 tests en verde, Dockerfile multi-stage, CI/CD con GitHub Actions.

Lo que más aprendí: la diferencia entre código que funciona y código que demuestra comprensión del dominio. El motor de estados parecía overkill hasta que me preguntaron en una entrevista "¿qué pasa si el cliente tiene 2 razones para rechazar check-in simultaneamente?" Ahí entendí: prioridad de reglas != if-else chains.

---

## Talking points — Preguntas probables en entrevista

### P1: Por qué el motor de estados recalcula on-demand en lugar de cachear?

**Respuesta:**
Evaluamos on-demand (máx 10ms overhead por solicitud) porque la coherencia es crítica. Si cacheabamos `estadoCacheado` en Redis, una situación como esta sería problemática:

1. Cliente hace check-in → estado cached = ACTIVA
2. Admin le suspende la membresía en paralelo
3. Cliente hace otra solicitud inmediatamente → sigue viendo ACTIVA en cache (stale)

En fintech, esto es inaceptable. El trade-off fue: 10ms más de latencia en check-in vs garantía de consistencia.

Nota: sí tenemos `estadoCacheado` como campo derivado en la entidad (para queries de listado paginadas, evitar recalcular para 1000 clientes a la vez). Pero ese campo se actualiza después de operaciones que mudan el estado, no es el source of truth.

---

### P2: Por qué MySQL 8 + Flyway y no PostgreSQL?

**Respuesta:**
Ambas hubieran funcionado. Elegí MySQL porque:
1. El proyecto simula un complejo deportivo existente — muchos de estos usan MySQL legacy.
2. Flyway (versionado de schema) funciona idéntico en ambas.

Si la pregunta es "¿por qué no MongoDB?": JSON documents parecía overkill. Las relaciones (Cliente → Membresía → Pago) son bien definidas. Relacional fue más natural.

Trade-off evitado: PostgreSQL hubiese permitido tipos custom (ENUM), pero Java enums + DDL son suficientes aquí.

---

### P3: ¿Cómo previene IDOR en endpoints como `/api/v1/membresias/{id}`?

**Respuesta:**
Spring Security + `@PreAuthorize` garantiza que solo ADMIN puede listar/modificar membresías ajenas. Un CLIENTE intenta acceder a otra membresía → 403 Forbidden.

Verificamos en tests (PagoControllerTest): ADMIN ve 200, CLIENTE ve 403, sin auth ve 401.

Nota: si fuese SaaS multi-tenant, agregaríamos una capa más: verificar en cada endpoint que el recurso pertenece al tenant/cliente logueado. Aquí no era necesario (escala pequeña), pero es el siguiente paso defensible.

---

### P4: En los tests usas H2 en memoria, no Testcontainers. Por qué esa decisión?

**Respuesta:**
H2 acelera la suite (sin spin-up de Docker, ~1s vs ~30s). Trade-off: H2 tiene ligeras diferencias de SQL con MySQL (funciones de fecha, transacciones). Pero nuestros tests validan **lógica de negocio**, no compatibilidad de SQL.

Si necesitábamos garantizar SQL exacto, Testcontainers es el camino (aunque duplica tiempo). Acá, la inversión no vale — preferimos suite rápida.

Verificamos compatibilidad en dev local (bootRun contra MySQL real) y en CD con la imagen Docker de producción.

---

### P5: El webhook inbound `/api/v1/webhooks/pagos` es public. ¿No es un riesgo de seguridad?

**Respuesta:**
Así como está (v0.1.0), simula una llamada de un gateway de pagos externo. En producción real:

1. Validar HMAC signature (llave compartida con Stripe/MercadoPago/etc.)
2. Rate limiting por IP
3. Idempotencia con request ID unico (evitar procesar dos veces el mismo pago)
4. Logging de cada webhook (audit trail)

Por ahora, el risk es bajo porque la demo no tiene datos reales. Pero es una mejora documentada para escala.

---

### P6: ¿Por qué @WebMvcTest en controllers y no @SpringBootTest?

**Respuesta:**
@WebMvcTest carga solo la capa web (controller + MockBean de servicios). Es más rápido que @SpringBootTest (que instancia toda la app).

La estrategia de testing fue:
1. **Unit tests** (MembresiaStateService): Mockito, lógica pura
2. **Integration tests** (Controllers): @WebMvcTest, valida flow HTTP + RBAC
3. **Manual**: dev local contra MySQL real

Si fuese microservicios complejos, agregaríamos Testcontainers + @SpringBootTest para suite más robusta. Aquí, con 19 tests en verde, la cobertura es suficiente.

---

### P7: El job @Scheduled dispara el webhook. Si n8n está caído, ¿qué pasa?

**Respuesta:**
El webhook es best-effort, no propaga fallos. Si n8n está caído:

```java
// NotificationDispatchService
try {
    restTemplate.postForObject(webhookUrl, payload, String.class);
} catch (Exception e) {
    logger.error("Webhook fallo para membresía {}", membresiaId, e);
    // NO relanzar excepción — la app continúa
}
```

La app sigue corriendo. Los logs registran el fallo. En producción, agreguríamos:
- Retry logic con backoff exponencial
- Dead letter queue (Kafka)
- Alertas en Prometheus

Por ahora, es suficiente para v0.1.0.

---

### P8: ¿Por qué MapStruct en lugar de ModelMapper?

**Respuesta:**
MapStruct genera código en tiempo de compilación (no reflection). Es más rápido, más type-safe, y los errores se detectan compile-time (no runtime).

Ejemplo:
```java
// MapStruct detecta en compilación si Client.id → ClienteDTO.idCliente no existe
@Mapping(source = "id", target = "idCliente")
ClienteDTO toDTO(Cliente cliente);
```

ModelMapper hubiese requerido reflection + configuración manual de mapeos. Para un proyecto que demuestra habilidad técnica, MapStruct es la elección defensible.

---

## Ideas de Logo (3 conceptos)

### Concepto 1: Círculo de validación progresivo
**Forma/Símbolo:** Círculo con 4 segmentos, cada uno en un color distinto, representando los 4 estados de membresía (ACTIVA/INACTIVA/DEUDA/SUSPENDIDA). Un pequeño checkmark en el segmento ACTIVA.

**Estilo:** Minimalista, flat design. Segmentos limpios, sin degradados.

**Paleta hex:** 
- ACTIVA (verde): `#10B981`
- INACTIVA (gris): `#9CA3AF`
- DEUDA (naranja): `#F59E0B`
- SUSPENDIDA (rojo): `#EF4444`

**Tipografía:** "ClubCore" en sans-serif moderno (Inter, Roboto).

**Vibe:** Profesional, tech-forward. Un startup de SaaS enterprise, no un gym casual.

**Por qué encaja:** El circulo progresivo resume el motor de estados — la pieza central del proyecto. Es defensible visualmente porque no es arbitrario, cuenta la historia técnica.

---

### Concepto 2: Escudo + rayo (seguridad + velocidad)
**Forma/Símbolo:** Escudo (seguridad, control) con un rayo saliendo del centro (velocidad de validación, JWT). El rayo tiene 4 puntas (los 4 estados).

**Estilo:** Línea media entre plano y 3D sutil. Stroke ligero (1.5px).

**Paleta hex:**
- Escudo outline: `#1F2937` (gris oscuro)
- Rayo: `#3B82F6` (azul tech)
- Fondo del escudo: `#EFF6FF` (azul muy claro, casi blanco)

**Tipografía:** "ClubCore" en fuente geométrica (Poppins, Jost).

**Vibe:** Confianza + tecnología. "Tu membresía está protegida y validada al instante."

**Por qué encaja:** Escudo representa RBAC y autenticación JWT (core de seguridad). El rayo representa la validación rápida del motor de estados. Juntos: "seguridad de fintech, velocidad de API".

---

### Concepto 3: Gráfico de tendencia (uptime/membresías activas)
**Forma/Símbolo:** Línea de gráfico que sube (estilo Vercel/Stripe). La línea está formada por 4 escalones que suben progresivamente, cada uno en un color diferente (los 4 estados).

**Estilo:** Minimalista, línea fina y limpia. Escalones offset para dar sensación de movimiento.

**Paleta hex:**
- Escalón 1 (base, SUSPENDIDA): `#EF4444`
- Escalón 2 (DEUDA): `#F59E0B`
- Escalón 3 (INACTIVA): `#9CA3AF`
- Escalón 4 (ACTIVA, arriba): `#10B981`

**Tipografía:** "ClubCore" en sans-serif limpia (Manrope, IBM Plex Sans).

**Vibe:** Crecimiento, salud del negocio. "Maximiza membresías activas, minimiza estados inactivos."

**Por qué encaja:** El gráfico representa el objetivo de negocio (crecer en membresías activas) mientras valida el motor de estados de fondo. Versátil para landing page + docs.

---

