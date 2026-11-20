# Configuración de Firebase

Se necesita un proyecto Firebase del equipo y un teléfono o emulador con Google Play services e internet.

## Android

1. Registrar una app Android en Firebase con el identificador `cl.duoc.tacticaltrack`.
2. Descargar su `google-services.json` y copiarlo a `APP/app/google-services.json`.
3. Sincronizar Gradle y volver a compilar la app.

El plugin de Google Services se aplica cuando existe ese archivo. Sin él, la app permite usar las visitas pero no activa push.

## Servidor

Habilitar Firebase Cloud Messaging API (HTTP v1) en el proyecto. Guardar la credencial de cuenta de servicio fuera del repositorio y configurar su ruta mediante `GOOGLE_APPLICATION_CREDENTIALS`. La cuenta necesita permiso para enviar mensajes FCM.

Desde la raíz del repositorio, Linux/macOS:

```bash
export GOOGLE_APPLICATION_CREDENTIALS="/ruta/privada/service-account.json"
export FIREBASE_PROJECT_ID="id-del-proyecto"
export PUSH_ENABLED=true
cd API
mvn spring-boot:run
```

PowerShell:

```powershell
$env:GOOGLE_APPLICATION_CREDENTIALS="C:\ruta\privada\service-account.json"
$env:FIREBASE_PROJECT_ID="id-del-proyecto"
$env:PUSH_ENABLED="true"
cd API
mvn spring-boot:run
```

La credencial privada se utiliza solo en el servidor. No va dentro del APK. El SDK Admin obtiene y renueva sus tokens de acceso.

Con `PUSH_ENABLED=false`, las visitas y sus avisos se guardan, pero no se envían mensajes a Firebase.

## Registrar el supervisor

1. Iniciar la API y comprobar que el teléfono puede conectarse a ella.
2. Entrar en la app como Supervisor.
3. En Inicio, pulsar Activar avisos y aceptar el permiso de Android si aparece.
4. Esperar la confirmación de registro del dispositivo.

Los puntos iniciales están asignados a `SUP-001`, la identidad usada por esta prueba. El registro de dispositivo no sustituye una autenticación de usuario.

## Probar el envío

1. Dejar el teléfono del supervisor en segundo plano.
2. Desde otro dispositivo, registrar una visita con foto y GPS.
3. Consultar `/api/notifications/{visitId}` para revisar su estado.
4. Tocar el aviso en el teléfono del supervisor. Si la app arranca desde cero, entrar con el rol Supervisor para abrir el detalle.
5. Aprobar o rechazar la visita y actualizarla desde el otro dispositivo.

Repetir con la app en primer plano, después de cerrarla desde recientes y con interrupciones de conexión. Forzar detención desde los ajustes del sistema puede impedir la recepción hasta abrirla otra vez.

Al desactivar avisos o cambiar a Operario, la app solicita dar de baja el dispositivo y borrar su token. La baja remota se completa cuando hay conexión; los mensajes que ya estén en tránsito pueden llegar con retraso.

## Reintentos

El backend conserva el aviso pendiente si FCM falla. Reintenta con espera creciente hasta ocho veces y descarta los avisos con más de 24 horas. Si todavía no hay dispositivos activos, espera sin gastar esos ocho intentos.

Un recibo por visita e instalación evita reenviar a destinos ya procesados. Si el proceso cae entre el envío y el commit, puede repetirse el aviso; se utiliza una etiqueta por visita para reemplazar la notificación visible.

La aceptación de FCM no garantiza que el teléfono haya mostrado el aviso. El supervisor siempre puede consultar las visitas desde el historial.

## Referencias

- [Firebase Admin SDK](https://firebase.google.com/docs/admin/setup)
- [Configurar Android](https://firebase.google.com/docs/cloud-messaging/android/get-started)
- [Recepción de mensajes](https://firebase.google.com/docs/cloud-messaging/android/receive-messages)
