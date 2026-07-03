# Mindray Receiver — App Android nativa (tablet como carpeta compartida)

Esta es la version **nativa Android (Kotlin)** del proyecto. A diferencia de
la version de escritorio (Python), esta app corre directamente en el
tablet Android y actua ella misma como el servidor de la carpeta
compartida SMB a la que el ecografo Mindray V6 le va a escribir las
imagenes.

---

## 1. Arquitectura (resumen)

```
Mindray V6 --(SMB, puerto 445, misma red Wi-Fi local)-->
    Router/AP local
        --> Tablet Android (rooteado)
              1. Regla iptables (root): puerto 445 -> puerto interno 8445
              2. JFileServer (SMB embebido) escuchando en 8445,
                 compartiendo la carpeta interna de la app
              3. FileObserver detecta archivo nuevo y estable
              4. Se agrupa en un "Exam" -> se guarda en Room (SQLite)
              5. Notificacion + aparece en la lista principal
              6. Botones: Ver, Drive, Telegram, Email, Informe PDF, Analisis
```

Por que hace falta rootear: Android no permite que una app comun escuchi
en el puerto 445 (los puertos <1024 requieren privilegios de root a nivel
de kernel Linux). Con root, agregamos **una sola regla de red** (no la app
entera corriendo como root) que redirige el trafico entrante del puerto
445 hacia el puerto 8445, donde si puede escuchar la app sin privilegios
especiales. Esta es la misma tecnica que usan apps de SMB-server rooteadas
ya publicadas (ej. SimbaDroid).

**Limitacion importante:** el servidor SMB embebido (`org.filesys:jfileserver`,
version open source) solo soporta **SMB1/CIFS** (protocolo antiguo, no
SMB2/3). Como el Mindray V6 es un equipo de ecografia con un cliente SMB
embebido tipicamente antiguo, esto normalmente **no es un problema** — es
justamente el protocolo que estos equipos suelen hablar. Si al probarlo
el V6 no logra conectar, ver la seccion 6 (Troubleshooting).

---

## 2. Estructura del proyecto

```
mindray-tablet-app/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/smb_config_template.xml     # Config JFileServer (plantilla)
│       ├── java/com/sonocare/mindrayreceiver/
│       │   ├── MainActivity.kt                # Lista de examenes + acciones
│       │   ├── MindrayApp.kt                  # Application, libsu, canales de notif.
│       │   ├── bridge/
│       │   │   ├── RootUtils.kt               # Redireccion de puerto via iptables
│       │   │   ├── SmbBridgeService.kt        # Servicio foreground: SMB + watcher
│       │   │   └── BootReceiver.kt            # Autoarranque al encender el tablet
│       │   ├── data/                          # Room: Exam, ExamDao, AppDatabase
│       │   ├── services/                      # Drive, Telegram, Email, PDF, Analisis
│       │   ├── notifications/NotificationHelper.kt
│       │   └── ui/                            # Settings, ImageViewer, Adapter
│       └── res/                                # Layouts, menus, estilos
├── build.gradle.kts
└── settings.gradle.kts
```

---

## 3. Como generar la carpeta compartida (lo que preguntaste)

**La carpeta no se "crea" manualmente vos — la crea la app sola.** Los
pasos reales son:

### Paso 1 — Rootear el tablet
El procedimiento exacto depende de la marca/modelo del tablet (cada
fabricante tiene su metodo de desbloqueo de bootloader). En general:
1. Activar "Opciones de desarrollador" (tocar 7 veces el numero de
   compilacion en Ajustes → Acerca del tablet).
2. Activar "Desbloqueo de OEM" y "Depuracion USB".
3. Desbloquear el bootloader (`fastboot flashing unlock`, especifico por
   fabricante — buscar la guia exacta para el modelo puntual del tablet).
4. Instalar **Magisk** (https://github.com/topjohnwu/Magisk) parcheando la
   imagen de boot y flasheandola.
5. Verificar que la app Magisk detecte el root correctamente.

Si me decis la marca/modelo exacto del tablet que vas a usar, te doy la
guia puntual de rooteo para ese equipo — varia bastante entre fabricantes.

### Paso 2 — Instalar y abrir la app Mindray Receiver
Al abrirla por primera vez:
1. Magisk va a mostrar un popup pidiendo **autorizar acceso root** a la
   app — hay que aceptarlo (una sola vez).
2. La app crea automaticamente la carpeta interna
   `Android/data/com.sonocare.mindrayreceiver/files/incoming/` — **esa es
   la carpeta compartida real**, no hace falta crearla vos a mano.
3. Aplica la regla de red (root) y arranca el servidor SMB embebido.
4. La pantalla principal muestra: `● Bridge SMB activo — carpeta: MindrayShare`

### Paso 3 — Anotar la IP del tablet
1. Andá a **Configuracion** (icono arriba a la derecha) dentro de la app.
2. Vas a ver un campo: **"IP del tablet (usar en el V6): 192.168.x.x"**
3. Ahi mismo podes cambiar (si queres) el **nombre de carpeta**, **usuario**
   y **contrasena** que va a pedir la carpeta compartida (por defecto:
   carpeta `MindrayShare`, usuario `mindray`, contrasena `mindray123` —
   cambialos por unos propios y anotalos).

### Paso 4 — Cargar esos datos en el Mindray V6
En la pantalla que ya nos mostraste (**Config → Preajuste red → Almacenamiento
de red**), en el bloque "Configur. servicio":

| Campo en el V6      | Que poner                                          |
|----------------------|-----------------------------------------------------|
| Nom servic.          | Cualquier nombre, ej. `TabletConsultorio`           |
| Dispositi. destino   | Si hay dropdown, elegir el tipo generico/Windows/PC |
| Direccion IP         | La IP que te mostro la app (Paso 3)                 |
| Nomb usua            | El usuario configurado en la app (ej. `mindray`)    |
| Contras              | La contrasena configurada en la app                 |
| Dir. comp.           | El nombre de carpeta configurado (ej. `MindrayShare`) |

Despues tocar **Añadir**, y despues **Actual** (para que quede activo el
servicio). El nombre va a aparecer en "Lista servidor". Desde ahi el V6 ya
deberia poder guardar/enviar las imagenes directo a esa carpeta.

### Paso 5 — Probar
Hacer una captura de prueba en el V6 y confirmar que la imagen aparece
en la lista principal de la app (con notificacion y sonido).

---

## 4. Compilacion del proyecto

1. Abrir la carpeta `mindray-tablet-app/` con **Android Studio** (version
   Koala 2024.1 o mas nueva recomendado).
2. Dejar que Android Studio sincronice Gradle (va a generar el wrapper
   automaticamente si no esta presente).
3. **Antes de compilar**, revisar `bridge/SmbBridgeService.kt` — la
   integracion con la libreria `org.filesys:jfileserver` esta dejada como
   plantilla comentada (el flujo de alto nivel esta correcto: cargar XML →
   `ServerConfiguration` → `SMBServer` → `startServer()`), pero los nombres
   exactos de clase pueden variar segun la version publicada. Verificar
   contra los ejemplos oficiales antes de la primera build:
   - https://github.com/FileSysOrg/jfileserver
   - https://filesys.org/wiki/index.php/Configuring_JFileServer
4. Conectar el tablet por USB (con depuracion USB activada) y correr
   ("Run") desde Android Studio, o generar el APK firmado
   (`Build → Generate Signed Bundle/APK`) para instalarlo directo.

---

## 5. Configuracion de cada servicio

### Google Drive
1. En [Google Cloud Console](https://console.cloud.google.com/), crear un
   proyecto, habilitar **Google Drive API**.
2. Crear credenciales **OAuth 2.0 Client ID** de tipo **Android**:
   necesita el `applicationId` (`com.sonocare.mindrayreceiver`) y la huella
   **SHA-1** del certificado de firma del APK (`keytool -list -v -keystore tu.keystore`).
3. No hace falta descargar ningun JSON: el login se hace con el boton
   "Conectar cuenta de Google" en Configuracion (Google Sign-In nativo).

### Telegram
1. Hablar con `@BotFather` → `/newbot` → copiar el token.
2. Conseguir el `chat_id` (hablarle al bot y consultar
   `https://api.telegram.org/bot<token>/getUpdates`, o usar `@userinfobot`).
3. Cargar ambos datos en Configuracion.

### Email
Igual que la version de escritorio: usar contrasena de aplicacion si es
Gmail (no la contrasena normal de la cuenta).

---

## 6. Troubleshooting

- **El V6 no logra conectar / da error de autenticacion:** confirmar que
  tablet y V6 esten en la misma subred (mismo SSID/Wi-Fi local, no dos
  redes distintas del mismo router). Probar primero con una PC Windows
  (`net use \\<ip-tablet>\MindrayShare /user:mindray mindray123`) para
  aislar si el problema es el V6 o el servidor SMB en si.
- **"Se requiere acceso root" en la pantalla principal:** revisar que
  Magisk no tenga la app en la lista de denegados (Magisk → Configuracion
  → Lista de denegados) y volver a abrir la app.
- **El bridge se detiene si el tablet entra en reposo:** ir a Ajustes de
  Android → Bateria → Mindray Receiver → **"Sin restricciones"** (o
  equivalente segun el fabricante), para que el sistema no mate el
  servicio en segundo plano durante la consulta.
- **Se reinicio el tablet y la regla de puerto se perdio:** es esperado —
  `BootReceiver` vuelve a aplicarla automaticamente al encender, siempre
  que "Iniciar automaticamente" este activado en Configuracion.

---

## 7. Que quedo como placeholder / a revisar

- `bridge/SmbBridgeService.kt`: integracion exacta con las clases de
  `org.filesys` (marcado con comentarios `NOTA` en el codigo).
- `services/ImageAnalyzer.kt`: analisis basico de nitidez/contraste. Punto
  de extension para un modelo de IA real en el futuro.
- Iconos de la app (`mipmap/ic_launcher`): generar desde Android Studio
  con **Build → Image Asset**.
