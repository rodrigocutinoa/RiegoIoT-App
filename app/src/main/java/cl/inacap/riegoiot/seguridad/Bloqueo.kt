package cl.inacap.riegoiot.seguridad

// Bloqueo del login tras 5 intentos fallidos seguidos, durante 30 segundos.
// Vive fuera de la pantalla, así que girar el teléfono no lo reinicia.
class Bloqueo(
    private val maxFallos: Int = 5,
    private val duracionMs: Long = 30_000,
    private val ahora: () -> Long = System::currentTimeMillis,
) {
    private var fallos = 0
    private var hasta = 0L

    // Milisegundos que faltan para poder intentar de nuevo; 0 si no está bloqueado.
    fun restanteMs(): Long = (hasta - ahora()).coerceAtLeast(0)

    fun registrarFallo() {
        fallos++
        if (fallos >= maxFallos) {
            hasta = ahora() + duracionMs
            fallos = 0
        }
    }

    fun registrarExito() {
        fallos = 0
        hasta = 0
    }

    companion object {
        val login = Bloqueo()
    }
}
