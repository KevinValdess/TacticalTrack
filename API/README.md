# API de TacticalTrack

Servicio Spring Boot para el catálogo de equipos, las visitas y los avisos al supervisor. Requiere JDK 17 y Maven.

## Arranque

Desde esta carpeta:

```bash
mvn test
mvn spring-boot:run
```

La dirección local es `http://localhost:8080`. H2 funciona dentro del servicio y conserva los datos en `data/tacticaltrack.mv.db`.

| Variable | Valor predeterminado | Uso |
|---|---|---|
| `PORT` | `8080` | Puerto HTTP |
| `DB_URL` | `jdbc:h2:file:./data/tacticaltrack` | Dirección de la base de datos |
| `DB_USER` | `sa` | Usuario de H2 |
| `DB_PASSWORD` | Vacía | Contraseña de H2 |
| `PUSH_ENABLED` | `false` | Activa el envío a Firebase |
| `FIREBASE_PROJECT_ID` | Vacío | Proyecto de Firebase |
| `GOOGLE_APPLICATION_CREDENTIALS` | Sin definir | Ruta de la credencial privada del servidor |
| `PUSH_POLL_MS` | `15000` | Intervalo de revisión de avisos |

## Recursos

| Recurso | Ruta |
|---|---|
| Puntos de venta | `/api/points` |
| Equipos | `/api/assets` |
| Visitas | `/api/visits` |
| Dispositivos de supervisores | `/api/devices/{installationId}` |
| Estado de un aviso | `/api/notifications/{visitId}` |

Los métodos y cuerpos de cada petición están en [Contrato HTTP](../DOCUMENTACION/CONTRATO_API.md). La carpeta [PRUEBAS](../PRUEBAS/README.md) contiene una colección Postman y un script de comprobación.

## Avisos

La visita y su aviso se guardan en la misma transacción. El envío a Firebase se procesa aparte, con espera creciente y hasta ocho intentos. Los avisos caducan después de 24 horas. Si FCM identifica un token como no registrado, se desactiva ese destino.

Los recibos se guardan por visita e instalación para reducir reenvíos. El estado `PROCESSED` indica que los destinos fueron procesados; no confirma que el supervisor haya leído la notificación.

Para activar el envío, seguir [Configuración de Firebase](../DOCUMENTACION/FIREBASE_CONFIGURACION.md). La cola está pensada para una instancia del servicio.

Los endpoints todavía no exigen una sesión autenticada. Usar datos de prueba hasta incorporar autenticación y permisos por usuario.
