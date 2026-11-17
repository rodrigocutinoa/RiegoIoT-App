package cl.inacap.riegoiot

import cl.inacap.riegoiot.enlace.Mensaje
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class MensajeTest {

    @Test
    fun aceptaLosMensajesDelProtocolo() {
        assertEquals(Mensaje("LECTURA", "suelo", "42"), Mensaje.desdeLinea("LECTURA;suelo;42"))
        assertEquals(Mensaje("LECTURA", "temp", "24.5"), Mensaje.desdeLinea("LECTURA;temp;24.5"))
        assertEquals(Mensaje("ESTADO", "bomba", "ON"), Mensaje.desdeLinea("ESTADO;bomba;ON"))
        assertEquals(Mensaje("ESTADO", "modo", "SEGURO"), Mensaje.desdeLinea("ESTADO;modo;SEGURO"))
        assertEquals(Mensaje("ESTADO", "enlace", "CAIDO"), Mensaje.desdeLinea("ESTADO;enlace;CAIDO"))
        assertEquals(Mensaje("CMD", "umbral", "35"), Mensaje.desdeLinea("CMD;umbral;35"))
        assertEquals(Mensaje("ACK", "bomba", "OFF"), Mensaje.desdeLinea("ACK;bomba;OFF\n"))
    }

    @Test
    fun descartaLoQueNoCalzaConElFormato() {
        assertNull(Mensaje.desdeLinea("ON")) // formato antiguo del escenario
        assertNull(Mensaje.desdeLinea("CMD;bomba"))
        assertNull(Mensaje.desdeLinea("CMD;bomba;ON;extra"))
        assertNull(Mensaje.desdeLinea("BORRAR;bomba;ON")) // tipo desconocido
        assertNull(Mensaje.desdeLinea("CMD;valvula;ON")) // clave desconocida
        assertNull(Mensaje.desdeLinea("CMD;bomba;TALVEZ"))
        assertNull(Mensaje.desdeLinea("LECTURA;suelo;150")) // fuera de rango
        assertNull(Mensaje.desdeLinea("LECTURA;temp;24,5")) // coma decimal
        assertNull(Mensaje.desdeLinea("CMD;umbral;5")) // umbral entre 10 y 90
    }

    @Test
    fun cadaTipoVaEnSuTopico() {
        assertEquals("riego/lectura", Mensaje("LECTURA", "suelo", "42").topico)
        assertEquals("riego/estado/bomba", Mensaje("ESTADO", "bomba", "ON").topico)
        assertEquals("riego/estado/modo", Mensaje("ESTADO", "modo", "AUTO").topico)
        assertEquals("riego/cmd", Mensaje("CMD", "bomba", "ON").topico)
        assertEquals("riego/ack", Mensaje("ACK", "bomba", "ON").topico)
        assertEquals("ESTADO;bomba;ON", Mensaje("ESTADO", "bomba", "ON").aLinea())
    }

    @Test
    fun soloElEstadoSeRetiene() {
        assertTrue(Mensaje("ESTADO", "bomba", "ON").retenido)
        assertFalse(Mensaje("ESTADO", "enlace", "CAIDO").retenido) // el Last Will no
        assertFalse(Mensaje("LECTURA", "suelo", "42").retenido)
        assertFalse(Mensaje("CMD", "bomba", "ON").retenido)
    }
}
