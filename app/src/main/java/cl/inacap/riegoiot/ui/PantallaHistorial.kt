package cl.inacap.riegoiot.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.inacap.riegoiot.datos.BaseDatos
import cl.inacap.riegoiot.datos.Lectura
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Texto legible de una fila del historial.
fun describir(l: Lectura): String = when (l.clave) {
    "suelo" -> "Suelo ${l.valor} %"
    "temp" -> "Aire ${l.valor} °C"
    "bomba" -> if (l.valor == "ON") "Bomba ENCENDIDA" else "Bomba APAGADA"
    else -> "${l.clave} ${l.valor}"
}

@Composable
fun PantallaHistorial(db: BaseDatos, modifier: Modifier, onVolver: () -> Unit) {
    var soloBomba by rememberSaveable { mutableStateOf(false) }
    // Se lee de SQLite fuera del hilo de la interfaz, y de nuevo al cambiar el filtro.
    val lecturas by produceState(emptyList<Lectura>(), soloBomba) {
        value = withContext(Dispatchers.IO) { db.lecturas(limite = 100, soloBomba = soloBomba) }
    }
    val hora = remember { SimpleDateFormat("dd-MM HH:mm:ss", Locale.getDefault()) }

    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Historial", style = MaterialTheme.typography.headlineSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !soloBomba, onClick = { soloBomba = false }, label = { Text("Todo") })
            FilterChip(selected = soloBomba, onClick = { soloBomba = true }, label = { Text("Solo bomba") })
        }
        Text("Últimos ${lecturas.size} registros, del más reciente al más antiguo.", style = MaterialTheme.typography.bodySmall)
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(lecturas) { l ->
                Text(
                    "${hora.format(Date(l.momento))}   ${describir(l)}",
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = if (l.clave == "bomba") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                HorizontalDivider()
            }
        }
        Button(onClick = onVolver, modifier = Modifier.fillMaxWidth()) { Text("Volver al panel") }
    }
}
