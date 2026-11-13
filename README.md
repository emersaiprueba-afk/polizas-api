# API de Gestión de Pólizas

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
- Una póliza individual solo puede tener 1 riesgo activo.
- Solo se agregan riesgos por API a pólizas colectivas.
- No se puede renovar una póliza cancelada.
- Cancelar una póliza cancela todos sus riesgos.
- Prima = canon mensual × meses de vigencia. La renovación ajusta el canon por IPC (configurable con la variable `IPC`) y recalcula la prima por el mismo periodo.

## Respuestas de error
Formato estándar Problem Details (RFC 9457):
- `400` datos inválidos
- `401` falta o es inválida la API key
- `404` póliza o riesgo inexistente
- `422` regla de negocio violada

## Decisiones de diseño
- **Capas**: `controller` (HTTP) → `service` (casos de uso) → `repository` (datos). Las reglas de negocio viven en la entidad `Poliza` (raíz del agregado) para que no se puedan saltar.
- **Puerto y adaptador para el CORE**: el servicio depende de la interfaz `CoreGateway`; `HttpCoreGateway` es el adaptador. Pasar del mock al servicio real en WebLogic solo requiere configuración (`CORE_URL`) o un nuevo adaptador.
- **DTOs** (`record`): la API no expone las entidades JPA.
- **Concurrencia optimista** con `@Version` para evitar cambios perdidos.
- **H2 en memoria** para la prueba; en producción iría Oracle con migraciones (Flyway).

## Mejoras para producción
- Patrón outbox + reintentos para garantizar la entrega al CORE.
- Circuit breaker (Resilience4j) hacia el CORE.
- Autenticación OAuth2/JWT en lugar de API key estática.
- Documentación OpenAPI/Swagger y pruebas de integración de los endpoints.
