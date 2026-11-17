package cl.inacap.riegoiot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.inacap.riegoiot.Sesion
import cl.inacap.riegoiot.datos.BaseDatos
import cl.inacap.riegoiot.seguridad.Bloqueo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PantallaLogin(db: BaseDatos, modifier: Modifier, onIngreso: () -> Unit) {
    var usuario by rememberSaveable { mutableStateOf("") }
    var clave by remember { mutableStateOf("") } // no se guarda al rotar
    var mensaje by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    fun ingresar(registrar: Boolean) {
        val restante = Bloqueo.login.restanteMs()
        if (restante > 0) {
            mensaje = "Demasiados intentos. Espera ${(restante + 999) / 1000} segundos."
            return
        }
        val nombre = usuario.trim()
        if (!nombre.matches(Regex("[A-Za-z0-9._-]{3,32}"))) {
            mensaje = "El usuario debe tener de 3 a 32 letras, números, punto, guion o guion bajo."
            return
        }
        if (clave.length < 8) {
            mensaje = "La contraseña debe tener al menos 8 caracteres."
            return
        }
        ocupado = true
        mensaje = null
        alcance.launch {
            // PBKDF2 tarda a propósito: se calcula fuera del hilo de la interfaz.
            val rol = withContext(Dispatchers.Default) {
                val c = clave.toCharArray()
                try {
                    if (registrar) db.registrar(nombre, c) else db.autenticar(nombre, c)
                } finally {
                    c.fill('\u0000')
                }
            }
            ocupado = false
            when {
                rol != null -> {
                    Bloqueo.login.registrarExito()
                    Sesion.usuario = nombre
                    Sesion.rol = rol
                    clave = ""
                    mensaje = null
                    onIngreso()
                }
                registrar -> mensaje = "Ese usuario ya existe."
                else -> {
                    Bloqueo.login.registrarFallo()
                    mensaje = if (Bloqueo.login.restanteMs() > 0) "Demasiados intentos. Espera 30 segundos."
                    else "Usuario o contraseña incorrectos."
                }
            }
        }
    }

    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("RiegoIoT", style = MaterialTheme.typography.headlineMedium)
        Text("Inicia sesión para monitorear y controlar el riego.")
        OutlinedTextField(
            usuario, { usuario = it }, label = { Text("Usuario") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            clave, { clave = it }, label = { Text("Contraseña") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        Button(onClick = { ingresar(registrar = false) }, enabled = !ocupado, modifier = Modifier.fillMaxWidth()) {
            Text(if (ocupado) "Verificando…" else "Ingresar")
        }
        OutlinedButton(onClick = { ingresar(registrar = true) }, enabled = !ocupado, modifier = Modifier.fillMaxWidth()) {
            Text("Crear cuenta")
        }
        mensaje?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(
            "El primer usuario creado queda como operador (controla la bomba); los siguientes, como observadores (solo monitorean).",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
