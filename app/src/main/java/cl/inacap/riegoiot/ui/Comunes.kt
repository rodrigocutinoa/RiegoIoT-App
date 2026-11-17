package cl.inacap.riegoiot.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

// Piezas que comparten las pantallas del nodo y del panel.

@Composable
fun TextoEnlace(estado: String) {
    Text(
        "Enlace: $estado",
        color = if (estado == "Conectado") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    )
}

@Composable
fun TextoBomba(bomba: Boolean?) {
    Text(
        when (bomba) {
            true -> "Bomba: ENCENDIDA"
            false -> "Bomba: APAGADA"
            null -> "Bomba: —"
        },
        style = MaterialTheme.typography.headlineMedium,
        color = if (bomba == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
    )
}
