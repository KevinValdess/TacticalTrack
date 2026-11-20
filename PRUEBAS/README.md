# Pruebas

## Automatizadas

- API: `mvn test` desde `API/`.
- Android: `./gradlew testDebugUnitTest` desde `APP/`.
- Cobertura Android: `./gradlew logicCoverageReport verifyLogicCoverage` desde `APP/`.

Las pruebas se encuentran en `API/src/test/` y `APP/app/src/test/`. GitHub Actions publica los reportes de cada ejecución.

## Postman

Importar `TacticalTrack.postman_collection.json`. La variable `baseUrl` comienza en `http://localhost:8080`. Crear una visita guarda su `visitId` para las siguientes peticiones.

Para registrar un dispositivo, completar `fcmToken` con un token real. El CRUD puede probarse sin ese paso.

## Comprobación HTTP

Con la API en ejecución, desde la raíz del repositorio:

```bash
python3 PRUEBAS/smoke_backend.py
```

El script crea una visita de prueba, comprueba el CRUD, la idempotencia y los errores 400/404, y elimina su propio registro al terminar.

Para otra dirección:

```bash
python3 PRUEBAS/smoke_backend.py https://servidor.example
```

Las pruebas de push entre dos dispositivos están descritas en [Configuración de Firebase](../DOCUMENTACION/FIREBASE_CONFIGURACION.md).
