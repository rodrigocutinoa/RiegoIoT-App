package cl.inacap.riegoiot.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.inacap.riegoiot.Sesion
import cl.inacap.riegoiot.datos.BaseDatos
import cl.inacap.riegoiot.enlace.MqttEnlace
import cl.inacap.riegoiot.seguridad.Cripto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Android 17 pide un permiso para conectarse a direcciones de la red local.
private fun tienePermisoRedLocal(ctx: Context) =
    Build.VERSION.SDK_INT < 37 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_LOCAL_NETWORK) ==
        PackageManager.PERMISSION_GRANTED

// Elige el rol (nodo o panel) y el broker MQTT. Desde el emulador, el puente de
// hertzbench que corre en el computador está en 10.0.2.2:1883.
@Composable
fun PantallaConexion(db: BaseDatos, modifier: Modifier, onListo: (rol: String) -> Unit, onCerrarSesion: () -> Unit) {
    val ctx = LocalContext.current
    val alcance = rememberCoroutineScope()
    var esNodo by rememberSaveable { mutableStateOf(false) }
    var host by rememberSaveable { mutableStateOf("10.0.2.2") }
    var puerto by rememberSaveable { mutableStateOf("1883") }
    var codigo by remember { mutableStateOf("") } // no se guarda al rotar
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var conectando by rememberSaveable { mutableStateOf(false) }

    fun iniciar() {
        val p = puerto.toIntOrNull()
        error = when {
            host.isBlank() -> "Escribe la dirección del broker."
            p == null || p !in 1..65535 -> "El puerto debe ser un número entre 1 y 65535."
            !codigo.matches(Regex("\\d{6}")) -> "El código de vinculación debe tener 6 dígitos (el mismo en el nodo y el panel)."
            !tienePermisoRedLocal(ctx) ->
                "Falta el permiso de dispositivos cercanos / red local (Ajustes → Apps → RiegoIoT → Permisos)."
            else -> null
        }
        if (error != null) return
        conectando = true
        alcance.launch {
            // PBKDF2 tarda a propósito: la clave se deriva fuera del hilo de la interfaz.
            val clave = withContext(Dispatchers.Default) { Cripto.claveDesdeCodigo(codigo) }
            val enlace = MqttEnlace(host.trim(), p!!, esNodo, clave)
            if (enlace.conectar()) {
                Sesion.iniciar(ctx, db, enlace, esNodo)
                onListo(if (esNodo) "nodo" else "panel")
            } else {
                error = "${enlace.estado.value}. ¿Está corriendo riego-mqtt.yaml en hertzbench?"
                enlace.cerrar()
            }
            conectando = false
        }
    }

    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("RiegoIoT", style = MaterialTheme.typography.headlineMedium)
        Text("Usuario: ${Sesion.usuario} (${Sesion.rol})", style = MaterialTheme.typography.bodyMedium)
        Text("1. Rol de este teléfono")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !esNodo, onClick = { esNodo = false }, label = { Text("Panel") })
            FilterChip(selected = esNodo, onClick = { esNodo = true }, label = { Text("Nodo de riego") })
        }
        Text("2. Broker MQTT")
        OutlinedTextField(
            host, { host = it }, label = { Text("Dirección") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            puerto, { puerto = it.filter(Char::isDigit) }, label = { Text("Puerto") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        Text("3. Código de vinculación")
        OutlinedTextField(
            codigo, { codigo = it.filter(Char::isDigit).take(6) }, label = { Text("6 dígitos, igual en nodo y panel") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = ::iniciar, enabled = !conectando, modifier = Modifier.fillMaxWidth()) {
            Text(if (conectando) "Conectando…" else "Conectar")
        }
        OutlinedButton(onClick = onCerrarSesion, enabled = !conectando, modifier = Modifier.fillMaxWidth()) {
            Text("Cerrar sesión")
        }
    }
}
