package com.argon.app

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Herramientas(private val act: Activity) {

    private val falta =
        "Falta un permiso. Ya se lo pedí al usuario: que lo acepte y vuelva a pedirte la tarea."

    private fun tStr(desc: String) = JSONObject().put("type", "string").put("description", desc)
    private fun tInt(desc: String) = JSONObject().put("type", "integer").put("description", desc)

    private fun decl(
        nombre: String, desc: String, props: JSONObject? = null, req: List<String> = emptyList()
    ): JSONObject {
        val d = JSONObject().put("name", nombre).put("description", desc)
        if (props != null) {
            val p = JSONObject().put("type", "object").put("properties", props)
            if (req.isNotEmpty()) p.put("required", JSONArray(req))
            d.put("parameters", p)
        }
        return d
    }

    fun declaraciones(): JSONArray = JSONArray()
        .put(
            decl(
                "leer_contactos",
                "Busca contactos del teléfono por nombre y devuelve nombre y número. Sin nombre, lista los primeros 30.",
                JSONObject().put("nombre", tStr("Nombre o parte del nombre a buscar (opcional)"))
            )
        )
        .put(
            decl(
                "leer_sms",
                "Lee los SMS recibidos más recientes. Úsalo solo cuando el usuario lo pida.",
                JSONObject()
                    .put("cantidad", tInt("Cuántos mensajes leer, máximo 15 (por defecto 5)"))
                    .put("buscar", tStr("Texto a buscar en remitente o mensaje (opcional)"))
            )
        )
        .put(
            decl(
                "poner_alarma",
                "Programa una alarma en el reloj del teléfono.",
                JSONObject()
                    .put("hora", tInt("Hora en formato de 24 horas, de 0 a 23"))
                    .put("minutos", tInt("Minutos, de 0 a 59"))
                    .put("mensaje", tStr("Etiqueta de la alarma (opcional)")),
                listOf("hora", "minutos")
            )
        )
        .put(
            decl(
                "poner_temporizador",
                "Inicia un temporizador de cuenta regresiva.",
                JSONObject()
                    .put("segundos", tInt("Duración en segundos"))
                    .put("mensaje", tStr("Etiqueta (opcional)")),
                listOf("segundos")
            )
        )
        .put(
            decl(
                "guardar_archivo",
                "Guarda un archivo de texto o código en la carpeta Descargas/Argon del teléfono.",
                JSONObject()
                    .put("nombre", tStr("Nombre del archivo con extensión, por ejemplo suma.py"))
                    .put("contenido", tStr("Contenido completo del archivo")),
                listOf("nombre", "contenido")
            )
        )
        .put(decl("ver_bateria", "Devuelve el nivel de batería y si está cargando."))
        .put(decl("ver_ubicacion", "Devuelve la última ubicación conocida del teléfono (latitud y longitud)."))

    fun ejecutar(nombre: String, a: JSONObject): String {
        return try {
            when (nombre) {
                "leer_contactos" -> contactos(a.optString("nombre", ""))
                "leer_sms" -> sms(a.optInt("cantidad", 5), a.optString("buscar", ""))
                "poner_alarma" -> alarma(
                    a.optInt("hora", -1), a.optInt("minutos", 0), a.optString("mensaje", "Argón")
                )
                "poner_temporizador" -> temporizador(
                    a.optInt("segundos", 0), a.optString("mensaje", "Argón")
                )
                "guardar_archivo" -> guardar(a.optString("nombre", ""), a.optString("contenido", ""))
                "ver_bateria" -> bateria()
                "ver_ubicacion" -> ubicacion()
                else -> "Herramienta desconocida: $nombre"
            }
        } catch (e: Exception) {
            "Error al ejecutar $nombre: ${e.message}"
        }
    }

    private fun permiso(perm: String): Boolean {
        if (act.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) return true
        act.runOnUiThread { act.requestPermissions(arrayOf(perm), 3) }
        return false
    }

    private fun lanzar(i: Intent) {
        act.runOnUiThread {
            try {
                act.startActivity(i)
            } catch (e: Exception) {
                Toast.makeText(act, "No pude abrir la app de Reloj", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun contactos(nombre: String): String {
        if (!permiso(Manifest.permission.READ_CONTACTS)) return falta
        val col = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        val buscar = nombre.isNotBlank()
        val sel = if (buscar) "$col LIKE ?" else null
        val args = if (buscar) arrayOf("%$nombre%") else null
        val sb = StringBuilder()
        var cont = 0
        act.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(col, ContactsContract.CommonDataKinds.Phone.NUMBER),
            sel, args, col
        )?.use { c ->
            while (c.moveToNext() && cont < 30) {
                sb.append(c.getString(0)).append(": ").append(c.getString(1)).append("\n")
                cont++
            }
        }
        return if (cont == 0) "No encontré contactos" else sb.toString()
    }

    private fun sms(cantidad: Int, buscar: String): String {
        if (!permiso(Manifest.permission.READ_SMS)) return falta
        val max = cantidad.coerceIn(1, 15)
        val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        var cont = 0
        act.contentResolver.query(
            Uri.parse("content://sms/inbox"),
            arrayOf("address", "body", "date"), null, null, "date DESC"
        )?.use { c ->
            while (c.moveToNext() && cont < max) {
                val de = c.getString(0) ?: ""
                val texto = c.getString(1) ?: ""
                if (buscar.isNotBlank() && !de.contains(buscar, true) && !texto.contains(buscar, true)) {
                    continue
                }
                sb.append(fmt.format(Date(c.getLong(2)))).append(" | ").append(de)
                    .append(": ").append(texto.take(300)).append("\n")
                cont++
            }
        }
        return if (cont == 0) "No encontré mensajes" else sb.toString()
    }

    private fun alarma(hora: Int, minutos: Int, mensaje: String): String {
        if (hora !in 0..23 || minutos !in 0..59) return "Hora inválida"
        val i = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, hora)
            .putExtra(AlarmClock.EXTRA_MINUTES, minutos)
            .putExtra(AlarmClock.EXTRA_MESSAGE, mensaje)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        lanzar(i)
        return "Pedí al reloj una alarma a las %02d:%02d. Conviene revisar la app de Reloj.".format(hora, minutos)
    }

    private fun temporizador(segundos: Int, mensaje: String): String {
        if (segundos !in 1..86400) return "Duración inválida"
        val i = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, segundos)
            .putExtra(AlarmClock.EXTRA_MESSAGE, mensaje)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        lanzar(i)
        return "Pedí al reloj un temporizador de $segundos segundos."
    }

    private fun guardar(nombre: String, contenido: String): String {
        if (nombre.isBlank()) return "Falta el nombre del archivo"
        if (Build.VERSION.SDK_INT < 29) return "Este Android es muy viejo para guardar en Descargas"
        val limpio = nombre.replace(Regex("[^A-Za-z0-9._ -]"), "_")
        val v = ContentValues()
        v.put(MediaStore.Downloads.DISPLAY_NAME, limpio)
        v.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
        v.put(MediaStore.Downloads.RELATIVE_PATH, "Download/Argon")
        val uri = act.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
            ?: return "No pude crear el archivo"
        act.contentResolver.openOutputStream(uri)?.use {
            it.write(contenido.toByteArray(Charsets.UTF_8))
        }
        return "Guardado en Descargas/Argon/$limpio"
    }

    private fun bateria(): String {
        val i = act.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return "No pude leer la batería"
        val nivel = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val max = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val carga = st == BatteryManager.BATTERY_STATUS_CHARGING ||
            st == BatteryManager.BATTERY_STATUS_FULL
        return "Batería: ${nivel * 100 / max}%, " + (if (carga) "cargando" else "sin cargar")
    }

    private fun ubicacion(): String {
        val fino = act.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val grueso = act.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!fino && !grueso) {
            act.runOnUiThread {
                act.requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), 4
                )
            }
            return falta
        }
        val lm = act.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var mejor: Location? = null
        for (p in listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )) {
            try {
                val l = lm.getLastKnownLocation(p)
                if (l != null && (mejor == null || l.time > mejor.time)) mejor = l
            } catch (e: Exception) {
            }
        }
        val m = mejor
            ?: return "No tengo una ubicación reciente. Abre un mapa un momento y vuelve a pedirla."
        val min = (System.currentTimeMillis() - m.time) / 60000
        return "Latitud ${m.latitude}, longitud ${m.longitude}, precisión ${m.accuracy.toInt()} m, " +
            "hace $min min. Mapa: https://maps.google.com/?q=${m.latitude},${m.longitude}"
    }
}
