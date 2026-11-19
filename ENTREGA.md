# Entrega EVA2: RiegoIoT-App

**Aplicaciones Móviles para IoT (TI3042, INACAP)** · Jenniffer Gonzalez y Rodrigo Cutiño · 2026-11-18

Profesor: este archivo resume el estado de la entrega frente a lo que usted pidió en la revisión ([issue #1](https://github.com/rodrigocutinoa/RiegoIoT-App/issues/1)). El detalle está en el [README](README.md).

## Lo que pidió en el issue #1

| Punto | Estado | Dónde verlo |
|---|:-:|---|
| Hito 3: permisos, conexión MQTT al puente de hertzbench, datos en vivo y comandos | ✔ | [README · Qué hace](README.md#qué-hace), pruebas P1, P2 y P4 |
| Notificación cuando la bomba se enciende o se apaga | ✔ | Pruebas P3 y P4, [captura](docs/capturas/demo-panel-riego-encendido.png) |
| Login con usuario y contraseña, roles operador y observador (Hito 5) | ✔ | [README · Autenticación](README.md#autenticación-y-almacenamiento), pruebas P8 a P11 |
| Modo seguro al cortarse el enlace (F5) | ✔ | Prueba P6 (`inject_fault`) |
| Historial de lecturas (Hito 6) | ✔ | Pruebas P12 y P13 |
| Seguridad ISO/IEC 27400 y OT, y pruebas (Hito 7) | ✔ | [README · Seguridad](README.md#seguridad-isoiec-27400-y-lineamientos-ot-hito-7) y [tabla P1–P14](README.md#pruebas): **14 de 14 pasan**, con captura cada una |
| Demostración (Hito 8) | ✔ | [Guion de 5 a 7 minutos](docs/DEMO.md), ensayado con dos emuladores (Panel y Nodo) |
| README con cómo correr la app contra hertzbench, con capturas | ✔ | [README · Cómo ejecutar](README.md#cómo-ejecutar-contra-hertzbench) |

**Sobre la pareja:** la EVA2 la hicimos en pareja, igual que la Unidad 1. Este repositorio es de los dos.

## Cómo revisarla rápido

1. Escenario: `ProyectRiego/hertzbench/riego-mqtt.yaml`, con un nodo simulado que publica mensajes cifrados. Para dos emuladores, `riego-broker.yaml`.
2. La app en el emulador: crear una cuenta (la primera es operador), elegir **Panel**, broker `10.0.2.2`, puerto `1883`, código de vinculación **`123456`**.
3. Para los dos "teléfonos" de la demo: `scripts/demo-emuladores.sh`.

## Además de lo pedido

- **Mensajes cifrados con AES-GCM** y un código de vinculación. El tópico va como dato autenticado, y los comandos de más de 30 s se rechazan (probado: un comando grabado y repetido no enciende la bomba).
- **Cada comando espera su confirmación (`ACK`)**: se reenvía hasta 3 veces y, si no llega, la app avisa. El estado queda retenido en el broker.
- **Reportes en hertzbench:** [#114](https://github.com/Rixmerz/hertzbench/issues/114) (Android 17 necesita `ACCESS_LOCAL_NETWORK`), [#115](https://github.com/Rixmerz/hertzbench/issues/115) (*keep-alive*, *Last Will* y sesión duplicada en el broker MQTT) y [#116](https://github.com/Rixmerz/hertzbench/issues/116) (prueba con la app Kotlin en el emulador).

## Límites declarados

- **Claves:** el código de 6 dígitos con sal fija sirve para la demostración. En producción se usaría ECDH o el Android Keystore.
- **Equipos:** sin un teléfono Android propio, la app se probó en emuladores, con su autorización.
- **ESP32:** el ESP32 emulado de hertzbench no corre en macOS (issue #54); el lado IoT se simuló con un broker y un nodo *scripted*, o con la propia app en el rol Nodo.
