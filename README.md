# API de Gestión de Pólizas

[![CI](https://github.com/emersaiprueba-afk/polizas-api/actions/workflows/ci.yml/badge.svg)](https://github.com/emersaiprueba-afk/polizas-api/actions/workflows/ci.yml)

API REST en Spring Boot para gestionar pólizas de arrendamiento (individuales y colectivas) y sus riesgos, con integración simulada al CORE de seguros.

## Requisitos
- Java 21
- Maven 3.9+

## Cómo ejecutar
```bash
mvn spring-boot:run
```
La API queda en `http://localhost:8080`. Al arrancar se cargan 2 pólizas de ejemplo:
- Póliza 1: INDIVIDUAL, 1 riesgo
- Póliza 2: COLECTIVA, 2 riesgos

## Cómo ejecutar las pruebas
```bash
mvn test
```
Incluye pruebas unitarias de las reglas (sin Spring), pruebas del servicio con mocks y pruebas de integración de los endpoints con MockMvc. Además, el pipeline de GitHub Actions arranca la API y la prueba con llamadas reales.

## Seguridad
Todas las peticiones exigen el header `x-api-key: 123456`. Sin él, la respuesta es `401`.
La clave se puede cambiar con la variable de entorno `API_KEY`.

## Endpoints
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/polizas?tipo=&estado=` | Lista pólizas; filtros opcionales `tipo` (INDIVIDUAL, COLECTIVA) y `estado` (ACTIVA, RENOVADA, CANCELADA) |
| GET | `/polizas/{id}/riesgos` | Lista los riesgos de una póliza |
| POST | `/polizas/{id}/renovar` | Renueva: canon y prima + IPC, estado RENOVADA |
| POST | `/polizas/{id}/cancelar` | Cancela la póliza y todos sus riesgos |
| POST | `/polizas/{id}/riesgos` | Agrega un riesgo (solo pólizas COLECTIVAS) |
| POST | `/riesgos/{id}/cancelar` | Cancela un riesgo |
| POST | `/core-mock/evento` | Mock del CORE: registra el evento en el log |

### Ejemplos
```bash
curl -H "x-api-key: 123456" "http://localhost:8080/polizas?tipo=COLECTIVA&estado=ACTIVA"

curl -X POST -H "x-api-key: 123456" http://localhost:8080/polizas/1/renovar

curl -X POST -H "x-api-key: 123456" -H "Content-Type: application/json" \
  -d '{"direccionInmueble":"Cl 50 # 10-20","arrendatario":"Juan Pérez","arrendador":"Sofía Ríos"}' \
  http://localhost:8080/polizas/2/riesgos
```

## Reglas de negocio implementadas
Todas viven en la entidad `Poliza` (raíz del agregado); el servicio solo orquesta.
- Una póliza individual nace con su único riesgo y no admite más. Solo las colectivas admiten riesgos nuevos.
- No se puede renovar una póliza cancelada.
- La renovación es idempotente: si la nueva vigencia aún no ha empezado, un segundo intento (doble clic) responde 422 y no aplica el IPC dos veces.
- Cancelar una póliza cancela todos sus riesgos.
- No se puede cancelar el único riesgo de una póliza individual (quedaría activa sin nada asegurado): se cancela la póliza.
- Prima = canon mensual × meses de vigencia. La renovación ajusta el canon por IPC (configurable con la variable `IPC`) y recalcula la prima por el mismo periodo.

## Respuestas de error
Todas en formato estándar Problem Details (RFC 9457):
- `400` datos inválidos, JSON mal formado o parámetro con valor no permitido
- `401` falta o es inválida la API key
- `404` póliza o riesgo inexistente
- `409` la póliza fue modificada al mismo tiempo por otro usuario (concurrencia optimista)
- `422` regla de negocio violada
- `500` error inesperado (sin exponer detalles internos)

## Decisiones de diseño
- **Capas**: `controller` (HTTP) → `service` (casos de uso) → `repository` (datos). Las reglas de negocio viven en la entidad `Poliza` (raíz del agregado) para que no se puedan saltar.
- **Puerto y adaptador para el CORE**: el servicio depende de la interfaz `CoreGateway`; `HttpCoreGateway` es el adaptador, con timeouts configurables. Pasar del mock al servicio real en WebLogic solo requiere configuración (`CORE_URL`) o un nuevo adaptador.
- **Aviso al CORE después del commit**: el servicio publica un evento y `NotificadorCore` lo envía con `@TransactionalEventListener(AFTER_COMMIT)`. Si la transacción falla, el CORE nunca recibe un cambio que no ocurrió.
- **DTOs** (`record`): la API no expone las entidades JPA.
- **Concurrencia optimista** con `@Version` para evitar cambios perdidos.
- **H2 en memoria** para la prueba; en producción iría Oracle con migraciones (Flyway).

## Mejoras para producción
- Patrón outbox + reintentos para garantizar la entrega al CORE.
- Circuit breaker (Resilience4j) hacia el CORE.
- Autenticación OAuth2/JWT en lugar de API key estática.
- Documentación OpenAPI/Swagger y pruebas de integración de los endpoints.
