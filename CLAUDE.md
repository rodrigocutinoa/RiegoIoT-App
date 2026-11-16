# RiegoIoT-App – contexto para Claude Code

App Android (Kotlin + Jetpack Compose) de Rodrigo Cutiño para la EVA2 del curso Aplicaciones Móviles para IoT (TI3042, INACAP). La evaluación es individual: este repo es solo de Rodrigo.

- Repo común del proyecto de riego (EVA1, escenarios de hertzbench, bitácora): `../ProyectRiego`. **Antes de seguir, lee `../ProyectRiego/docs/RETOMAR.md` y `../ProyectRiego/docs/EVA2_AVANCE.md`.**
- La guía del profesor está en `../ProyectRiego/_Document/` (no se sube a ningún repo).

## Requisitos del profesor

1. Login (Hito 5: SQLite, PBKDF2 con 120 000 iteraciones y sal de 16 bytes, roles operador y observador, bloqueo de 30 s tras 5 fallos).
2. Notificación y estado cuando el riego se enciende o se apaga.
3. Lado IoT simulado con hertzbench por MCP (`.mcp.json`).

## Técnico

- Paquete `cl.inacap.riegoiot`, minSdk 26, targetSdk 37, Kotlin DSL, AGP 9.5.1.
- MQTT por el puente de hertzbench: desde el emulador `tcp://10.0.2.2:1883`, con `../ProyectRiego/hertzbench/riego-mqtt.yaml` corriendo.
- Tópicos: `riego/lectura` (`LECTURA;suelo;<n>`), `riego/estado` (`ON|OFF`), `riego/cmd` (`CMD;bomba;ON|OFF`).
- Compilar desde la terminal: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew installDebug`.
- Emulador: `Medium_Phone_API_37.0` (Android 17, arm64).

## Estado

| Hito | Estado |
|---|---|
| 0 | Proyecto creado y corriendo en el emulador (2026-11-16). Falta correrlo en un teléfono físico. |
| 1–8 | Pendientes |

## Convenciones

- Español en código, commits y documentos. Repo **público**.
- Commits por hito: "Hito N: ...".
- Al cerrar cada sesión, agregar una fila a la bitácora de `../ProyectRiego/docs/EVA2_AVANCE.md`.
