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
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val BASE = "gemini-flash-latest"
private const val RAPIDO = "gemini-flash-lite-latest"
private const val CANAL = "argon_respuestas"
private const val SISTEMA =
    "Eres Argón, el asistente personal de rafachafa, así lo llamas siempre. " +
    "Hablas español. Eres directo, serio y de pocas palabras: respuestas cortas y simples, " +
    "sin saludos exagerados, sin emojis y sin entusiasmo de más. " +
    "Eres un programador experto cuando te piden código."

class MainActivity : Activity() {

    private class Resultado(val texto: String?, val error: String?, val modelo: String?)

    private lateinit var prefs: SharedPreferences
    private lateinit var chat: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var entrada: EditText
    private lateinit var enviar: Button
    private lateinit var btnBase: Button
    private lateinit var btnRapido: Button
    private lateinit var descripcion: TextView
    private lateinit var cargando: LinearLayout

    private val historial = JSONArray()
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
        agregarBurbuja("Listo, rafachafa.", false)
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
        fila1.setPadding(dp(12), dp(8), dp(12), 0)
        val titulo = TextView(this)
        titulo.text = "Argón"
        titulo.textSize = 22f
        titulo.setTypeface(null, Typeface.BOLD)
        titulo.setTextColor(Color.WHITE)
        fila1.addView(titulo, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val btnClave = Button(this)
        btnClave.text = "Clave"
        btnClave.transformationMethod = null
        btnClave.setOnClickListener { pedirClave() }
        fila1.addView(btnClave)
        root.addView(fila1)

        val fila2 = LinearLayout(this)
        fila2.orientation = LinearLayout.HORIZONTAL
        fila2.setPadding(dp(8), dp(4), dp(8), 0)
        btnBase = Button(this)
        btnBase.text = "Argón base"
        btnBase.transformationMethod = null
        btnBase.setOnClickListener { modo = "base"; actualizarModo() }
        btnRapido = Button(this)
        btnRapido.text = "Argón rápido"
        btnRapido.transformationMethod = null
        btnRapido.setOnClickListener { modo = "rapido"; actualizarModo() }
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

        val fila3 = LinearLayout(this)
        fila3.orientation = LinearLayout.HORIZONTAL
        fila3.gravity = Gravity.CENTER_VERTICAL
        fila3.setPadding(dp(8), dp(4), dp(8), dp(8))
        entrada = EditText(this)
        entrada.hint = "Escríbele a Argón..."
        entrada.setHintTextColor(Color.parseColor("#6B7480"))
        entrada.setTextColor(Color.WHITE)
        entrada.inputType = InputType.TYPE_CLASS_TEXT or
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        entrada.maxLines = 5
        fila3.addView(entrada, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        enviar = Button(this)
        enviar.text = "Enviar"
        enviar.transformationMethod = null
        enviar.setOnClickListener { enviarMensaje() }
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

    private fun enviarMensaje() {
        val texto = entrada.text.toString().trim()
        if (texto.isEmpty() || ocupado) return
        val clave = prefs.getString("clave", "") ?: ""
        if (clave.isEmpty()) {
            pedirClave()
            return
        }
        entrada.setText("")
        agregarBurbuja(texto, true)
        historial.put(
            JSONObject().put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", texto)))
        )
        ocupado = true
        enviar.isEnabled = false
        cargando.visibility = View.VISIBLE
        scrollAbajo()

        val modelos = if (modo == "base") listOf(BASE, RAPIDO) else listOf(RAPIDO)
        val modoUsado = modo
        Thread {
            val r = llamarApi(clave, modelos)
            runOnUiThread {
                ocupado = false
                enviar.isEnabled = true
                cargando.visibility = View.GONE
                if (r.texto != null) {
                    historial.put(
                        JSONObject().put("role", "model")
                            .put("parts", JSONArray().put(JSONObject().put("text", r.texto)))
                    )
                    agregarBurbuja(r.texto, false)
                    if (modoUsado == "base" && r.modelo == RAPIDO) {
                        agregarNota("Respondió Argón rápido porque Argón base estaba saturado")
                    }
                    if (enSegundoPlano) notificar(r.texto)
                } else {
                    historial.remove(historial.length() - 1)
                    agregarBurbuja(r.error ?: "Error desconocido", false, true)
                }
            }
        }.start()
    }

    private fun llamarApi(clave: String, modelos: List<String>): Resultado {
        var ultimoError = "Error desconocido"
        for (modelo in modelos) {
            for (intento in 0 until 2) {
                try {
                    val cuerpo = JSONObject()
                        .put(
                            "system_instruction",
                            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", SISTEMA)))
                        )
                        .put("contents", historial)
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
