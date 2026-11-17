package cl.inacap.riegoiot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.inacap.riegoiot.Sesion
import java.util.Locale

// Este teléfono hace de nodo de riego: un sensor de humedad del suelo y uno de
// temperatura (deslizadores) y la bomba, que se controla sola en AUTO o desde el panel.
@Composable
fun PantallaNodo(modifier: Modifier, onSalir: () -> Unit) {
    val enlace = Sesion.enlace ?: run { LaunchedEffect(Unit) { onSalir() }; return }
    val estado by enlace.estado.collectAsState()
    val suelo by Sesion.suelo.collectAsState()
    val temp by Sesion.temp.collectAsState()
    val bomba by Sesion.bomba.collectAsState()
    val modo by Sesion.modo.collectAsState()
    val umbral by Sesion.umbral.collectAsState()
    val rechazados by enlace.rechazados.collectAsState()

    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Nodo de riego (simulado)", style = MaterialTheme.typography.headlineSmall)
        TextoEnlace(estado)
        if (rechazados > 0) Text(
            "Comandos rechazados: $rechazados (sin cifrar, otro código, alterados o viejos)",
            color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
        )
        Text("Humedad del suelo: ${suelo ?: "—"} %")
        Slider(
            value = (suelo ?: 50).toFloat(), onValueChange = { Sesion.cambiarSuelo(it.toInt()) },
            valueRange = 0f..100f,
        )
        Text("Temperatura del aire: " + (temp?.let { String.format(Locale.US, "%.1f °C", it) } ?: "—"))
        Slider(
            value = (temp ?: 22.0).toFloat(), onValueChange = { Sesion.cambiarTemp(it.toDouble()) },
            valueRange = 0f..45f,
        )
        TextoBomba(bomba)
        Text("Modo: ${modo ?: "—"} · Umbral: ${umbral ?: "—"} %")
        Text(
            "En AUTO la bomba se enciende bajo el umbral y se apaga al llegar a umbral + ${Sesion.HISTERESIS} %. " +
                "Si el enlace se corta, la bomba se apaga y el nodo pasa a SEGURO.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(onClick = { Sesion.cerrar(); onSalir() }) { Text("Desconectar") }
    }
}
