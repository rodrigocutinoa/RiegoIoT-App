package cl.inacap.riegoiot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.inacap.riegoiot.Sesion
import java.util.Locale

@Composable
fun PantallaPanel(modifier: Modifier, onSalir: () -> Unit) {
    val enlace = Sesion.enlace ?: run { LaunchedEffect(Unit) { onSalir() }; return }
    val estado by enlace.estado.collectAsState()
    val nodoEnLinea by Sesion.nodoEnLinea.collectAsState()
    val suelo by Sesion.suelo.collectAsState()
    val temp by Sesion.temp.collectAsState()
    val bomba by Sesion.bomba.collectAsState()
    val modo by Sesion.modo.collectAsState()
    val umbral by Sesion.umbral.collectAsState()
    val puedeControlar = Sesion.rol == "operador" // control de acceso por rol (Hito 5)
    val habilitado = puedeControlar && estado == "Conectado" && nodoEnLinea

    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Panel de riego", style = MaterialTheme.typography.headlineSmall)
        TextoEnlace(estado)
        Text(
            if (nodoEnLinea) "Nodo: en línea" else "Nodo: sin datos",
            color = if (nodoEnLinea) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Text("Suelo ${suelo?.let { "$it %" } ?: "— %"}", style = MaterialTheme.typography.displaySmall)
        Text("Aire " + (temp?.let { String.format(Locale.US, "%.1f °C", it) } ?: "— °C"))
        TextoBomba(bomba)
        Text("Modo: ${modo ?: "—"} · Umbral: ${umbral?.let { "$it %" } ?: "—"}")

        Button(
            enabled = habilitado && bomba != null,
            onClick = { Sesion.enviarComando("bomba", if (bomba == true) "OFF" else "ON") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (bomba == true) "Apagar bomba" else "Encender bomba") }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                enabled = habilitado && umbral != null && umbral!! > 10,
                onClick = { Sesion.enviarComando("umbral", (umbral!! - 5).coerceAtLeast(10).toString()) },
            ) { Text("Umbral −5") }
            OutlinedButton(
                enabled = habilitado && umbral != null && umbral!! < 90,
                onClick = { Sesion.enviarComando("umbral", (umbral!! + 5).coerceAtMost(90).toString()) },
            ) { Text("Umbral +5") }
            OutlinedButton(
                enabled = habilitado && modo != "AUTO",
                onClick = { Sesion.enviarComando("modo", "AUTO") },
            ) { Text("AUTO") }
        }
        if (!puedeControlar) Text("Tu rol es observador: puedes monitorear, pero no enviar comandos.")
        OutlinedButton(onClick = { Sesion.cerrar(); onSalir() }, modifier = Modifier.fillMaxWidth()) {
            Text("Desconectar")
        }
    }
}
