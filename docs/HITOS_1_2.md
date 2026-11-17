# RiegoIoT-App: definición y diseño (Hitos 1 y 2)

**Aplicaciones Móviles para IoT (TI3042, INACAP) · Unidad 2**
Estudiante: Rodrigo Cutiño · Proyecto de la Unidad 1: [ProyectRiego](https://github.com/rodrigocutinoa/ProyectRiego)

---

## Hito 1: definir el proyecto

### 1.1 Problemática

En un huerto o invernadero, regar con horario fijo desperdicia agua cuando el suelo ya está húmedo y deja secar las plantas cuando hace calor. En la Unidad 1 construimos un nodo con ESP32 que mide la humedad del suelo y enciende una bomba cuando baja de un umbral, comunicándose por Wi-Fi con MQTT. Falta que la persona a cargo pueda **ver el estado del riego desde su teléfono, enterarse en el momento en que la bomba se enciende o se apaga y controlarla a distancia**, sin que cualquiera pueda hacerlo. Esta app cubre esa necesidad.

### 1.2 Del proyecto de la Unidad 1 a la app

| Proyecto de la Unidad 1 | Qué simula el nodo | Qué hace el panel | Cuándo llega una notificación |
|---|---|---|---|
| Monitoreo de humedad del suelo con activación de riego | Sensor de humedad del suelo (deslizador), temperatura y bomba de riego | Ve la humedad, la temperatura y el estado de la bomba; enciende o apaga la bomba; cambia el modo y el umbral | Panel: **la bomba se encendió o se apagó**, y el enlace se cortó. Nodo: llegó un comando. |

El nodo de riego es un rol de la misma app (como en la guía) y también puede ser un nodo simulado en **hertzbench**, el simulador de red que pidió el docente. En la Unidad 4 ese nodo será el ESP32 real de la Unidad 1, que ya habla el mismo protocolo MQTT.

### 1.3 Funcionalidades

| ID | Funcionalidad | Quién la usa | Evaluación |
|---|---|---|:-:|
| F1 | El nodo mide la humedad del suelo y la temperatura, y las publica cada 5 segundos. En modo automático enciende la bomba bajo el umbral y la apaga al llegar a umbral + 5 % (histéresis, igual que en la Unidad 1). | Nodo | F S |
| F2 | El panel muestra en vivo la humedad, la temperatura, el estado de la bomba, el modo (AUTO, MANUAL o SEGURO) y el estado del enlace. | Panel | F S |
| F3 | El panel enciende o apaga la bomba y cambia el modo y el umbral; el nodo confirma cada comando. | Panel → Nodo | F S |
| F4 | **Notificación en el panel cada vez que la bomba se enciende o se apaga**, y en el nodo al recibir un comando. | Ambos | F S |
| F5 | Si el enlace se corta: aviso en ambos, el nodo apaga la bomba por sí mismo y pasa a modo SEGURO, sin esperar ningún comando. Al reconectar, el panel ve el estado real de inmediato. | Ambos | F S |
| F6 | Login con usuario y contraseña; roles **operador** (controla la bomba) y **observador** (solo mira). | Panel | S |
| F7 | Cada lectura y cada encendido o apagado de la bomba se guardan y se consultan en un historial. | Panel | S |

### 1.4 Herramientas justificadas

La aplicación se desarrolla en **Android Studio** con **Kotlin**, el lenguaje que Google recomienda para Android, y la interfaz con **Jetpack Compose y Material 3**, que siguen las guías de diseño de Android. Las tareas de red corren en **corrutinas** para no bloquear la interfaz, y las alertas usan **notificaciones con canal propio** (`NotificationCompat`), pidiendo el permiso `POST_NOTIFICATIONS` en tiempo de ejecución.

La comunicación usa **MQTT 3.1.1 sobre TCP**, el mismo protocolo del nodo ESP32 de la Unidad 1, con la librería **Eclipse Paho MQTT (`org.eclipse.paho.client.mqttv3`)**. Es el cliente MQTT de referencia, de código abierto, pequeño y sin dependencias, y maneja la reconexión y el *Last Will* que se usa para detectar la caída del nodo (F5).

El lado IoT se simula con **hertzbench**, conectado a Claude Code por MCP: un broker MQTT simulado y el canal Wi-Fi con distancia, muros y pérdidas. El emulador Android llega a él por `10.0.2.2:1883`. Con hertzbench se pueden **inyectar fallas** (pérdida de paquetes, corte del enlace) para probar el estado seguro.

**Sumativa:** usuarios, lecturas y eventos se guardan en **SQLite**, incluido en Android, porque la app debe funcionar sin Internet en el terreno y no necesita compartir datos entre equipos. Las contraseñas se protegen con **PBKDF2** y los mensajes con **AES-GCM**, ambos de `javax.crypto`, sin librerías extra.

**Equipos:** se prueba en el emulador de Android Studio (Android 17, arm64), autorizado por el docente porque no cuento con un teléfono Android.

---

## Hito 2: diseñar la solución

### 2.1 Medio inalámbrico: Wi-Fi con MQTT

| Pregunta | En este proyecto | Conviene |
|---|---|---|
| Distancia | El huerto o invernadero está a más de 10 m de donde se mira el panel, y a veces fuera de la casa | Wi-Fi |
| Consumo de energía | El nodo ya necesita una fuente para la bomba; no funciona a batería | Da lo mismo (no favorece a Bluetooth) |
| Estabilidad | Hay un router propio en el lugar. MQTT reconecta solo y avisa la caída con *Last Will* | Wi-Fi |
| Volumen de datos | Lecturas cortas cada 5 segundos | Cualquiera |

**Justificación:** se elige **Wi-Fi con MQTT** porque el nodo y el panel están a más de 10 m y en espacios distintos, el nodo tiene alimentación permanente (la bomba la necesita), y hay un router propio. MQTT desacopla a los equipos: ninguno necesita la IP del otro, ambos hablan con el broker por tópicos, y el *Last Will* avisa al panel cuando el nodo se desconecta sin aviso. Es el mismo medio del ESP32 de la Unidad 1, así que en la Unidad 4 la placa real se conecta sin cambiar la app.

Se descarta Bluetooth, aunque la guía lo recomienda para una misma sala: su alcance de unos 10 m no cubre el caso, y el puente Bluetooth de hertzbench solo funciona en Linux, así que no permitiría simular el nodo en macOS.

### 2.2 Mensajes

Cada mensaje es una línea de texto con tres partes separadas por punto y coma, como en la guía. Se agrupan por tópico MQTT:

| Tópico | Mensaje | Dirección | Significado |
|---|---|---|---|
| `riego/lectura` | `LECTURA;suelo;42` | Nodo → Panel | Humedad del suelo en % |
| `riego/lectura` | `LECTURA;temp;24.5` | Nodo → Panel | Temperatura del aire en °C (punto decimal) |
| `riego/estado` | `ESTADO;bomba;ON` · `ESTADO;bomba;OFF` | Nodo → Panel | La bomba cambió de estado: **genera la notificación del panel** |
| `riego/estado` | `ESTADO;modo;AUTO` · `MANUAL` · `SEGURO` | Nodo → Panel | Modo de operación del nodo |
| `riego/estado` | `ESTADO;enlace;CAIDO` | Broker → Panel | *Last Will* del nodo: se perdió el enlace |
| `riego/cmd` | `CMD;bomba;ON` · `CMD;bomba;OFF` | Panel → Nodo | Encender o apagar la bomba (pasa a modo MANUAL) |
| `riego/cmd` | `CMD;modo;AUTO` | Panel → Nodo | Volver al modo automático |
| `riego/cmd` | `CMD;umbral;35` | Panel → Nodo | Nuevo umbral de humedad, entre 10 y 90 % |
| `riego/ack` | `ACK;bomba;ON` | Nodo → Panel | Confirmación: la bomba quedó en ese estado |

Cualquier línea que no calce con este formato, o con un valor fuera de rango, se descarta. Eso también es una medida de seguridad.

**Reglas para que ningún comando se pierda en silencio** (salen de la prueba de la sección 2.4):

- **Confirmación obligatoria:** después de enviar un `CMD`, el panel muestra el botón como **pendiente** hasta recibir el `ACK`. Si no llega en 5 segundos, reenvía el comando hasta 3 veces y después avisa «Sin respuesta del nodo».
- **Estado retenido:** el nodo publica `riego/estado` con *retain*. El broker guarda el último estado y se lo entrega al panel apenas se conecta o se reconecta, así que el panel nunca muestra un estado viejo.
- **Estado seguro en el nodo:** si el nodo pierde el enlace, apaga la bomba y pasa a `SEGURO` por sí mismo. Al reconectar publica `ESTADO;modo;SEGURO` y `ESTADO;bomba;OFF`, y solo sale de `SEGURO` con un comando nuevo del operador.

### 2.3 Mockup de pantallas

```
 Login (S)            Conexión               Panel                       Historial (S)
┌──────────────┐    ┌──────────────────┐    ┌───────────────────────┐    ┌──────────────────────┐
│  RiegoIoT    │    │ 1. Rol           │    │ Enlace: ● Conectado   │    │ 12:01:05 Bomba ON    │
│              │    │ [Nodo] [Panel]   │    │ Usuario: rodrigo (op) │    │ 12:01:05 Suelo 29 %  │
│ Usuario      │    │                  │    │                       │    │ 12:01:00 Suelo 31 %  │
│ [__________] │ →  │ 2. Broker        │ →  │   Suelo     29 %      │ →  │ 12:00:55 Suelo 33 %  │
│ Contraseña   │    │ [10.0.2.2:1883]  │    │   ▂▃▅▃▂ (últimos min) │    │ ...                  │
│ [••••••••••] │    │                  │    │   Aire      24.5 °C   │    │                      │
│  [Ingresar]  │    │ 3. Código        │    │                       │    │ [Solo bomba] [Todo]  │
│ Crear cuenta │    │ [••••••]         │    │ Bomba: ENCENDIDA      │    │      [Volver]        │
└──────────────┘    │    [Conectar]    │    │ Modo:  MANUAL         │    └──────────────────────┘
                    └──────────────────┘    │ Umbral: 35 % [−] [+]  │
                             │              │ [Apagar bomba] [AUTO] │
                             ▼              │ [Ver historial]       │
                    ┌──────────────────┐    │ [Desconectar]         │
                    │ Nodo             │    └───────────────────────┘
                    │ Enlace: ● Conect.│
                    │ Suelo ━━━●━━ 29 %│     Notificación en el panel:
                    │ Aire  ━━●━━ 24.5 │    ┌───────────────────────────────┐
                    │ Bomba: ENCENDIDA │    │ 💧 RiegoIoT · Riego encendido  │
                    │ Modo: AUTO       │    │ Suelo al 29 % (umbral 35 %)    │
                    │ [Desconectar]    │    └───────────────────────────────┘
                    └──────────────────┘
```

- El **observador** ve el panel sin los botones de control.
- Si el enlace se corta, el panel muestra **Enlace: ○ Caído** en rojo, y el nodo apaga la bomba y pasa a **SEGURO**.

### 2.4 Prueba del diseño con hertzbench

Antes de programar, el protocolo se probó en hertzbench, controlado desde Claude Code por MCP, con el escenario `riego-mqtt.yaml` de [ProyectRiego](https://github.com/rodrigocutinoa/ProyectRiego/blob/main/hertzbench/riego-mqtt.yaml). Ese escenario tiene un broker MQTT y un nodo de riego simulados en un canal Wi-Fi.

| Tiempo | Acción | Resultado |
|---|---|---|
| 0 s | Arrancan el broker y el nodo | El nodo se conecta y publica `LECTURA;suelo;42` y su estado |
| 4,2 s | Se publica `CMD;bomba;ON` (como el panel) | El nodo lo recibe 2 ms después |
| 9,4 s | Se corta el enlace del nodo (`inject_fault`, 100 % de pérdida) y se publica `CMD;bomba;OFF` | **El comando no llega** y se pierden las lecturas |
| 17,3 s | Se restablece el enlace | — |
| 20–21 s | El nodo detecta la caída y se reconecta solo | El `CMD;bomba;OFF` **nunca se entrega**: MQTT con QoS 0 no lo reenvía |

**Conclusión:** sin más medidas, un corte de 8 segundos puede dejar la bomba encendida mientras el panel cree que la apagó. Por eso se agregan las tres reglas de la sección 2.2: confirmación con reenvío, estado retenido y estado seguro en el nodo. La misma prueba se repetirá con la app terminada (Hito 4) y en la demostración.

### 2.5 Login y autenticación (sumativa)

Se usa **SQLite local**: el sistema opera en terreno sin Internet garantizado, hay pocos usuarios por equipo y no se necesita sincronizar entre sedes. La contraseña nunca se guarda: se almacena un resumen **PBKDF2-SHA256 con sal aleatoria de 16 bytes y 120 000 iteraciones**. El primer usuario registrado es **operador** (controla la bomba, el modo y el umbral); los siguientes son **observadores** (solo monitorean). Tras **5 intentos fallidos**, el login se bloquea **30 segundos**. Si más adelante se necesita compartir usuarios entre equipos, se migra a Firebase Authentication.
