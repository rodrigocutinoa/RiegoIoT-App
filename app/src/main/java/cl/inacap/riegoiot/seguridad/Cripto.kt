package cl.inacap.riegoiot.seguridad

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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
}
