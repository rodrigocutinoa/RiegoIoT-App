package cl.inacap.riegoiot

import cl.inacap.riegoiot.seguridad.Bloqueo
import cl.inacap.riegoiot.seguridad.Cripto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeguridadTest {

    @Test
    fun laContrasenaCorrectaVerificaYLaIncorrectaNo() {
        val sal = Cripto.nuevaSal()
        val hash = Cripto.hash("riego2026!".toCharArray(), sal)
        assertEquals(16, sal.size)
        assertEquals(32, hash.size) // 256 bits
        assertTrue(Cripto.verificar("riego2026!".toCharArray(), sal, hash))
        assertFalse(Cripto.verificar("riego2026?".toCharArray(), sal, hash))
    }

    @Test
    fun laMismaContrasenaConOtraSalDaOtroHash() {
        val clave = "riego2026!"
        assertNotEquals(
            Cripto.hash(clave.toCharArray(), Cripto.nuevaSal()).toList(),
            Cripto.hash(clave.toCharArray(), Cripto.nuevaSal()).toList(),
        )
    }

    @Test
    fun cincoFallosBloqueanTreintaSegundos() {
        var reloj = 1_000L
        val b = Bloqueo(ahora = { reloj })
        repeat(4) { b.registrarFallo() }
        assertEquals(0, b.restanteMs()) // 4 fallos: todavía no
        b.registrarFallo()
        assertEquals(30_000, b.restanteMs()) // el quinto bloquea
        reloj += 29_000
        assertEquals(1_000, b.restanteMs())
        reloj += 1_000
        assertEquals(0, b.restanteMs()) // pasados 30 s, se puede intentar de nuevo
    }

    @Test
    fun unIngresoCorrectoReiniciaLosFallos() {
        val b = Bloqueo(ahora = { 0L })
        repeat(4) { b.registrarFallo() }
        b.registrarExito()
        repeat(4) { b.registrarFallo() }
        assertEquals(0, b.restanteMs())
    }
}
