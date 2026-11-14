package com.argon.app

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Extras(private val act: Activity) {

    private val horaFmt = SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", Locale("es"))

    private fun tStr(desc: String) = JSONObject().put("type", "string").put("description", desc)
    private fun tInt(desc: String) = JSONObject().put("type", "integer").put("description", desc)

    private fun pr(vararg c: Pair<String, JSONObject>): JSONObject {
        val o = JSONObject()
        for ((k, v) in c) o.put(k, v)
        return o
    }

    private fun decl(nombre: String, desc: String, props: JSONObject?, req: List<String>): JSONObject {
        val d = JSONObject().put("name", nombre).put("description", desc)
        if (props != null) {
            val p = JSONObject().put("type", "object").put("properties", props)
            if (req.isNotEmpty()) p.put("required", JSONArray(req))
            d.put("parameters", p)
        }
        return d
    }

    fun declaraciones(): JSONArray {
        val a = JSONArray()
        a.put(decl("youtube_buscar", "Busca algo en YouTube y abre los resultados.",
            pr("busqueda" to tStr("Qué buscar")), listOf("busqueda")))
        a.put(decl("spotify_buscar", "Busca música en Spotify y abre los resultados.",
            pr("busqueda" to tStr("Canción, artista o álbum")), listOf("busqueda")))
        a.put(decl("telegram_enviar", "Abre Telegram para compartir un texto; el usuario elige el chat y envía.",
            pr("texto" to tStr("Texto a compartir")), listOf("texto")))
        a.put(decl("play_store_buscar", "Busca una app en la tienda de aplicaciones.",
            pr("app" to tStr("Nombre de la app")), listOf("app")))
        a.put(decl("buscar_en_navegador", "Abre una búsqueda de Google en el navegador para que el usuario la vea.",
            pr("consulta" to tStr("Qué buscar")), listOf("consulta")))
        a.put(decl("abrir_camara", "Abre la app de cámara.", null, emptyList()))
        a.put(decl("abrir_galeria", "Abre la galería de fotos.", null, emptyList()))
        a.put(decl("abrir_calculadora", "Abre la calculadora del teléfono.", null, emptyList()))
        a.put(decl("ajustes_de_app", "Abre los ajustes de una app instalada (permisos, almacenamiento).",
            pr("nombre" to tStr("Nombre de la app")), listOf("nombre")))
        a.put(decl("compartir_en_app", "Comparte un texto directamente en una app instalada, por su nombre.",
            pr("app" to tStr("Nombre de la app"), "texto" to tStr("Texto a compartir")), listOf("app", "texto")))
        a.put(decl("rutina_agregar_paso", "Agrega un paso al final de una rutina guardada.",
            pr("nombre" to tStr("Nombre de la rutina"), "paso" to tStr("Paso a agregar")), listOf("nombre", "paso")))
        a.put(decl("rutina_quitar_paso", "Quita un paso de una rutina por su número (empieza en 1).",
            pr("nombre" to tStr("Nombre de la rutina"), "numero" to tInt("Número del paso")), listOf("nombre", "numero")))
        a.put(decl("rutina_renombrar", "Cambia el nombre de una rutina.",
            pr("nombre" to tStr("Nombre actual"), "nuevo" to tStr("Nombre nuevo")), listOf("nombre", "nuevo")))
        a.put(decl("rutina_ejecutar",
            "Prepara una rutina para ejecutarla: devuelve sus pasos con las variables ya reemplazadas. Después haz cada paso con tus herramientas. Las variables en los pasos se escriben {así}.",
            pr("nombre" to tStr("Nombre de la rutina"),
                "variables" to tStr("Valores, por ejemplo: ciudad=Lima; hora=7 (opcional)")),
            listOf("nombre")))
        a.put(decl("rutina_programar",
            "Programa una rutina para que a cierta hora llegue una notificación que la ejecuta con un toque.",
            pr("nombre" to tStr("Nombre de la rutina"), "hora" to tInt("Hora de 0 a 23"),
                "minutos" to tInt("Minutos de 0 a 59"),
                "dias" to tStr("Días, por ejemplo: lunes, miércoles, viernes. Vacío o 'todos' = todos los días")),
            listOf("nombre", "hora", "minutos")))
        a.put(decl("rutina_programaciones", "Lista las rutinas programadas y su próxima hora.", null, emptyList()))
        a.put(decl("rutina_quitar_programacion", "Quita la programación de una rutina (la rutina se conserva).",
            pr("nombre" to tStr("Nombre de la rutina")), listOf("nombre")))
        a.put(decl("vista_previa",
            "Abre una vista previa de una página HTML de un proyecto, con su CSS y JavaScript. No carga recursos de internet.",
            pr("proyecto" to tStr("Nombre del proyecto"), "archivo" to tStr("Archivo HTML, por defecto index.html")),
            listOf("proyecto")))
        return a
    }

    fun ejecutar(nombre: String, a: JSONObject): String? {
        return try {
            when (nombre) {
                "youtube_buscar" -> youtube(a.optString("busqueda", ""))
                "spotify_buscar" -> spotify(a.optString("busqueda", ""))
                "telegram_enviar" -> telegram(a.optString("texto", ""))
                "play_store_buscar" -> tienda(a.optString("app", ""))
                "buscar_en_navegador" -> navegador(a.optString("consulta", ""))
                "abrir_camara" -> camara()
                "abrir_galeria" -> galeria()
                "abrir_calculadora" -> calculadora()
                "ajustes_de_app" -> ajustesApp(a.optString("nombre", ""))
                "compartir_en_app" -> compartirEnApp(a.optString("app", ""), a.optString("texto", ""))
                "rutina_agregar_paso" -> agregarPaso(a.optString("nombre", ""), a.optString("paso", ""))
                "rutina_quitar_paso" -> quitarPaso(a.optString("nombre", ""), a.optInt("numero", 0))
                "rutina_renombrar" -> renombrar(a.optString("nombre", ""), a.optString("nuevo", ""))
                "rutina_ejecutar" -> ejecutarRutina(a.optString("nombre", ""), a.optString("variables", ""))
                "rutina_programar" -> programar(
                    a.optString("nombre", ""), a.optInt("hora", -1), a.optInt("minutos", 0), a.optString("dias", "")
                )
                "rutina_programaciones" -> programaciones()
                "rutina_quitar_programacion" -> quitarProgramacion(a.optString("nombre", ""))
                "vista_previa" -> vistaPrevia(a.optString("proyecto", ""), a.optString("archivo", ""))
                else -> null
            }
        } catch (e: Throwable) {
            "Error en $nombre: ${e.message}"
        }
    }

    // ---------- Utilidades ----------

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    private fun lanzar(i: Intent, error: String) {
        act.runOnUiThread {
            try {
                act.startActivity(i)
            } catch (e: Exception) {
                Toast.makeText(act, error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun lanzarConAlternativa(i1: Intent, i2: Intent, error: String) {
        act.runOnUiThread {
            try {
                act.startActivity(i1)
            } catch (e: Exception) {
                try {
                    act.startActivity(i2)
                } catch (e2: Exception) {
                    Toast.makeText(act, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun buscarApp(nombre: String): Pair<String, String>? {
        val pm = act.packageManager
        val filtro = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val buscado = nombre.lowercase().trim()
        for (ri in pm.queryIntentActivities(filtro, 0)) {
            val etiqueta = ri.loadLabel(pm).toString()
            if (etiqueta.lowercase().contains(buscado)) return Pair(ri.activityInfo.packageName, etiqueta)
        }
        return null
    }

    // ---------- Integraciones ----------

    private fun youtube(q: String): String {
        if (q.isBlank()) return "Falta qué buscar"
        lanzar(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + enc(q))),
            "No pude abrir YouTube"
        )
        return "Abrí YouTube con la búsqueda \"$q\"."
    }

    private fun spotify(q: String): String {
        if (q.isBlank()) return "Falta qué buscar"
        lanzarConAlternativa(
            Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:" + enc(q))),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/" + enc(q))),
            "No pude abrir Spotify"
        )
        return "Abrí Spotify con la búsqueda \"$q\"."
    }

    private fun telegram(texto: String): String {
        if (texto.isBlank()) return "Falta el texto"
        lanzar(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/share/url?url=&text=" + enc(texto))),
            "No pude abrir Telegram"
        )
        return "Abrí Telegram para compartir el texto. El usuario elige el chat y envía."
    }

    private fun tienda(app: String): String {
        if (app.isBlank()) return "Falta el nombre de la app"
        lanzarConAlternativa(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + enc(app) + "&c=apps")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=" + enc(app) + "&c=apps")),
            "No pude abrir la tienda"
        )
        return "Abrí la tienda con la búsqueda \"$app\"."
    }

    private fun navegador(q: String): String {
        if (q.isBlank()) return "Falta qué buscar"
        lanzar(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + enc(q))),
            "No pude abrir el navegador"
        )
        return "Abrí la búsqueda en el navegador."
    }

    private fun camara(): String {
        lanzar(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), "No encontré una app de cámara")
        return "Abrí la cámara."
    }

    private fun galeria(): String {
        lanzar(
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_GALLERY),
            "No encontré la galería"
        )
        return "Abrí la galería."
    }

    private fun calculadora(): String {
        lanzar(
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR),
            "No encontré la calculadora"
        )
        return "Abrí la calculadora."
    }

    private fun ajustesApp(nombre: String): String {
        if (nombre.isBlank()) return "Falta el nombre de la app"
        val app = buscarApp(nombre) ?: return "No encontré una app llamada $nombre"
        lanzar(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.first)),
            "No pude abrir los ajustes"
        )
        return "Abrí los ajustes de ${app.second}."
    }

    private fun compartirEnApp(nombre: String, texto: String): String {
        if (nombre.isBlank() || texto.isBlank()) return "Faltan la app o el texto"
        val app = buscarApp(nombre) ?: return "No encontré una app llamada $nombre"
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, texto)
        i.setPackage(app.first)
        lanzar(i, "Esa app no permite compartir texto")
        return "Abrí ${app.second} para compartir el texto. El usuario debe confirmar el envío."
    }

    // ---------- Vista previa ----------

    private fun vistaPrevia(proyecto: String, archivo: String): String {
        if (proyecto.isBlank()) return "Falta el nombre del proyecto"
        val i = Intent(act, VistaPreviaActivity::class.java)
        i.putExtra("proyecto", proyecto.trim())
        i.putExtra("archivo", if (archivo.isBlank()) "index.html" else archivo.trim())
        lanzar(i, "No pude abrir la vista previa")
        return "Abrí la vista previa de $proyecto. El usuario la cierra con el botón de arriba."
    }

    // ---------- Rutinas avanzadas ----------

    private fun rutinas(): JSONObject =
        try {
            JSONObject(File(act.filesDir, "rutinas.json").readText())
        } catch (e: Exception) {
            JSONObject()
        }

    private fun guardarRutinas(r: JSONObject) {
        File(act.filesDir, "rutinas.json").writeText(r.toString())
    }

    private fun nombreRutina(r: JSONObject, nombre: String): String? {
        val ks = r.keys()
        while (ks.hasNext()) {
            val k = ks.next()
            if (k.equals(nombre.trim(), true)) return k
        }
        return null
    }

    private fun agregarPaso(nombre: String, paso: String): String {
        if (paso.isBlank()) return "Falta el paso"
        val r = rutinas()
        val clave = nombreRutina(r, nombre) ?: return "No encontré esa rutina"
        val actual = r.optString(clave)
        r.put(clave, if (actual.isBlank()) paso.trim() else actual.trimEnd() + "\n" + paso.trim())
        guardarRutinas(r)
        return "Paso agregado a la rutina \"$clave\"."
    }

    private fun quitarPaso(nombre: String, numero: Int): String {
        val r = rutinas()
        val clave = nombreRutina(r, nombre) ?: return "No encontré esa rutina"
        val pasos = r.optString(clave).split("\n").filter { it.isNotBlank() }.toMutableList()
        if (numero !in 1..pasos.size) return "Esa rutina tiene ${pasos.size} pasos, el número no es válido"
        val quitado = pasos.removeAt(numero - 1)
        r.put(clave, pasos.joinToString("\n"))
        guardarRutinas(r)
        return "Quité el paso $numero (\"${quitado.trim()}\")."
    }

    private fun renombrar(nombre: String, nuevo: String): String {
        if (nuevo.isBlank()) return "Falta el nombre nuevo"
        val r = rutinas()
        val clave = nombreRutina(r, nombre) ?: return "No encontré esa rutina"
        r.put(nuevo.trim(), r.optString(clave))
        if (!clave.equals(nuevo.trim(), true)) r.remove(clave)
        guardarRutinas(r)
        Programador.renombrar(act, clave, nuevo.trim())
        return "Rutina renombrada a \"${nuevo.trim()}\"."
    }

    private fun ejecutarRutina(nombre: String, variables: String): String {
        val r = rutinas()
        val clave = nombreRutina(r, nombre) ?: return "No encontré esa rutina"
        var pasos = r.optString(clave)
        for (par in variables.split(";", "\n")) {
            val i = par.indexOf('=')
            if (i > 0) {
                val k = par.substring(0, i).trim()
                val v = par.substring(i + 1).trim()
                if (k.isNotEmpty()) pasos = pasos.replace("{" + k + "}", v)
            }
        }
        val faltan = Regex("\\{([^}]+)\\}").findAll(pasos).map { it.groupValues[1] }.toSet()
        val numerados = pasos.split("\n").filter { it.isNotBlank() }
            .mapIndexed { i, p -> "${i + 1}. ${p.trim()}" }.joinToString("\n")
        return "Rutina \"$clave\". Haz estos pasos en orden con tus herramientas y cuéntame el resultado:\n" +
            numerados +
            (if (faltan.isEmpty()) "" else "\nFaltan valores para: " + faltan.joinToString(", ") + ". Pregúntaselos al usuario.")
    }

    private fun parsearDias(s: String): Set<Int> {
        val t = s.lowercase()
        if (t.isBlank() || t.contains("todos") || t.contains("diari")) return emptySet()
        val mapa = mapOf(
            "dom" to Calendar.SUNDAY, "lun" to Calendar.MONDAY, "mar" to Calendar.TUESDAY,
            "mie" to Calendar.WEDNESDAY, "mié" to Calendar.WEDNESDAY, "jue" to Calendar.THURSDAY,
            "vie" to Calendar.FRIDAY, "sab" to Calendar.SATURDAY, "sáb" to Calendar.SATURDAY
        )
        val out = HashSet<Int>()
        for ((k, v) in mapa) if (t.contains(k)) out.add(v)
        return out
    }

    private fun programar(nombre: String, hora: Int, minutos: Int, dias: String): String {
        val r = rutinas()
        val clave = nombreRutina(r, nombre) ?: return "No encontré esa rutina. Guárdala primero."
        if (hora !in 0..23 || minutos !in 0..59) return "Hora inválida"
        val ds = parsearDias(dias)
        Programador.agregar(act, clave, hora, minutos, ds)
        val cuando = Programador.proximo(hora, minutos, ds)
        return "Rutina \"$clave\" programada a las %02d:%02d (%s). Próxima vez: %s. A esa hora llegará una notificación para ejecutarla con un toque.".format(
            hora, minutos, if (ds.isEmpty()) "todos los días" else "días elegidos", horaFmt.format(Date(cuando))
        )
    }

    private fun programaciones(): String {
        val a = Programador.cargar(act)
        if (a.length() == 0) return "No hay rutinas programadas"
        val sb = StringBuilder()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            val ds = HashSet<Int>()
            val arr = o.optJSONArray("d")
            if (arr != null) for (k in 0 until arr.length()) ds.add(arr.getInt(k))
            val cuando = Programador.proximo(o.getInt("h"), o.getInt("m"), ds)
            sb.append(o.optString("n")).append(": ")
                .append("%02d:%02d".format(o.getInt("h"), o.getInt("m")))
                .append(", próxima vez ").append(horaFmt.format(Date(cuando))).append("\n")
        }
        return sb.toString()
    }

    private fun quitarProgramacion(nombre: String): String {
        val r = rutinas()
        val clave = nombreRutina(r, nombre) ?: nombre.trim()
        return if (Programador.quitar(act, clave)) "Programación de \"$clave\" quitada." else "Esa rutina no estaba programada."
    }
}
