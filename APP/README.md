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

El acceso usa cuentas ficticias con rol fijo:

| Correo | Contraseña de prueba | Nombre | Rol |
|---|---|---|---|
| `kevin@tacticaltrack.example` | `Tactical123` | Kevin | Supervisor |
| `jona@tacticaltrack.example` | `Tactical123` | Jona | Operario |
| `victor@tacticaltrack.example` | `Tactical123` | Víctor | Operario |

No se envían correos ni se requiere crear estas cuentas en Firebase. La validación es local para la presentación; no hay autenticación ni autorización de usuarios en el servidor. Mantener la API en la red privada de Tailscale para esta prueba. Estas contraseñas son públicas y no deben reutilizarse en cuentas reales.

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
