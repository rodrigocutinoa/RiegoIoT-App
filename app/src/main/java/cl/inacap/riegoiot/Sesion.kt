package cl.inacap.riegoiot

import android.content.Context
import cl.inacap.riegoiot.enlace.Mensaje
import cl.inacap.riegoiot.enlace.MqttEnlace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

// El «cerebro» de la app: guarda la conexión activa y el estado del riego en flujos
// (StateFlow) que las pantallas observan. Vive fuera de las pantallas, así que no se
// pierde al rotar el teléfono ni al cambiar de pantalla.
object Sesion {
    const val HISTERESIS = 5 // la bomba se apaga al llegar a umbral + 5 % (igual que en la Unidad 1)

    var usuario: String? = null
    var rol = "operador" // Hito 5: el login asignará operador u observador

    var enlace: MqttEnlace? = null
        private set
    var esNodo = false
        private set

    // Estado del riego. En el nodo es la fuente de verdad; en el panel, lo último que informó el nodo.
    val suelo = MutableStateFlow<Int?>(null)
    val temp = MutableStateFlow<Double?>(null)
    val bomba = MutableStateFlow<Boolean?>(null)
    val modo = MutableStateFlow<String?>(null)
    val umbral = MutableStateFlow<Int?>(null)
    val nodoEnLinea = MutableStateFlow(false) // solo panel: ¿el nodo está publicando?

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var trabajo: Job? = null

    fun iniciar(ctx: Context, nuevo: MqttEnlace, comoNodo: Boolean) {
        cerrar()
        enlace = nuevo
        esNodo = comoNodo
        val app = ctx.applicationContext
        if (comoNodo) {
            suelo.value = 50; temp.value = 22.0; bomba.value = false; modo.value = "AUTO"; umbral.value = 35
        }
        trabajo = alcance.launch {
            launch { vigilarEnlace(app, nuevo) }
            if (comoNodo) launch { publicarLecturas(nuevo) }
            nuevo.entrantes.collect { m -> if (comoNodo) atenderNodo(app, m) else atenderPanel(app, m) }
        }
    }

    // Si se corta el enlace: avisar y, en el nodo, pasar al estado seguro (F5).
    private suspend fun vigilarEnlace(app: Context, e: MqttEnlace) {
        var antes = ""
        e.estado.collect { ahora ->
            if (antes == "Conectado" && ahora != "Conectado") {
                if (esNodo) {
                    bomba.value = false
                    modo.value = "SEGURO"
                    Notificaciones.mostrar(app, "Enlace perdido", "Bomba apagada por seguridad (modo SEGURO)")
                } else {
                    nodoEnLinea.value = false
                    Notificaciones.mostrar(app, "Enlace perdido", "Se cortó la conexión con el broker")
                }
            }
            // Al (re)conectar, el nodo informa su estado real.
            if (esNodo && ahora == "Conectado" && antes != "Conectado") publicarEstado(e)
            antes = ahora
        }
    }

    // Nodo: publica las lecturas cada 5 s y repite el estado cada 15 s, para que un panel
    // que se conecta después lo vea sin esperar un cambio.
    private suspend fun publicarLecturas(e: MqttEnlace) {
        var vuelta = 1 // el primer estado lo publica vigilarEnlace al conectar
        while (true) {
            suelo.value?.let { e.enviar(Mensaje("LECTURA", "suelo", it.toString())) }
            temp.value?.let { e.enviar(Mensaje("LECTURA", "temp", String.format(Locale.US, "%.1f", it))) }
            if (vuelta++ % 3 == 0) publicarEstado(e)
            delay(5_000)
        }
    }

    private suspend fun publicarEstado(e: MqttEnlace) {
        e.enviar(Mensaje("ESTADO", "modo", modo.value ?: return))
        e.enviar(Mensaje("ESTADO", "bomba", if (bomba.value == true) "ON" else "OFF"))
        umbral.value?.let { e.enviar(Mensaje("ESTADO", "umbral", it.toString())) }
    }

    // ---------- Nodo ----------

    // Lo llama la pantalla del nodo al mover el deslizador de humedad.
    fun cambiarSuelo(valor: Int) {
        suelo.value = valor
        if (modo.value == "AUTO") evaluarAuto()
    }

    fun cambiarTemp(valor: Double) {
        temp.value = valor
    }

    // Modo automático con histéresis: enciende bajo el umbral y apaga al llegar a umbral + 5 %.
    private fun evaluarAuto() {
        val s = suelo.value ?: return
        val u = umbral.value ?: return
        when {
            s < u && bomba.value != true -> ponerBomba(true)
            s >= u + HISTERESIS && bomba.value == true -> ponerBomba(false)
        }
    }

    private fun ponerBomba(encendida: Boolean) {
        bomba.value = encendida
        val e = enlace ?: return
        alcance.launch { e.enviar(Mensaje("ESTADO", "bomba", if (encendida) "ON" else "OFF")) }
    }

    private suspend fun atenderNodo(app: Context, m: Mensaje) {
        if (m.tipo != "CMD") return
        val e = enlace ?: return
        when (m.clave) {
            "bomba" -> {
                // Un comando manual deja el nodo en modo MANUAL hasta que el panel pida AUTO.
                modo.value = "MANUAL"
                e.enviar(Mensaje("ESTADO", "modo", "MANUAL"))
                ponerBomba(m.valor == "ON")
            }
            "modo" -> {
                if (m.valor != "AUTO") return // desde el panel solo se puede volver a AUTO
                modo.value = "AUTO"
                e.enviar(Mensaje("ESTADO", "modo", "AUTO"))
                evaluarAuto()
            }
            "umbral" -> {
                umbral.value = m.valor.toInt()
                if (modo.value == "AUTO") evaluarAuto()
            }
            else -> return
        }
        e.enviar(Mensaje("ACK", m.clave, m.valor))
        Notificaciones.mostrar(app, "Comando recibido", "${m.clave} → ${m.valor}")
    }

    // ---------- Panel ----------

    private fun atenderPanel(app: Context, m: Mensaje) {
        if (m.clave == "enlace") { // Last Will del nodo
            nodoEnLinea.value = false
            Notificaciones.mostrar(app, "Nodo desconectado", "El nodo de riego perdió el enlace")
            return
        }
        nodoEnLinea.value = true
        when (m.clave) {
            "suelo" -> suelo.value = m.valor.toInt()
            "temp" -> temp.value = m.valor.toDouble()
            "modo" -> modo.value = m.valor
            "umbral" -> umbral.value = m.valor.toInt()
            "bomba" -> {
                val encendida = m.valor == "ON"
                val anterior = bomba.value
                bomba.value = encendida
                // Requisito del profesor: notificar cada vez que el riego se enciende o se apaga.
                if (m.tipo == "ESTADO" && anterior != null && anterior != encendida) {
                    val detalle = suelo.value?.let { "Suelo al $it %" } ?: ""
                    Notificaciones.mostrar(app, if (encendida) "Riego encendido" else "Riego apagado", detalle)
                }
            }
        }
    }

    // Lo llaman los botones del panel.
    fun enviarComando(clave: String, valor: String) {
        val e = enlace ?: return
        alcance.launch { e.enviar(Mensaje("CMD", clave, valor)) }
    }

    fun cerrar() {
        trabajo?.cancel()
        trabajo = null
        enlace?.cerrar()
        enlace = null
        suelo.value = null
        temp.value = null
        bomba.value = null
        modo.value = null
        umbral.value = null
        nodoEnLinea.value = false
    }
}
