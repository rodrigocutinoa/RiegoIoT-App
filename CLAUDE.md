# RiegoIoT-App – contexto para Claude Code

App Android (Kotlin + Jetpack Compose) de Jenniffer Gonzalez y Rodrigo Cutiño para la EVA2 del curso Aplicaciones Móviles para IoT (TI3042, INACAP). **La EVA2 es en pareja** (confirmado por Rodrigo el 2026-11-17, aunque la guía dice individual); este es el repo de los dos.

- Repo común del proyecto de riego (EVA1, escenarios de hertzbench, bitácora): `../ProyectRiego`. **Antes de seguir, lee `../ProyectRiego/docs/RETOMAR.md` y `../ProyectRiego/docs/EVA2_AVANCE.md`.**
- La guía del profesor está en `../ProyectRiego/_Document/` (no se sube a ningún repo).

## Requisitos del profesor

1. Login (Hito 5: SQLite, PBKDF2 con 120 000 iteraciones y sal de 16 bytes, roles operador y observador, bloqueo de 30 s tras 5 fallos).
2. Notificación y estado cuando el riego se enciende o se apaga.
3. Lado IoT simulado con hertzbench por MCP (`.mcp.json`).

## Técnico

- Paquete `cl.inacap.riegoiot`, minSdk 26, targetSdk 37, Kotlin DSL, AGP 9.5.1.
- MQTT por el puente de hertzbench: desde el emulador `tcp://10.0.2.2:1883`, con `../ProyectRiego/hertzbench/riego-mqtt.yaml` corriendo.
- Tópicos y mensajes: ver «Decisiones de diseño» y `docs/HITOS_1_2.md`, sección 2.2.
- Compilar desde la terminal: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug`.
- Emulador: `Medium_Phone_API_37.0` (Android 17, arm64).
- **Android 17 exige `ACCESS_LOCAL_NETWORK`** para conectarse a IPs privadas como `10.0.2.2`; sin él, la conexión expira (SocketTimeoutException) aunque `adb shell nc` funcione.
- Escenarios: `../ProyectRiego/hertzbench/riego-mqtt.yaml` (nodo simulado, para probar el Panel) y `riego-broker.yaml` (solo broker + monitor suscrito a `riego/#`, para probar la app como Nodo o dos emuladores).
- Pruebas con dos emuladores a la vez dejan al Mac sin memoria (`-read-only`, se cuelga `adb`); se probó un rol a la vez y el otro lado desde el MCP (`mqtt_publish`, `get_serial` del monitor).
- No usar `uiautomator dump` (se cuelga); tocar por coordenadas a partir de capturas.

## Estado

| Hito | Estado |
|---|---|
| 0 | **Terminado** (2026-11-16): proyecto creado, corre en el emulador y está en GitHub. No hay teléfono Android (solo iPhone); el docente autorizó usar el emulador. |
| 1 y 2 | Borrador en [docs/HITOS_1_2.md](docs/HITOS_1_2.md) (2026-11-16). **Falta la revisión del docente** antes de programar. |
| 3 | **Funciona** (2026-11-17): MQTT con Paho, roles Nodo y Panel, comandos con ACK, notificaciones de encendido y apagado, estado seguro al cortarse el enlace. Probado contra hertzbench en el emulador (ver bitácora). Punto 4 completo (2026-11-17): confirmación con reenvío (5 s, hasta 3 reenvíos, aviso «Sin respuesta del nodo») y estado retenido en `riego/estado/<clave>`. |
| 5 | **Funciona** (2026-11-17): login con SQLite y PBKDF2-SHA256 (120 000 iteraciones, sal de 16 bytes), primer usuario operador y los siguientes observadores, bloqueo de 30 s tras 5 fallos (sobrevive a la rotación), el observador no puede enviar comandos (se controla en la pantalla y en `Sesion`). Probado en el emulador; PBKDF2 tarda 10–15 s en el emulador cargado. |
| 6 | **Funciona** (2026-11-17): el panel guarda cada lectura y cada cambio de la bomba en SQLite; historial con filtros «Todo» y «Solo bomba». Verificado que persiste con la app cerrada y que la contraseña no aparece en el archivo de la base. |
| 4, 7, 8 | Pendientes (pruebas P1–P14, seguridad ISO/IEC 27400 y OT, demostración) y README con capturas |

## Pendientes que reportó el profesor (issue #1, 2026-11-17), en orden

1. Hito 3: permisos (`INTERNET`, `POST_NOTIFICATIONS`), conexión MQTT al puente de hertzbench, humedad, temperatura, bomba y modo en vivo, y comandos.
2. Notificación cuando la bomba se enciende o se apaga (requisito mínimo).
3. Login con usuario y contraseña, roles operador y observador (Hito 5, requisito mínimo).
4. Modo seguro al cortarse el enlace (F5).
5. Historial de lecturas (Hito 6).
6. Seguridad y pruebas (Hito 7) y demostración (Hito 8).
7. README con cómo correr la app contra hertzbench, con capturas.

Además: la entrega del AAI quedó en borrador (Rodrigo debe presionar «Enviar tarea»); aclarar con el profesor que es en pareja y si espera un ESP32 emulado (en macOS no corre, issue #54 de hertzbench). Cuando el Hito 3 funcione: subir commits, enviar la tarea y mandar el reporte con la app que pidió en el #110.

## Decisiones de diseño (2026-11-16)

- Cliente MQTT: **Eclipse Paho** (`org.eclipse.paho.client.mqttv3`), justificado en el Hito 1.
- **Una sola app con dos roles** (Nodo y Panel), como la guía; se prueba con dos emuladores. hertzbench simula el broker, el canal y las fallas.
- Mensajes `TIPO;clave;valor` por tópico: `riego/lectura`, `riego/estado/<clave>` (con *retain*, un tópico por clave porque el broker retiene uno por tópico), `riego/cmd`, `riego/ack`; *Last Will* `ESTADO;enlace;CAIDO` en `riego/estado/enlace`. Tabla completa en el Hito 2.
- Ningún comando se pierde en silencio: el panel espera el `ACK` (reenvía a los 5 s, hasta 3 veces) y el nodo pasa solo a `SEGURO` al perder el enlace. Sale de la prueba con `inject_fault` (QoS 0 pierde el comando enviado durante un corte).

## Convenciones

- Español en código, commits y documentos. Repo **público**.
- Commits por hito: "Hito N: ...".
- Al cerrar cada sesión, agregar una fila a la bitácora de `../ProyectRiego/docs/EVA2_AVANCE.md`.
