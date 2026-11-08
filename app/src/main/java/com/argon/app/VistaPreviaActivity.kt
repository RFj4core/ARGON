package com.argon.app

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView

class VistaPreviaActivity : Activity() {

    private var web: WebView? = null

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    private fun incrustar(p: Proyectos, proyecto: String, archivo: String, base: String): String {
        val carpeta = archivo.substringBeforeLast("/", "")
        fun resolver(ruta: String): String {
            val limpia = ruta.trim().removePrefix("./")
            return if (carpeta.isEmpty()) limpia else "$carpeta/$limpia"
        }
        var h = Regex("<link[^>]*href=[\"']([^\"']+\\.css)[\"'][^>]*>", RegexOption.IGNORE_CASE)
            .replace(base) { m ->
                val css = p.leerTexto(proyecto, resolver(m.groupValues[1]))
                if (css != null) "<style>\n$css\n</style>" else m.value
            }
        h = Regex("<script[^>]*src=[\"']([^\"']+\\.js)[\"'][^>]*>\\s*</script>", RegexOption.IGNORE_CASE)
            .replace(h) { m ->
                val js = p.leerTexto(proyecto, resolver(m.groupValues[1]))
                if (js != null) "<script>\n" + js.replace("</script", "<\\/script") + "\n</script>" else m.value
            }
        return h
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val proyecto = intent.getStringExtra("proyecto") ?: ""
        val archivo = intent.getStringExtra("archivo") ?: "index.html"
        var html: String? = intent.getStringExtra("html")
        if (html == null) {
            val p = Proyectos(this)
            val base = p.leerTexto(proyecto, archivo)
            html = if (base == null) {
                "<body style='background:#111;color:#eee;font-family:sans-serif;padding:24px'>" +
                    "No encontré <b>$archivo</b> en el proyecto <b>$proyecto</b>.</body>"
            } else {
                                Visuales.conMotor3D(incrustar(p, proyecto, archivo, base))
            }
        }

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#0E1117"))
        root.setOnApplyWindowInsetsListener { v, ins ->
            v.setPadding(
                ins.systemWindowInsetLeft, ins.systemWindowInsetTop,
                ins.systemWindowInsetRight, ins.systemWindowInsetBottom
            )
            ins
        }

        val barra = LinearLayout(this)
        barra.orientation = LinearLayout.HORIZONTAL
        barra.gravity = Gravity.CENTER_VERTICAL
        barra.setPadding(dp(10), dp(8), dp(10), dp(8))
        val cerrar = TextView(this)
        cerrar.text = "✕"
        cerrar.textSize = 18f
        cerrar.setTextColor(Color.parseColor("#E8EBF4"))
        cerrar.gravity = Gravity.CENTER
        val g = GradientDrawable()
        g.shape = GradientDrawable.OVAL
        g.setColor(Color.argb(28, 255, 255, 255))
        g.setStroke(dp(1), Color.argb(48, 255, 255, 255))
        cerrar.background = g
        cerrar.layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        cerrar.setOnClickListener { finish() }
        barra.addView(cerrar)
        val titulo = TextView(this)
        titulo.text = if (proyecto.isEmpty()) "Vista previa" else "Vista previa · $archivo"
        titulo.setTextColor(Color.parseColor("#E8EBF4"))
        titulo.textSize = 16f
        titulo.setPadding(dp(14), 0, 0, 0)
        barra.addView(titulo)
        root.addView(barra)

        val w = WebView(this)
        web = w
        w.settings.javaScriptEnabled = true
        w.settings.blockNetworkLoads = true
        w.settings.allowFileAccess = false
        w.settings.allowContentAccess = false
        w.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
        }
        w.setBackgroundColor(Color.WHITE)
        w.loadDataWithBaseURL(null, html ?: "", "text/html", "utf-8", null)
        root.addView(w, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    override fun onDestroy() {
        super.onDestroy()
        web?.destroy()
    }
}
