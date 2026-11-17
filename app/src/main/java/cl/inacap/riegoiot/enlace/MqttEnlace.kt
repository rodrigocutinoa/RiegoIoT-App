package cl.inacap.riegoiot.enlace

import android.util.Log
import cl.inacap.riegoiot.seguridad.Cripto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.math.abs

private const val VENTANA_CMD_MS = 30_000L

// Conexión con el broker MQTT (el puente de hertzbench, o el broker real en la Unidad 4).
// El resto de la app solo conoce `estado`, `entrantes`, `enviar` y `cerrar`.
class MqttEnlace(
    host: String,
    puerto: Int,
    private val esNodo: Boolean,
    private val clave: SecretKey, // derivada del código de vinculación
) {
    // Mensajes descartados: sin cifrar, con otro código, alterados, fuera de formato o comandos viejos.
    private val _rechazados = MutableStateFlow(0)
    val rechazados: StateFlow<Int> = _rechazados

    private val _estado = MutableStateFlow("Desconectado")
    val estado: StateFlow<String> = _estado

    private val _entrantes = MutableSharedFlow<Mensaje>(extraBufferCapacity = 64)
    val entrantes: SharedFlow<Mensaje> = _entrantes

    private val cliente = MqttClient(
        "tcp://$host:$puerto",
        (if (esNodo) "riegoiot-nodo-" else "riegoiot-panel-") + UUID.randomUUID().toString().take(8),
        MemoryPersistence(),
    )

    // El nodo escucha comandos; el panel escucha lecturas, estado y confirmaciones.
    private val suscripciones =
        if (esNodo) arrayOf("riego/cmd") else arrayOf("riego/lectura", "riego/estado/#", "riego/ack")

    init {
        cliente.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String) {
                // Tras una reconexión automática hay que volver a suscribirse.
                try {
                    cliente.subscribe(suscripciones, IntArray(suscripciones.size) { 0 })
                    _estado.value = "Conectado"
                } catch (e: MqttException) {
                    _estado.value = "Error: ${e.message}"
                }
            }

            override fun connectionLost(causa: Throwable?) {
                _estado.value = "Reconectando…"
            }

            override fun messageArrived(topico: String, msg: MqttMessage) {
                val m = abrir(topico, String(msg.payload, Charsets.UTF_8))
                if (m == null) _rechazados.value++ else _entrantes.tryEmit(m)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })
    }

    // Lo que viaja es Base64(AES-GCM("<hora en ms>|TIPO;clave;valor")). Devuelve null si algo no calza.
    private fun abrir(topico: String, payload: String): Mensaje? {
        val texto = Cripto.descifrar(payload, clave, topico) ?: return null
        val hora = texto.substringBefore('|', "").toLongOrNull() ?: return null
        val m = Mensaje.desdeLinea(texto.substringAfter('|')) ?: return null
        if (m.topico != topico) return null // cada tipo de mensaje va por su propio tópico
        // Contra la repetición de comandos grabados: el nodo rechaza los de más de 30 s.
        // (El estado retenido puede ser antiguo a propósito, por eso no se aplica a él.)
        if (m.tipo == "CMD" && abs(System.currentTimeMillis() - hora) > VENTANA_CMD_MS) return null
        return m
    }

    private fun sellar(m: Mensaje): ByteArray =
        Cripto.cifrar("${System.currentTimeMillis()}|${m.aLinea()}", clave, m.topico).toByteArray(Charsets.UTF_8)

    suspend fun conectar() = withContext(Dispatchers.IO) {
        _estado.value = "Conectando…"
        val opciones = MqttConnectOptions().apply {
            isCleanSession = true
            isAutomaticReconnect = true
            connectionTimeout = 5
            keepAliveInterval = 5 // detecta un corte en ~10 s
            if (esNodo) {
                // Last Will: si el nodo se cae sin despedirse, el broker avisa al panel.
                val will = Mensaje("ESTADO", "enlace", "CAIDO")
                setWill(will.topico, sellar(will), 0, false)
            }
        }
        try {
            cliente.connect(opciones)
            cliente.subscribe(suscripciones, IntArray(suscripciones.size) { 0 })
            _estado.value = "Conectado"
            true
        } catch (e: MqttException) {
            Log.w("RiegoIoT", "No se pudo conectar al broker", e)
            val causa = e.cause?.let { "${it.javaClass.simpleName}: ${it.message}" } ?: "código ${e.reasonCode}"
            _estado.value = "Error: no se pudo conectar ($causa)"
            false
        }
    }

    suspend fun enviar(m: Mensaje) = withContext(Dispatchers.IO) {
        if (!cliente.isConnected) return@withContext false
        try {
            cliente.publish(m.topico, sellar(m), 0, m.retenido)
            true
        } catch (e: MqttException) {
            false
        }
    }

    fun cerrar() {
        try {
            if (cliente.isConnected) cliente.disconnect(500)
        } catch (_: MqttException) {
        }
        try {
            cliente.close()
        } catch (_: MqttException) {
        }
        _estado.value = "Desconectado"
    }
}
