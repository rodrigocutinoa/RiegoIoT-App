package cl.inacap.riegoiot

import cl.inacap.riegoiot.seguridad.Bloqueo
import cl.inacap.riegoiot.seguridad.Cripto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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

    private val k = Cripto.claveDesdeCodigo("123456")

    @Test
    fun elMensajeCifradoSeRecuperaConElMismoCodigo() {
        val c = Cripto.cifrar("1|CMD;bomba;ON", k, "riego/cmd")
        assertFalse(c.contains("bomba")) // en el aire no se lee
        assertEquals("1|CMD;bomba;ON", Cripto.descifrar(c, Cripto.claveDesdeCodigo("123456"), "riego/cmd"))
    }

    @Test
    fun descifraLoQueCifraElScriptDeHertzbench() {
        // Generado con: deno run ProyectRiego/hertzbench/cifrar.ts 123456 riego/lectura "LECTURA;suelo;42"
        val deDeno = "dnM65gGabpl/HBAme+cntssrbWDHwjGYnGM4qjAGqm49uahqmSdkDeswAKRQzfwCHwdvldN40VvtxQ=="
        val texto = Cripto.descifrar(deDeno, k, "riego/lectura")
        assertTrue(texto!!.endsWith("|LECTURA;suelo;42"))
    }

    @Test
    fun cadaMensajeLlevaUnIvDistinto() {
        assertNotEquals(Cripto.cifrar("x", k, "t"), Cripto.cifrar("x", k, "t"))
    }

    @Test
    fun seRechazaOtroCodigoOtroTopicoAlteradoOSinCifrar() {
        val c = Cripto.cifrar("1|CMD;bomba;ON", k, "riego/cmd")
        assertNull(Cripto.descifrar(c, Cripto.claveDesdeCodigo("654321"), "riego/cmd")) // otro código
        assertNull(Cripto.descifrar(c, k, "riego/estado/bomba")) // movido a otro tópico
        val bytes = java.util.Base64.getDecoder().decode(c).also { it[20] = (it[20].toInt() xor 1).toByte() }
        assertNull(Cripto.descifrar(java.util.Base64.getEncoder().encodeToString(bytes), k, "riego/cmd")) // alterado
        assertNull(Cripto.descifrar("CMD;bomba;ON", k, "riego/cmd")) // texto plano
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
