# Contrato HTTP

Base local: `http://localhost:8080`. Android Emulator usa `http://10.0.2.2:8080/`.

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/api/points` | Puntos de venta y supervisor asignado |
| GET | `/api/assets` | Todos los equipos |
| GET | `/api/assets?pointId=PV-001` | Equipos de un punto |
| GET | `/api/visits` | Lista de visitas |
| GET | `/api/visits/{id}` | Visita por ID |
| POST | `/api/visits` | Crea la visita y encola el aviso; devuelve 201 |
| PUT | `/api/visits/{id}` | Cambia estado del equipo y observaciones |
| PATCH | `/api/visits/{id}/status` | Cambia el estado de supervisión |
| DELETE | `/api/visits/{id}` | Elimina visita, aviso y recibos; devuelve 204 |
| PUT | `/api/devices/{installationId}` | Registra o actualiza un dispositivo; devuelve 204 |
| DELETE | `/api/devices/{installationId}` | Desactiva el dispositivo; devuelve 204 |
| GET | `/api/notifications/{visitId}` | Consulta el procesamiento del aviso |

Los datos inválidos devuelven 400 y los registros inexistentes, 404.

## Crear una visita

Campos del cuerpo JSON:

`operatorName`, `operatorRole`, `pointId`, `pointName`, `assetId`, `assetCode`, `assetType`, `visitType`, `equipmentStatus`, `observations`, `latitude`, `longitude`, `locationAccuracyMeters`, `distanceToPointMeters`, `presenceVerified`, `timestamp`, `supervisorStatus`, `evidenceBase64` y `clientRequestId`.

El servidor asigna el ID, toma los nombres del catálogo y asigna el supervisor del punto de venta. El equipo debe pertenecer a ese punto. La visita comienza como `Pendiente` y la presencia se calcula con la distancia recibida y un radio de 150 metros.

`timestamp` usa milisegundos desde Unix epoch. `locationAccuracyMeters` admite null. La fotografía se envía como Base64, con un máximo de 3.000.000 caracteres. No se utiliza el URI local del teléfono como archivo compartido.

`clientRequestId` identifica un envío. Repetir un POST con la misma clave devuelve la visita existente y no agrega otro aviso.

## Editar y revisar

PUT `/api/visits/{id}`:

```json
{"equipmentStatus":"Con falla","observations":"Motor detenido"}
```

PATCH `/api/visits/{id}/status`:

```json
{"status":"Aprobada"}
```

Estados de equipo: `Operativo`, `Operativo con observaciones` y `Con falla`. Para los últimos dos, se necesitan al menos cinco caracteres de observación. Máximo de observaciones: 2000 caracteres.

Estados de supervisión: `Pendiente`, `Aprobada` y `Rechazada`.

## Registrar un dispositivo

PUT `/api/devices/{installationId}`:

```json
{"supervisorId":"SUP-001","fcmToken":"TOKEN_DEL_DISPOSITIVO"}
```

El ID de instalación es un UUID generado por Android. Cuando cambia el token, se actualiza la misma instalación. Los endpoints de consulta no devuelven tokens.

## Estado del aviso

La consulta devuelve `visitId`, `state`, `attempts` y `lastError`.

| Estado | Significado |
|---|---|
| `PENDING` | Espera el próximo intento o un dispositivo activo |
| `PROCESSED` | Se procesaron los destinos de ese intento |
| `FAILED` | Se agotaron los ocho intentos |
| `EXPIRED` | Pasaron más de 24 horas |

Un destino procesado puede haber sido aceptado por FCM o desactivado por token inválido. Ninguno de estos estados confirma lectura por parte del supervisor.

## Clima

La app consulta Open-Meteo mediante un cliente Retrofit independiente:

```text
GET https://api.open-meteo.com/v1/forecast?latitude=-33.5186&longitude=-70.7159&current=temperature_2m,weather_code&timezone=auto
```

Se muestran la temperatura, el código WMO y la hora del dato. Es información actual de referencia y no modifica los registros de visita.
