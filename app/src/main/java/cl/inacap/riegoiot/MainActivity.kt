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
import cl.inacap.riegoiot.datos.BaseDatos
import cl.inacap.riegoiot.ui.PantallaConexion
import cl.inacap.riegoiot.ui.PantallaLogin
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
        val db = BaseDatos(this)
        setContent {
            RiegoIoTAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { relleno ->
                    var pantalla by rememberSaveable {
                        mutableStateOf(if (Sesion.usuario == null) "login" else "conexion")
                    }
                    val m = Modifier.padding(relleno)
                    // Sin sesión iniciada no se entra a ninguna otra pantalla.
                    if (Sesion.usuario == null && pantalla != "login") pantalla = "login"
                    when (pantalla) {
                        "login" -> PantallaLogin(db, m) { pantalla = "conexion" }
                        "conexion" -> PantallaConexion(
                            m,
                            onListo = { rol -> pantalla = rol },
                            onCerrarSesion = { Sesion.cerrarSesion(); pantalla = "login" },
                        )
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
