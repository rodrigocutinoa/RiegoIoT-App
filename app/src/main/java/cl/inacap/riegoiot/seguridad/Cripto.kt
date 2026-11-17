package cl.inacap.riegoiot.seguridad

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Cripto {
    private const val ITERACIONES = 120_000 // PBKDF2 tarda a propósito: frena los ataques de fuerza bruta

    fun nuevaSal(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    // PBKDF2-SHA256: la contraseña nunca se guarda, solo este resumen con sal.
    fun hash(clave: CharArray, sal: ByteArray): ByteArray {
        val spec = PBEKeySpec(clave, sal, ITERACIONES, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    // Comparación en tiempo constante, para no filtrar información por cuánto tarda.
    fun verificar(clave: CharArray, sal: ByteArray, esperado: ByteArray): Boolean =
        MessageDigest.isEqual(hash(clave, sal), esperado)

    // ---------- Cifrado de los mensajes (Hito 4) ----------

    // Clave AES-256 derivada del código de vinculación de 6 dígitos que se escribe en el nodo
    // y en el panel. La sal es fija para que ambos lleguen a la misma clave (límite declarado:
    // sirve para la demo; en producción se usaría ECDH o el Android Keystore).
    private val SAL_CODIGO = "RiegoIoT-v1".toByteArray()

    fun claveDesdeCodigo(codigo: String): SecretKey =
        SecretKeySpec(hash(codigo.toCharArray(), SAL_CODIGO), "AES")

    // AES-GCM cifra y además detecta si el mensaje fue alterado. El tópico va como dato
    // autenticado (AAD): un mensaje cifrado para riego/estado no sirve en riego/cmd.
    // Salida: Base64(iv de 12 bytes + datos cifrados + etiqueta).
    fun cifrar(texto: String, k: SecretKey, topico: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, k) // IV aleatorio nuevo en cada mensaje
        c.updateAAD(topico.toByteArray())
        return Base64.getEncoder().encodeToString(c.iv + c.doFinal(texto.toByteArray()))
    }

    // null si no se puede descifrar: código distinto, tópico cambiado o datos alterados.
    fun descifrar(base64: String, k: SecretKey, topico: String): String? = try {
        val datos = Base64.getDecoder().decode(base64.trim())
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, k, GCMParameterSpec(128, datos, 0, 12))
        c.updateAAD(topico.toByteArray())
        String(c.doFinal(datos, 12, datos.size - 12))
    } catch (e: Exception) {
        null
    }
}
