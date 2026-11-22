# App Android

Proyecto Kotlin con Jetpack Compose. Usa Retrofit para acceder a la API y Room para conservar la última copia de las visitas.

## Preparación

1. Abrir esta carpeta en Android Studio.
2. Seleccionar JDK 17 para Gradle.
3. Instalar SDK 35 y Build Tools 35.0.0.
4. Iniciar la API en otra terminal.

La URL predeterminada, `http://10.0.2.2:8080/`, permite acceder al computador desde el emulador Android.

Para cambiarla al compilar:

```bash
./gradlew assembleDebug -PbackendUrl=https://servidor.example/
```

La URL debe terminar en `/`. HTTP sin cifrar solo está permitido para `localhost`, `127.0.0.1` y `10.0.2.2`.

Para probar un teléfono por USB:

```bash
adb reverse tcp:8080 tcp:8080
./gradlew assembleDebug -PbackendUrl=http://127.0.0.1:8080/
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. En Windows usar `gradlew.bat`.

## Acceso y visitas

El acceso se valida en la API mediante `POST /api/auth/login`. Los nombres y roles provienen de `APP_USERS`; la app no contiene una lista de cuentas ni contraseñas. Los usuarios de evaluación ya creados en el servidor son Kevin (`ad@duoc.cl`), Victor (`op@duoc.cl`) y Jonathan (`nuevo@duoc.cl`).

La sesión se guarda cifrada con una clave de Android Keystore. Al abrir la app se comprueba mediante `GET /api/auth/me`, y se vuelve a consultar al regresar a primer plano. Los cambios de nombre se reflejan desde la API. Un HTTP 401 elimina la sesión local y vuelve al acceso. Con un fallo de conexión al iniciar, se puede reintentar la validación sin borrar el token.

El botón **Cerrar sesión** desregistra los avisos del dispositivo e intenta revocar el token mediante `POST /api/auth/logout`; la sesión local se elimina incluso sin conexión. Las solicitudes al backend incluyen el token Bearer. Las consultas a Open-Meteo usan un cliente independiente, sin ese token.

Esta versión requiere los endpoints de usuarios y autenticación que se instalaron en el servidor. **El código de esa ampliación del backend todavía debe sincronizarse desde el servidor al repositorio**: la carpeta `API/` de este commit conserva la versión anterior. No reemplazar el JAR desplegado por una compilación de esa carpeta hasta completar dicha sincronización. La protección de visitas y dispositivos sigue pendiente en el servidor; los permisos de usuarios no implican que todos los endpoints estén protegidos.

Para actualizar y compilar desde PowerShell, en la raíz del repositorio:

```powershell
git pull --ff-only origin main
cd APP
.\gradlew.bat assembleRelease "-PbackendUrl=https://server.tailc8b8f3.ts.net:8443/"
```

Conservar `keystore.properties` y el archivo de firma originales. Instalar `app/build/outputs/apk/release/app-release.apk` como actualización de la app existente.

El operario registra el punto de venta, el equipo, el estado, las observaciones, la foto y la ubicación. El supervisor revisa las visitas y puede aprobarlas o rechazarlas.

El historial se conserva en `tacticaltrack_remote.db`. La app no importa automáticamente los registros de la base antigua `tacticaltrack.db`.

## Notificaciones

Añadir la configuración del proyecto en `app/google-services.json` y seguir [la guía de Firebase](../DOCUMENTACION/FIREBASE_CONFIGURACION.md).

En el teléfono del supervisor, entrar con ese rol y activar los avisos desde Inicio. Android 13 o superior solicita permiso para mostrarlos. WorkManager se encarga de registrar el dispositivo y reintentar errores de conexión.

## Pruebas

```bash
./gradlew testDebugUnitTest logicCoverageReport verifyLogicCoverage
```

El reporte se genera en `app/build/reports/jacoco/logicCoverageReport/html/index.html`. La medición incluye ViewModels, repositorios y reglas; excluye interfaz, DTO, factorías y adaptadores de plataforma.

## APK firmado

Crear una llave de evaluación desde esta carpeta:

```bash
mkdir -p signing
keytool -genkeypair -keystore signing/tacticaltrack-evaluacion.jks -storetype JKS -alias tacticaltrack-evaluacion -keyalg RSA -keysize 2048 -validity 3650
```

Copiar `keystore.properties.example` como `keystore.properties` y completar los valores locales. Luego:

```bash
./gradlew assembleRelease -PbackendUrl=http://10.0.2.2:8080/
```

Para un teléfono USB usar la URL `http://127.0.0.1:8080/` junto con `adb reverse`. El APK queda en `app/build/outputs/apk/release/app-release.apk`.

Verificar e instalar con las herramientas del SDK:

```bash
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/release/app-release.apk
```

La llave, sus contraseñas y la configuración local están excluidas de Git. Si ya hay una app instalada con otra firma, respaldar sus datos antes de desinstalarla.
