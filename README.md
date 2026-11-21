# TacticalTrack

Aplicación Android para registrar visitas en terreno y revisar el estado de los equipos de cada punto de venta. Cada visita incluye foto, ubicación, observaciones y revisión del supervisor.

## Qué incluye

- Catálogo de puntos de venta y equipos.
- Registro, consulta, edición y eliminación de visitas.
- Evidencia fotográfica y ubicación GPS.
- Aprobación o rechazo por parte del supervisor.
- Avisos push cuando se registra una nueva visita.
- Consulta del clima en el lugar de la visita.

## Estructura

| Carpeta | Contenido |
|---|---|
| `APP/` | Proyecto Android con Kotlin, Jetpack Compose, Retrofit y Room |
| `API/` | Servicio Spring Boot, persistencia con H2 y envío de notificaciones |
| `DOCUMENTACION/` | Contrato HTTP y configuración de Firebase |
| `PRUEBAS/` | Colección de Postman y prueba HTTP manual |

## Ejecutar la API

Requiere JDK 17 y Maven. Desde la raíz del repositorio:

```bash
cd API
mvn spring-boot:run
```

La API escucha en `http://localhost:8080`. H2 se inicia con el servicio y guarda los datos en `API/data/`. Ese directorio debe conservarse al reiniciar.

Para comprobar la conexión, abrir `http://localhost:8080/api/points`.

## Ejecutar la app

Abrir `APP/` en Android Studio, seleccionar JDK 17 para Gradle e instalar Android SDK 35 y Build Tools 35.0.0.

Desde `APP/`:

```bash
./gradlew assembleDebug
```

En Windows usar `gradlew.bat`. La configuración predeterminada conecta con la API desde un emulador Android en `http://10.0.2.2:8080/`.

Para un teléfono por USB, activar la depuración y ejecutar:

```bash
adb reverse tcp:8080 tcp:8080
./gradlew assembleDebug -PbackendUrl=http://127.0.0.1:8080/
```

La API debe permanecer en ejecución mientras se usa la app. Abrir el APK no inicia el servidor. Las visitas ya descargadas pueden consultarse sin conexión; guardar o modificar datos requiere acceso a la API.

## Notificaciones

El supervisor puede activar los avisos desde Inicio. La API guarda un aviso junto con la visita y reintenta el envío si Firebase no responde. Al tocar la notificación, la app consulta el detalle de la visita.

Para usar push hay que configurar un proyecto Firebase. El archivo de cliente va en `APP/app/google-services.json`; la credencial de servidor se configura únicamente en la API. Los pasos están en [Configuración de Firebase](DOCUMENTACION/FIREBASE_CONFIGURACION.md).

Sin esa configuración, el catálogo y el CRUD siguen funcionando, pero no se envían notificaciones push.

## Pruebas

API, desde `API/`:

```bash
mvn test
```

Android, desde `APP/`:

```bash
./gradlew testDebugUnitTest logicCoverageReport verifyLogicCoverage
```

El control de cobertura exige al menos 80% de líneas en ViewModels, repositorios y reglas. El reporte queda en `APP/app/build/reports/jacoco/logicCoverageReport/html/index.html`.

GitHub Actions ejecuta las pruebas de ambos proyectos y compila un APK de depuración. Los reportes y el APK se publican como artefactos de cada ejecución.

También se puede importar la [colección Postman](PRUEBAS/TacticalTrack.postman_collection.json) o seguir la [guía de pruebas](PRUEBAS/README.md).

## Datos de prueba y alcance

El catálogo inicial contiene tres puntos de venta y cinco equipos. El supervisor `SUP-001` está asignado a esos puntos. El rol se asigna al iniciar sesión con una de las [cuentas de prueba](APP/README.md#acceso-y-visitas). La validación es local; todavía no hay autenticación ni autorización de usuarios en el servidor. Para la presentación, mantener la API dentro de la red privada de Tailscale.

La verificación GPS usa un radio de 150 metros y la distancia informada por el dispositivo. El clima mostrado corresponde al momento de la consulta, no a la fecha histórica de la visita.

Para generar un APK release firmado, seguir [la guía de Android](APP/README.md). La conexión push debe probarse con el proyecto Firebase y los dispositivos del equipo antes de la entrega.

Desarrollado para DSY1105, Duoc UC. Continúa el trabajo de [EA-2-TacticalTrack](https://github.com/JonaSlack/EA-2-TacticalTrack).
