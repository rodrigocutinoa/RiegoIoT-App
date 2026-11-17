package cl.inacap.riegoiot.enlace

import android.util.Log
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

// Conexión con el broker MQTT (el puente de hertzbench, o el broker real en la Unidad 4).
// El resto de la app solo conoce `estado`, `entrantes`, `enviar` y `cerrar`.
class MqttEnlace(
    host: String,
    puerto: Int,
    private val esNodo: Boolean,
) {
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
                val m = Mensaje.desdeLinea(String(msg.payload, Charsets.UTF_8)) ?: return
                // El mensaje debe venir por el tópico que le corresponde a su tipo.
                if (m.topico == topico) _entrantes.tryEmit(m)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })
    }

    suspend fun conectar() = withContext(Dispatchers.IO) {
        _estado.value = "Conectando…"
        val opciones = MqttConnectOptions().apply {
            isCleanSession = true
            isAutomaticReconnect = true
            connectionTimeout = 5
            keepAliveInterval = 5 // detecta un corte en ~10 s
            if (esNodo) {
                // Last Will: si el nodo se cae sin despedirse, el broker avisa al panel.
                setWill("riego/estado/enlace", "ESTADO;enlace;CAIDO".toByteArray(), 0, false)
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
            cliente.publish(m.topico, m.aLinea().toByteArray(Charsets.UTF_8), 0, m.retenido)
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
