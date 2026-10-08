# DOMAIN.md

**Última verificación:** 2026-10-08 (contrato leído de `/v3/api-docs` en la app corriendo; reglas leídas del código).

## Entidades (tablas MySQL)
`usuarios` (rol `ADMIN | ENTRENADOR | CLIENTE`) · `clientes` (1:1 con `usuarios`) · `planes` (`limite_reservas_mes`, NULL = ilimitado) · `membresias` (cliente + plan) · `pagos` (de una membresía; `referencia` identifica el evento del gateway) · `reservas` (de un cliente) · `accesos` (check-in: cliente + membresía) · `rutinas` (de un cliente).
Estados: `EstadoPago = PENDIENTE | PAGADO | FALLIDO`; `EstadoReserva = PENDIENTE | CONFIRMADA | CANCELADA` (**no existe `COMPLETADA`**).

## Estado de una membresía (regla central, FACT)
`MembresiaStateService.evaluarEstado` decide **siempre on-demand**, en este orden: suspendida → vencida (`INACTIVA`) → pago pendiente (`DEUDA`) → cupo mensual agotado (`INACTIVA`) → si no, `ACTIVA`. `estado_cacheado` existe solo como copia para listados, nunca como fuente de verdad. Una membresía no `ACTIVA` bloquea reservas y check-in.

## Roles y permisos (FACT: probado contra la API real)
| Recurso | ADMIN | ENTRENADOR | CLIENTE |
|---|---|---|---|
| clientes (listar/crear/editar/borrar) | ✔ | solo `GET /{id}` | ✘ (403) |
| planes | ✔ crear/editar | ver | ver |
| membresías, pagos | ✔ | solo `GET /membresias/{id}` | ✘ |
| reservas | listar, crear, cancelar | listar, ver | crear/cancelar **solo las propias** |
| accesos | ✔ | ✔ | ✘ |
| rutinas | ✔ | ✔ crear/borrar | solo leer las propias |

**Propiedad de objetos:** `AuthenticatedClienteResolver` liga el usuario autenticado a su cliente por email. Un CLIENTE que reserva a nombre de otro recibe **404** (no 403, para no revelar que el id existe). Verificado.

## Reglas de negocio
- **Cupo mensual:** `limite_reservas_mes` por plan; se cuenta sobre reservas `PENDIENTE`/`CONFIRMADA` (INFERENCIA, por las referencias en `MembresiaStateService`); se cierra con un lock por cliente (ver `ARCHITECTURE.md`: solo una JVM).
- **Cancelar una reserva es idempotente:** cancelar una ya cancelada devuelve 200 `CANCELADA`. No hay estado "completada" que proteger.
- **Borrar un cliente borra también su usuario** (probado: el login pasa a 401 y el email se puede reutilizar).
- **Pagos por webhook:** upsert por `referencia`; un evento posterior pisa el estado anterior (por eso el timestamp firmado: un evento viejo reenviado no debe poder revertirlo).
- **Job diario 08:00:** publica los vencimientos de mañana para notificar a n8n.
- **Credenciales demo (solo `dev`):** `admin@clubcore.com`, `entrenador@clubcore.com`, `cliente1@clubcore.com`, todos con `Admin1234!`.
