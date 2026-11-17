package cl.inacap.riegoiot

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cl.inacap.riegoiot.ui.PantallaConexion
import cl.inacap.riegoiot.ui.PantallaNodo
import cl.inacap.riegoiot.ui.PantallaPanel
import cl.inacap.riegoiot.ui.theme.RiegoIoTAppTheme

class MainActivity : ComponentActivity() {
    private val pedirPermisos =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notificaciones.crearCanal(this)
        permisosNecesarios().takeIf { it.isNotEmpty() }?.let { pedirPermisos.launch(it) }
        setContent {
            RiegoIoTAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { relleno ->
                    // Hito 5 agrega la pantalla de login antes de «conexion».
                    var pantalla by rememberSaveable { mutableStateOf("conexion") }
                    val m = Modifier.padding(relleno)
                    when (pantalla) {
                        "conexion" -> PantallaConexion(m) { rol -> pantalla = rol }
                        "nodo" -> PantallaNodo(m) { pantalla = "conexion" }
                        "panel" -> PantallaPanel(m) { pantalla = "conexion" }
                    }
                }
            }
        }
    }

    // Solo se piden los permisos peligrosos que existen en la versión de Android del teléfono.
    // INTERNET es un permiso normal: se concede al instalar.
    private fun permisosNecesarios(): Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        if (Build.VERSION.SDK_INT >= 37) add(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }.toTypedArray()
}
