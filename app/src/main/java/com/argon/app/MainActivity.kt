package com.argon.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val BASE = "gemini-flash-latest"
private const val RAPIDO = "gemini-flash-lite-latest"
private const val CANAL = "argon_respuestas"
private const val SISTEMA =
    "Eres Argón, el asistente personal de rafachafa, así lo llamas siempre. " +
    "Hablas español. Eres directo, serio y de pocas palabras: respuestas cortas y simples, " +
    "sin saludos exagerados, sin emojis y sin entusiasmo de más. " +
    "Eres un programador experto cuando te piden código. " +
    "Si rafachafa te pide que recuerdes algo, o te cuenta un dato personal duradero, " +
    "agrega al final de tu respuesta, en una línea nueva, [[RECORDAR: dato corto]]. " +
    "Guarda solo lo que él dijo, nunca inventes, y no guardes contraseñas, claves ni datos bancarios. " +
    "No menciones este mecanismo."

private val EXT_TEXTO = setOf(
    "txt", "md", "py", "kt", "java", "js", "ts", "tsx", "jsx", "json", "csv", "html", "css",
    "xml", "yml", "yaml", "sh", "c", "cpp", "h", "sql", "gradle", "kts", "properties",
    "log", "ini", "toml", "php", "rb", "go", "rs", "swift", "bat"
)

class MainActivity : Activity() {

    private class Resultado(val texto: String?, val error: String?, val modelo: String?)
    private class Adjunto(val nombre: String, val partes: List<JSONObject>)
    private class ResAdjunto(val adjunto: Adjunto?, val error: String?)

    private lateinit var prefs: SharedPreferences
    private lateinit var chat: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var entrada: EditText
    private lateinit var enviar: Button
    private lateinit var btnBase: Button
    private lateinit var btnRapido: Button
    private lateinit var descripcion: TextView
    private lateinit var cargando: LinearLayout
    private lateinit var adjuntoTxt: TextView

    private var historial = JSONArray()
    private var chatId = ""
    private var tituloChat = ""
    private var adjunto: Adjunto? = null
    private var modo = "base"
    private var enSegundoPlano = false
    private var ocupado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("argon", MODE_PRIVATE)
        modo = prefs.getString("modo", "base") ?: "base"

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Respuestas de Argón", NotificationManager.IMPORTANCE_DEFAULT)
        )
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        setContentView(construirUi())
        actualizarModo()

        val ultimo = prefs.getString("ultimo", "") ?: ""
        if (ultimo.isNotEmpty() && File(File(filesDir, "chats"), "$ultimo.json").exists()) {
            cargarChat(ultimo)
        } else {
            agregarBurbuja("Listo, rafachafa.", false)
        }
        if ((prefs.getString("clave", "") ?: "").isEmpty()) pedirClave()
    }

    override fun onPause() {
        super.onPause()
        enSegundoPlano = true
    }

    override fun onResume() {
        super.onResume()
        enSegundoPlano = false
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    private fun boton(texto: String, accion: () -> Unit): Button {
        val b = Button(this)
        b.text = texto
        b.textSize = 13f
        b.transformationMethod = null
        b.minWidth = 0
        b.minimumWidth = 0
        b.setOnClickListener { accion() }
        return b
    }

    private fun construirUi(): View {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#101418"))
        root.setOnApplyWindowInsetsListener { v, ins ->
            v.setPadding(
                ins.systemWindowInsetLeft, ins.systemWindowInsetTop,
                ins.systemWindowInsetRight, ins.systemWindowInsetBottom
            )
            ins
        }

        val fila1 = LinearLayout(this)
        fila1.orientation = LinearLayout.HORIZONTAL
        fila1.gravity = Gravity.CENTER_VERTICAL
        fila1.setPadding(dp(12), dp(8), dp(8), 0)
        val titulo = TextView(this)
        titulo.text = "Argón"
        titulo.textSize = 22f
        titulo.setTypeface(null, Typeface.BOLD)
        titulo.setTextColor(Color.WHITE)
        fila1.addView(titulo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        fila1.addView(boton("Chats") { abrirChats() })
        fila1.addView(boton("Memoria") { abrirMemoria() })
        fila1.addView(boton("Clave") { pedirClave() })
        root.addView(fila1)

        val fila2 = LinearLayout(this)
        fila2.orientation = LinearLayout.HORIZONTAL
        fila2.setPadding(dp(8), dp(4), dp(8), 0)
        btnBase = boton("Argón base") { modo = "base"; actualizarModo() }
        btnRapido = boton("Argón rápido") { modo = "rapido"; actualizarModo() }
        fila2.addView(btnBase, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        fila2.addView(btnRapido, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(fila2)

        descripcion = TextView(this)
        descripcion.textSize = 12f
        descripcion.setTextColor(Color.parseColor("#8A94A0"))
        descripcion.setPadding(dp(14), dp(2), dp(14), dp(4))
        root.addView(descripcion)

        chat = LinearLayout(this)
        chat.orientation = LinearLayout.VERTICAL
        chat.setPadding(dp(8), dp(8), dp(8), dp(8))
        scroll = ScrollView(this)
        scroll.addView(chat)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        cargando = LinearLayout(this)
        cargando.orientation = LinearLayout.HORIZONTAL
        cargando.gravity = Gravity.CENTER_VERTICAL
        cargando.setPadding(dp(14), dp(4), dp(14), dp(4))
        cargando.visibility = View.GONE
        cargando.addView(ProgressBar(this), LinearLayout.LayoutParams(dp(24), dp(24)))
        val txt = TextView(this)
        txt.text = "  Argón pensando..."
        txt.setTextColor(Color.parseColor("#8A94A0"))
        cargando.addView(txt)
        root.addView(cargando)

        adjuntoTxt = TextView(this)
        adjuntoTxt.textSize = 13f
        adjuntoTxt.setTextColor(Color.parseColor("#8AB4F8"))
        adjuntoTxt.setPadding(dp(14), dp(4), dp(14), dp(4))
        adjuntoTxt.visibility = View.GONE
        adjuntoTxt.setOnClickListener {
            adjunto = null
            adjuntoTxt.visibility = View.GONE
        }
        root.addView(adjuntoTxt)

        val fila3 = LinearLayout(this)
        fila3.orientation = LinearLayout.HORIZONTAL
        fila3.gravity = Gravity.CENTER_VERTICAL
        fila3.setPadding(dp(8), dp(4), dp(8), dp(8))
        val btnMas = boton("+") { adjuntar() }
        btnMas.textSize = 20f
        entrada = EditText(this)
        entrada.hint = "Escríbele a Argón..."
        entrada.setHintTextColor(Color.parseColor("#6B7480"))
        entrada.setTextColor(Color.WHITE)
        entrada.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        entrada.maxLines = 5
        enviar = boton("Enviar") { enviarMensaje() }
        enviar.textSize = 14f
        fila3.addView(btnMas)
        fila3.addView(entrada, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        fila3.addView(enviar)
        root.addView(fila3)

        return root
    }

    private fun actualizarModo() {
        prefs.edit().putString("modo", modo).apply()
        val activo = Color.parseColor("#3D7BEA")
        val inactivo = Color.parseColor("#2A2F36")
        btnBase.setBackgroundColor(if (modo == "base") activo else inactivo)
        btnRapido.setBackgroundColor(if (modo == "rapido") activo else inactivo)
        btnBase.setTextColor(Color.WHITE)
        btnRapido.setTextColor(Color.WHITE)
        descripcion.text = if (modo == "base")
            "Base: el modelo normal, mejor para programar y temas largos"
        else
            "Rápido: el modelo liviano, para charla y textos cortos"
    }

    private fun pedirClave() {
        val tieneClave = (prefs.getString("clave", "") ?: "").isNotEmpty()
        val campo = EditText(this)
        campo.hint = "Clave de Gemini"
        campo.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        val dialogo = AlertDialog.Builder(this)
            .setTitle("Clave de Gemini")
            .setMessage("Pégala aquí. Se guarda solo en tu teléfono.")
            .setView(campo)
            .setCancelable(tieneClave)
            .setPositiveButton("Guardar") { _, _ ->
                prefs.edit().putString("clave", campo.text.toString().trim()).apply()
            }
        if (tieneClave) dialogo.setNegativeButton("Cancelar", null)
        dialogo.show()
    }

    private fun abrirMemoria() {
        val campo = EditText(this)
        campo.setText(prefs.getString("memoria", "") ?: "")
        campo.hint = "Aquí se guarda lo que Argón recuerda de ti. Una línea por dato."
        campo.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        campo.minLines = 6
        campo.maxLines = 12
        campo.gravity = Gravity.TOP
        AlertDialog.Builder(this)
            .setTitle("Memoria de Argón")
            .setView(campo)
            .setPositiveButton("Guardar") { _, _ ->
                prefs.edit().putString("memoria", campo.text.toString().trim()).apply()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun sistema(): String {
        val mem = prefs.getString("memoria", "") ?: ""
        return if (mem.isNotBlank())
            SISTEMA + "\n\nLo que sabes de rafachafa (tu memoria, úsala solo cuando sea relevante):\n" + mem
        else SISTEMA
    }

    private fun procesarRecuerdos(texto: String): Pair<String, List<String>> {
        val re = Regex("\\[\\[RECORDAR:\\s*(.+?)\\]\\]", RegexOption.DOT_MATCHES_ALL)
        var mem = prefs.getString("memoria", "") ?: ""
        val nuevos = ArrayList<String>()
        for (m in re.findAll(texto)) {
            val dato = m.groupValues[1].trim().replace("\n", " ")
            if (dato.isNotEmpty() && !mem.contains(dato)) {
                mem += (if (mem.isEmpty()) "" else "\n") + "- " + dato
                nuevos.add(dato)
            }
        }
        while (mem.length > 6000 && mem.contains("\n")) mem = mem.substringAfter("\n")
        prefs.edit().putString("memoria", mem).apply()
        return Pair(re.replace(texto, "").trim(), nuevos)
    }

    private fun agregarBurbuja(texto: String, usuario: Boolean, error: Boolean = false) {
        val tv = TextView(this)
        tv.text = texto
        tv.textSize = 16f
        tv.setTextColor(Color.WHITE)
        tv.setTextIsSelectable(true)
        tv.setPadding(dp(12), dp(8), dp(12), dp(8))
        tv.maxWidth = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val fondo = GradientDrawable()
        fondo.cornerRadius = dp(14).toFloat()
        fondo.setColor(
            Color.parseColor(if (error) "#7A2E2E" else if (usuario) "#2F5FA8" else "#222831")
        )
        tv.background = fondo
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(dp(4), dp(4), dp(4), dp(4))
        lp.gravity = if (usuario) Gravity.END else Gravity.START
        tv.layoutParams = lp
        chat.addView(tv)
        scrollAbajo()
    }

    private fun agregarNota(texto: String) {
        val tv = TextView(this)
        tv.text = texto
        tv.textSize = 12f
        tv.setTextColor(Color.parseColor("#8A94A0"))
        tv.setPadding(dp(8), 0, dp(8), dp(4))
        chat.addView(tv)
        scrollAbajo()
    }

    private fun scrollAbajo() {
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    // ---------- Archivos ----------

    private fun adjuntar() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        startActivityForResult(i, 2)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 2 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        Thread {
            val r = leerAdjunto(uri)
            runOnUiThread {
                val a = r.adjunto
                if (a != null) {
                    adjunto = a
                    adjuntoTxt.text = "Adjunto: ${a.nombre} (toca para quitar)"
                    adjuntoTxt.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, r.error ?: "No se pudo leer el archivo", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun leerAdjunto(uri: Uri): ResAdjunto {
        try {
            var nombre = "archivo"
            var tam = -1L
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val i1 = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (i1 >= 0) nombre = c.getString(i1) ?: "archivo"
                    val i2 = c.getColumnIndex(OpenableColumns.SIZE)
                    if (i2 >= 0 && !c.isNull(i2)) tam = c.getLong(i2)
                }
            }
            if (tam > 10L * 1024 * 1024) return ResAdjunto(null, "El archivo pesa más de 10 MB")
            val mime = contentResolver.getType(uri) ?: ""
            val ext = nombre.substringAfterLast('.', "").lowercase()
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return ResAdjunto(null, "No pude abrir el archivo")
            if (bytes.size > 10 * 1024 * 1024) return ResAdjunto(null, "El archivo pesa más de 10 MB")

            val esTexto = ext in EXT_TEXTO || mime.startsWith("text/") ||
                mime == "application/json" || mime == "application/xml"
            if (esTexto) {
                var t = String(bytes, Charsets.UTF_8)
                var aviso = ""
                if (t.length > 150000) {
                    t = t.take(150000)
                    aviso = "\n[...archivo cortado por tamaño...]"
                }
                val parte = JSONObject().put("text", "Archivo adjunto: $nombre\n$t$aviso")
                return ResAdjunto(Adjunto(nombre, listOf(parte)), null)
            }

            val mimeFinal = when {
                mime.startsWith("image/") || mime.startsWith("audio/") || mime == "application/pdf" -> mime
                ext == "pdf" -> "application/pdf"
                else -> ""
            }
            if (mimeFinal.isEmpty()) {
                return ResAdjunto(
                    null, "Tipo de archivo no compatible. Sirven imágenes, PDF, audio y archivos de texto o código."
                )
            }
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val p1 = JSONObject().put("text", "Archivo adjunto: $nombre")
            val p2 = JSONObject().put(
                "inline_data", JSONObject().put("mime_type", mimeFinal).put("data", b64)
            )
            return ResAdjunto(Adjunto(nombre, listOf(p1, p2)), null)
        } catch (e: Exception) {
            return ResAdjunto(null, "Error al leer el archivo: ${e.message}")
        }
    }

    // ---------- Chats ----------

    private fun guardarChat() {
        if (historial.length() == 0) return
        if (chatId.isEmpty()) chatId = System.currentTimeMillis().toString()
        val dir = File(filesDir, "chats")
        dir.mkdirs()
        val limpio = JSONArray()
        for (i in 0 until historial.length()) {
            val m = historial.getJSONObject(i)
            val partes = m.getJSONArray("parts")
            val nuevas = JSONArray()
            for (k in 0 until partes.length()) {
                val p = partes.getJSONObject(k)
                if (p.has("inline_data")) {
                    nuevas.put(JSONObject().put("text", "[Contenido de ese archivo ya no disponible en este chat]"))
                } else {
                    nuevas.put(p)
                }
            }
            limpio.put(JSONObject().put("role", m.getString("role")).put("parts", nuevas))
        }
        val o = JSONObject()
            .put("id", chatId)
            .put("titulo", if (tituloChat.isEmpty()) "Chat" else tituloChat)
            .put("t", System.currentTimeMillis())
            .put("historial", limpio)
        File(dir, "$chatId.json").writeText(o.toString())
        prefs.edit().putString("ultimo", chatId).apply()
    }

    private fun reconstruirChat() {
        chat.removeAllViews()
        for (i in 0 until historial.length()) {
            val m = historial.getJSONObject(i)
            val usuario = m.getString("role") == "user"
            val partes = m.getJSONArray("parts")
            for (k in 0 until partes.length()) {
                val t = partes.getJSONObject(k).optString("text", "")
                if (t.isEmpty()) continue
                if (t.startsWith("Archivo adjunto: ")) {
                    agregarNota("Adjunto: " + t.removePrefix("Archivo adjunto: ").lineSequence().first())
                } else if (t.startsWith("[Contenido de ese archivo")) {
                    continue
                } else {
                    agregarBurbuja(t, usuario)
                }
            }
        }
    }

    private fun cargarChat(id: String) {
        try {
            val o = JSONObject(File(File(filesDir, "chats"), "$id.json").readText())
            historial = o.getJSONArray("historial")
            chatId = id
            tituloChat = o.optString("titulo", "Chat")
            prefs.edit().putString("ultimo", id).apply()
            adjunto = null
            adjuntoTxt.visibility = View.GONE
            reconstruirChat()
        } catch (e: Exception) {
            Toast.makeText(this, "No pude abrir ese chat", Toast.LENGTH_SHORT).show()
        }
    }

    private fun nuevoChat() {
        if (ocupado) {
            Toast.makeText(this, "Espera a que Argón termine", Toast.LENGTH_SHORT).show()
            return
        }
        historial = JSONArray()
        chatId = ""
        tituloChat = ""
        adjunto = null
        adjuntoTxt.visibility = View.GONE
        prefs.edit().putString("ultimo", "").apply()
        chat.removeAllViews()
        agregarBurbuja("Chat nuevo. Listo, rafachafa.", false)
    }

    private fun abrirChats() {
        if (ocupado) {
            Toast.makeText(this, "Espera a que Argón termine", Toast.LENGTH_SHORT).show()
            return
        }
        val dir = File(filesDir, "chats")
        val archivos = (dir.listFiles() ?: emptyArray()).filter { it.name.endsWith(".json") }
        val lista = archivos.mapNotNull { f ->
            try {
                val o = JSONObject(f.readText())
                Triple(o.getString("id"), o.optString("titulo", "Chat"), o.optLong("t", 0))
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.third }
        val nombres = arrayOf("+ Nuevo chat") + lista.map { it.second }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Chats")
            .setItems(nombres) { _, which ->
                if (which == 0) {
                    nuevoChat()
                } else {
                    val (id, titulo, _) = lista[which - 1]
                    AlertDialog.Builder(this)
                        .setTitle(titulo)
                        .setItems(arrayOf("Abrir", "Borrar")) { _, op ->
                            if (op == 0) {
                                cargarChat(id)
                            } else {
                                File(File(filesDir, "chats"), "$id.json").delete()
                                if (id == chatId) {
                                    nuevoChat()
                                } else {
                                    Toast.makeText(this, "Chat borrado", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .show()
                }
            }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    // ---------- Enviar y recibir ----------

    private fun enviarMensaje() {
        val texto0 = entrada.text.toString().trim()
        val adj = adjunto
        if ((texto0.isEmpty() && adj == null) || ocupado) return
        val clave = prefs.getString("clave", "") ?: ""
        if (clave.isEmpty()) {
            pedirClave()
            return
        }
        val texto = if (texto0.isEmpty()) "Analiza este archivo." else texto0
        entrada.setText("")
        adjunto = null
        adjuntoTxt.visibility = View.GONE
        if (historial.length() == 0) {
            tituloChat = if (texto0.isNotEmpty()) texto0.take(40) else "Archivo: " + (adj?.nombre ?: "")
        }

        val partes = JSONArray()
        if (adj != null) {
            for (p in adj.partes) partes.put(p)
            agregarNota("Adjunto: ${adj.nombre}")
        }
        partes.put(JSONObject().put("text", texto))
        agregarBurbuja(texto, true)
        historial.put(JSONObject().put("role", "user").put("parts", partes))

        ocupado = true
        enviar.isEnabled = false
        cargando.visibility = View.VISIBLE
        scrollAbajo()

        val modelos = if (modo == "base") listOf(BASE, RAPIDO) else listOf(RAPIDO)
        val modoUsado = modo
        val sis = sistema()
        val contenido = historial
        Thread {
            val r = llamarApi(clave, modelos, sis, contenido)
            runOnUiThread { manejarRespuesta(r, modoUsado) }
        }.start()
    }

    private fun manejarRespuesta(r: Resultado, modoUsado: String) {
        ocupado = false
        enviar.isEnabled = true
        cargando.visibility = View.GONE
        val bruto = r.texto
        if (bruto != null) {
            val (limpio0, datos) = procesarRecuerdos(bruto)
            val limpio = if (limpio0.isEmpty()) "Anotado." else limpio0
            historial.put(
                JSONObject().put("role", "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", limpio)))
            )
            agregarBurbuja(limpio, false)
            for (d in datos) agregarNota("Recordado: $d")
            if (modoUsado == "base" && r.modelo == RAPIDO) {
                agregarNota("Respondió Argón rápido porque Argón base estaba saturado")
            }
            guardarChat()
            if (enSegundoPlano) notificar(limpio)
        } else {
            historial.remove(historial.length() - 1)
            agregarBurbuja(r.error ?: "Error desconocido", false, true)
        }
    }

    private fun llamarApi(
        clave: String, modelos: List<String>, sistema: String, contenido: JSONArray
    ): Resultado {
        var ultimoError = "Error desconocido"
        for (modelo in modelos) {
            for (intento in 0 until 2) {
                try {
                    val cuerpo = JSONObject()
                        .put(
                            "system_instruction",
                            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", sistema)))
                        )
                        .put("contents", contenido)
                    val url = URL(
                        "https://generativelanguage.googleapis.com/v1beta/models/$modelo:generateContent"
                    )
                    val con = url.openConnection() as HttpURLConnection
                    con.requestMethod = "POST"
                    con.setRequestProperty("Content-Type", "application/json")
                    con.setRequestProperty("x-goog-api-key", clave)
                    con.connectTimeout = 20000
                    con.readTimeout = 120000
                    con.doOutput = true
                    con.outputStream.use { it.write(cuerpo.toString().toByteArray(Charsets.UTF_8)) }
                    val codigo = con.responseCode
                    val stream = if (codigo in 200..299) con.inputStream else con.errorStream
                    val resp = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    con.disconnect()

                    if (codigo in 200..299) {
                        val cand = JSONObject(resp).getJSONArray("candidates").getJSONObject(0)
                        val partes = cand.optJSONObject("content")?.optJSONArray("parts")
                        val sb = StringBuilder()
                        if (partes != null) {
                            for (k in 0 until partes.length()) {
                                sb.append(partes.getJSONObject(k).optString("text", ""))
                            }
                        }
                        val texto = sb.toString().trim()
                        if (texto.isEmpty()) {
                            return Resultado(null, "Argón no devolvió texto (Google pudo haberlo bloqueado).", modelo)
                        }
                        return Resultado(texto, null, modelo)
                    }

                    if (codigo == 429 || codigo == 500 || codigo == 503) {
                        ultimoError = "Google está saturado o llegaste al límite gratis ($codigo). Espera un momento."
                        if (intento == 0) Thread.sleep(2000)
                        continue
                    }

                    val msg = try {
                        JSONObject(resp).getJSONObject("error").getString("message")
                    } catch (e: Exception) {
                        resp.take(200)
                    }
                    return Resultado(null, "Error $codigo: $msg", modelo)
                } catch (e: Exception) {
                    ultimoError = "Sin conexión o error de red: ${e.message}"
                    break
                }
            }
        }
        return Resultado(null, ultimoError, null)
    }

    private fun notificar(texto: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(this, CANAL)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle("Argón")
            .setContentText(texto.take(120))
            .setStyle(Notification.BigTextStyle().bigText(texto.take(400)))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(1, n)
    }
}
