package cl.inacap.riegoiot.enlace

// Una línea de texto por mensaje: TIPO;CLAVE;VALOR (Hito 2, sección 2.2).
//   LECTURA;suelo;42 · LECTURA;temp;24.5      nodo → panel   (riego/lectura)
//   ESTADO;bomba;ON  · ESTADO;modo;AUTO       nodo → panel   (riego/estado/<clave>, retenido)
//   CMD;bomba;OFF    · CMD;umbral;35          panel → nodo   (riego/cmd)
//   ACK;bomba;OFF                             nodo → panel   (riego/ack)
data class Mensaje(val tipo: String, val clave: String, val valor: String) {

    fun aLinea() = "$tipo;$clave;$valor"

    // Cada tipo de mensaje viaja en su propio tópico MQTT. El estado usa un tópico por clave
    // (riego/estado/bomba, riego/estado/modo...) porque el broker retiene un mensaje por tópico:
    // así un panel que se conecta recibe al tiro el último valor de cada uno.
    val topico: String get() = if (tipo == "ESTADO") "riego/estado/$clave" else TOPICOS.getValue(tipo)

    // El estado se publica retenido; el Last Will (enlace) no, para no dejar un aviso viejo.
    val retenido: Boolean get() = tipo == "ESTADO" && clave != "enlace"

    companion object {
        val TOPICOS = mapOf(
            "LECTURA" to "riego/lectura",
            "ESTADO" to "riego/estado",
            "CMD" to "riego/cmd",
            "ACK" to "riego/ack",
        )

        // Valores permitidos por clave. Todo lo que no calce se descarta:
        // también es una medida de seguridad (no se procesa basura ni datos inyectados).
        private val ON_OFF = setOf("ON", "OFF")
        private val MODOS = setOf("AUTO", "MANUAL", "SEGURO")

        fun desdeLinea(linea: String): Mensaje? {
            val p = linea.trim().split(";")
            if (p.size != 3) return null
            val (tipo, clave, valor) = p
            if (tipo !in TOPICOS) return null
            val valido = when (clave) {
                "suelo" -> valor.toIntOrNull()?.let { it in 0..100 } == true
                "temp" -> valor.toDoubleOrNull()?.let { it in -40.0..80.0 } == true
                "bomba" -> valor in ON_OFF
                "modo" -> valor in MODOS
                "umbral" -> valor.toIntOrNull()?.let { it in 10..90 } == true
                "enlace" -> valor == "CAIDO"
                else -> false
            }
            return if (valido) Mensaje(tipo, clave, valor) else null
        }
    }
}
