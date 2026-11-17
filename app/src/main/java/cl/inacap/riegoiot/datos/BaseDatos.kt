package cl.inacap.riegoiot.datos

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.inacap.riegoiot.seguridad.Cripto

// SQLite local (Hito 2, sección 2.5): funciona sin Internet en el terreno.
class BaseDatos(ctx: Context) : SQLiteOpenHelper(ctx, "riegoiot.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        // Se guarda la sal y el hash, nunca la contraseña.
        db.execSQL("CREATE TABLE usuarios (usuario TEXT PRIMARY KEY, sal BLOB NOT NULL, hash BLOB NOT NULL, rol TEXT NOT NULL)")
        // Lecturas y eventos del riego (Hito 6).
        db.execSQL("CREATE TABLE lecturas (id INTEGER PRIMARY KEY AUTOINCREMENT, momento INTEGER NOT NULL, clave TEXT NOT NULL, valor TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, anterior: Int, nueva: Int) {}

    // El primer usuario queda como "operador"; los siguientes, como "observador".
    // Devuelve el rol asignado, o null si el usuario ya existe.
    fun registrar(usuario: String, clave: CharArray): String? {
        val hayUsuarios = readableDatabase.rawQuery("SELECT 1 FROM usuarios LIMIT 1", null).use { it.moveToFirst() }
        val rol = if (hayUsuarios) "observador" else "operador"
        val sal = Cripto.nuevaSal()
        val fila = ContentValues().apply {
            put("usuario", usuario)
            put("sal", sal)
            put("hash", Cripto.hash(clave, sal))
            put("rol", rol)
        }
        return if (writableDatabase.insert("usuarios", null, fila) == -1L) null else rol
    }

    // Devuelve el rol si las credenciales son válidas; null si no.
    // Las consultas usan ? en vez de pegar texto: eso impide la inyección SQL.
    fun autenticar(usuario: String, clave: CharArray): String? =
        readableDatabase.rawQuery("SELECT sal, hash, rol FROM usuarios WHERE usuario = ?", arrayOf(usuario)).use { c ->
            if (c.moveToFirst() && Cripto.verificar(clave, c.getBlob(0), c.getBlob(1))) c.getString(2) else null
        }
}
