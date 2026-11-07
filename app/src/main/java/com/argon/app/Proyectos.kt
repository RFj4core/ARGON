package com.argon.app

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class Proyectos(private val act: Activity) {

    private val raiz = "Download/Argon/proyectos/"

    private fun tStr(desc: String) = JSONObject().put("type", "string").put("description", desc)

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
        val proy = tStr("Nombre del proyecto, por ejemplo calculadora")
        val ruta = tStr("Ruta del archivo dentro del proyecto, por ejemplo src/main.js")
        a.put(decl("proyecto_listar",
            "Sin nombre, lista los proyectos. Con nombre, lista los archivos de ese proyecto.",
            pr("proyecto" to tStr("Nombre del proyecto (opcional)")), emptyList()))
        a.put(decl("proyecto_leer", "Lee un archivo de un proyecto.",
            pr("proyecto" to proy, "ruta" to ruta), listOf("proyecto", "ruta")))
        a.put(decl("proyecto_guardar",
            "Crea o actualiza un archivo de un proyecto (los proyectos pueden tener varios archivos y carpetas). Antes de sobrescribir, se guarda una copia exacta de la versión anterior.",
            pr("proyecto" to proy, "ruta" to ruta, "contenido" to tStr("Contenido completo del archivo")),
            listOf("proyecto", "ruta", "contenido")))
        a.put(decl("proyecto_borrar", "Borra un archivo de un proyecto (antes se guarda una copia).",
            pr("proyecto" to proy, "ruta" to ruta), listOf("proyecto", "ruta")))
        a.put(decl("proyecto_versiones", "Lista las versiones anteriores guardadas de un archivo.",
            pr("proyecto" to proy, "ruta" to ruta), listOf("proyecto", "ruta")))
        a.put(decl("proyecto_restaurar", "Restaura una versión anterior de un archivo (antes guarda la actual).",
            pr("proyecto" to proy, "ruta" to ruta, "version" to tStr("Versión tal como la da proyecto_versiones")),
            listOf("proyecto", "ruta", "version")))
        a.put(decl("proyecto_zip", "Empaqueta el proyecto en un .zip en Descargas/Argon.",
            pr("proyecto" to proy), listOf("proyecto")))
        return a
    }

    fun ejecutar(nombre: String, a: JSONObject): String? {
        if (!nombre.startsWith("proyecto_")) return null
        if (Build.VERSION.SDK_INT < 29) return "Este Android es muy viejo para los proyectos"
        return try {
            when (nombre) {
                "proyecto_listar" -> listar(a.optString("proyecto", ""))
                "proyecto_leer" -> leer(a.optString("proyecto", ""), a.optString("ruta", ""))
                "proyecto_guardar" -> guardar(
                    a.optString("proyecto", ""), a.optString("ruta", ""), a.optString("contenido", "")
                )
                "proyecto_borrar" -> borrar(a.optString("proyecto", ""), a.optString("ruta", ""))
                "proyecto_versiones" -> versiones(a.optString("proyecto", ""), a.optString("ruta", ""))
                "proyecto_restaurar" -> restaurar(
                    a.optString("proyecto", ""), a.optString("ruta", ""), a.optString("version", "")
                )
                "proyecto_zip" -> zip(a.optString("proyecto", ""))
                else -> null
            }
        } catch (e: Exception) {
            "Error en $nombre: ${e.message}"
        }
    }

    // ---------- Utilidades ----------

    private fun limpioSeg(s: String): String = s.trim().replace(Regex("[^A-Za-z0-9._ -]"), "_")

    private fun partes(ruta: String): List<String>? {
        val segs = ruta.replace("\\", "/").split("/").map { it.trim() }.filter { it.isNotEmpty() }
        if (segs.isEmpty() || segs.any { it == ".." || it == "." }) return null
        return segs.map { limpioSeg(it) }
    }

    private fun relPath(proyecto: String, subdirs: List<String>, respaldo: Boolean): String {
        val sb = StringBuilder(raiz).append(proyecto).append("/")
        if (respaldo) sb.append("_respaldos/")
        for (d in subdirs) sb.append(d).append("/")
        return sb.toString()
    }

    private fun marca(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())

    private fun buscar(rel: String, nombre: String): Uri? {
        act.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads._ID),
            MediaStore.Downloads.RELATIVE_PATH + " = ? AND " + MediaStore.Downloads.DISPLAY_NAME + " = ?",
            arrayOf(rel, nombre), null
        )?.use { c ->
            if (c.moveToFirst()) {
                return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0))
            }
        }
        return null
    }

    private fun leerBytes(uri: Uri): ByteArray? =
        act.contentResolver.openInputStream(uri)?.use { it.readBytes() }

    private fun escribir(rel: String, nombre: String, bytes: ByteArray): Boolean {
        val existente = buscar(rel, nombre)
        if (existente != null) {
            val s = act.contentResolver.openOutputStream(existente, "wt") ?: return false
            s.use { it.write(bytes) }
            return true
        }
        val v = ContentValues()
        v.put(MediaStore.Downloads.DISPLAY_NAME, nombre)
        v.put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
        v.put(MediaStore.Downloads.RELATIVE_PATH, rel)
        val uri = act.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v) ?: return false
        val s = act.contentResolver.openOutputStream(uri) ?: return false
        s.use { it.write(bytes) }
        return true
    }

    // ---------- Herramientas ----------

    private fun guardarBytes(p: String, segs: List<String>, bytes: ByteArray, verbo: String): String {
        val nombre = segs.last()
        val subs = segs.dropLast(1)
        val rel = relPath(p, subs, false)
        val existente = buscar(rel, nombre)
        var aviso = ""
        if (existente != null) {
            val ant = leerBytes(existente)
            if (ant != null) {
                val ts = marca()
                if (!escribir(relPath(p, subs, true), "$nombre.$ts.bak", ant)) {
                    return "No pude guardar el respaldo, así que no toqué el archivo."
                }
                aviso = " Copia exacta de la versión anterior en $p/_respaldos (versión $ts)."
            }
        }
        if (!escribir(rel, nombre, bytes)) return "No pude guardar el archivo"
        val que = if (existente != null) "Archivo actualizado" else verbo
        return que + ": Descargas/Argon/proyectos/" + p + "/" + segs.joinToString("/") + "." + aviso
    }

    private fun guardar(proyecto: String, ruta: String, contenido: String): String {
        val p = limpioSeg(proyecto)
        if (p.isEmpty()) return "Falta el nombre del proyecto"
        val segs = partes(ruta) ?: return "Ruta inválida"
        if (contenido.length > 300000) return "El archivo es demasiado grande (máximo 300000 caracteres)"
        return guardarBytes(p, segs, contenido.toByteArray(Charsets.UTF_8), "Archivo creado")
    }

    private fun leer(proyecto: String, ruta: String): String {
        val p = limpioSeg(proyecto)
        val segs = partes(ruta) ?: return "Ruta inválida"
        val uri = buscar(relPath(p, segs.dropLast(1), false), segs.last())
            ?: return "No encontré ese archivo en el proyecto"
        val bytes = leerBytes(uri) ?: return "No pude abrir el archivo"
        return String(bytes, Charsets.UTF_8).take(30000)
    }

    private fun borrar(proyecto: String, ruta: String): String {
        val p = limpioSeg(proyecto)
        val segs = partes(ruta) ?: return "Ruta inválida"
        val nombre = segs.last()
        val subs = segs.dropLast(1)
        val uri = buscar(relPath(p, subs, false), nombre) ?: return "No encontré ese archivo en el proyecto"
        val ant = leerBytes(uri) ?: return "No pude leer el archivo para respaldarlo"
        val ts = marca()
        if (!escribir(relPath(p, subs, true), "$nombre.$ts.bak", ant)) {
            return "No pude guardar el respaldo, así que no borré el archivo."
        }
        act.contentResolver.delete(uri, null, null)
        return "Archivo borrado. Quedó una copia exacta en $p/_respaldos (versión $ts)."
    }

    private fun listar(proyecto: String): String {
        val p = limpioSeg(proyecto)
        val vistos = LinkedHashSet<String>()
        act.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Downloads.RELATIVE_PATH, MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE
            ),
            MediaStore.Downloads.RELATIVE_PATH + " LIKE ?",
            arrayOf(raiz + (if (p.isEmpty()) "" else "$p/") + "%"),
            MediaStore.Downloads.RELATIVE_PATH + " ASC, " + MediaStore.Downloads.DISPLAY_NAME + " ASC"
        )?.use { c ->
            while (c.moveToNext() && vistos.size < 200) {
                val rel = c.getString(0) ?: continue
                val nombre = c.getString(1) ?: continue
                val resto = rel.removePrefix(raiz)
                if (p.isEmpty()) {
                    vistos.add(resto.substringBefore("/"))
                } else {
                    val dentro = resto.removePrefix("$p/")
                    if (dentro.startsWith("_respaldos/")) continue
                    vistos.add(dentro + nombre + " (" + c.getLong(2) / 1024 + " KB)")
                }
            }
        }
        if (vistos.isEmpty()) {
            return if (p.isEmpty()) "No hay proyectos todavía" else "El proyecto está vacío o no existe"
        }
        return vistos.joinToString("\n")
    }

    private fun versiones(proyecto: String, ruta: String): String {
        val p = limpioSeg(proyecto)
        val segs = partes(ruta) ?: return "Ruta inválida"
        val nombre = segs.last()
        val rel = relPath(p, segs.dropLast(1), true)
        val lista = ArrayList<String>()
        act.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Downloads.DISPLAY_NAME, MediaStore.Downloads.SIZE),
            MediaStore.Downloads.RELATIVE_PATH + " = ?", arrayOf(rel),
            MediaStore.Downloads.DISPLAY_NAME + " DESC"
        )?.use { c ->
            while (c.moveToNext()) {
                val n = c.getString(0) ?: continue
                if (n.startsWith("$nombre.") && n.endsWith(".bak")) {
                    lista.add(n.removePrefix("$nombre.").removeSuffix(".bak") + " (" + c.getLong(1) / 1024 + " KB)")
                }
            }
        }
        return if (lista.isEmpty()) "No hay versiones anteriores de ese archivo"
        else "Versiones (la primera es la más reciente):\n" + lista.joinToString("\n")
    }

    private fun restaurar(proyecto: String, ruta: String, version: String): String {
        val p = limpioSeg(proyecto)
        val segs = partes(ruta) ?: return "Ruta inválida"
        val v = version.trim().substringBefore(" ")
        if (v.isEmpty()) return "Falta la versión"
        val nombre = segs.last()
        val rel = relPath(p, segs.dropLast(1), true)
        val uri = buscar(rel, "$nombre.$v.bak") ?: return "No encontré esa versión"
        val bytes = leerBytes(uri) ?: return "No pude leer esa versión"
        return guardarBytes(p, segs, bytes, "Archivo restaurado")
    }

    private fun zip(proyecto: String): String {
        val p = limpioSeg(proyecto)
        if (p.isEmpty()) return "Falta el nombre del proyecto"
        val salida = ByteArrayOutputStream()
        var cont = 0
        ZipOutputStream(salida).use { z ->
            act.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(
                    MediaStore.Downloads._ID, MediaStore.Downloads.RELATIVE_PATH,
                    MediaStore.Downloads.DISPLAY_NAME
                ),
                MediaStore.Downloads.RELATIVE_PATH + " LIKE ?", arrayOf(raiz + "$p/%"), null
            )?.use { c ->
                while (c.moveToNext() && cont < 300) {
                    val rel = c.getString(1) ?: continue
                    val nombre = c.getString(2) ?: continue
                    val dentro = rel.removePrefix(raiz).removePrefix("$p/")
                    if (dentro.startsWith("_respaldos/")) continue
                    val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, c.getLong(0))
                    val bytes = leerBytes(uri) ?: continue
                    z.putNextEntry(ZipEntry(p + "/" + dentro + nombre))
                    z.write(bytes)
                    z.closeEntry()
                    cont++
                }
            }
        }
        if (cont == 0) return "El proyecto está vacío o no existe"
        val datos = salida.toByteArray()
        if (datos.size > 20 * 1024 * 1024) return "El proyecto pesa demasiado para empaquetarlo"
        if (!escribir("Download/Argon/", "$p.zip", datos)) return "No pude crear el .zip"
        return "Creé Descargas/Argon/$p.zip con $cont archivos."
    }
}
