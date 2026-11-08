package com.argon.app

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import kotlin.math.PI
import kotlin.math.sin

class PuntosCarga(ctx: Context, color: Int) : View(ctx) {

    private val pincel = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1300
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            t = it.animatedValue as Float
            invalidate()
        }
    }

    init {
        pincel.color = color
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) {
            if (!anim.isStarted) anim.start()
        } else {
            anim.cancel()
        }
    }

    override fun onDetachedFromWindow() {
        anim.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val r = height * 0.15f
        val sep = width / 3f
        for (i in 0 until 3) {
            val fase = (((t - i * 0.18f) % 1f) + 1f) % 1f
            val k = (0.5 + 0.5 * sin(2.0 * PI * fase - PI / 2)).toFloat()
            pincel.alpha = (70 + 185 * k).toInt()
            canvas.drawCircle(sep * (i + 0.5f), height / 2f - r * k, r * (0.8f + 0.4f * k), pincel)
        }
    }
}

object Visuales {

    sealed class Trozo {
        class Texto(val t: String) : Trozo()
        class Tabla(
            val encabezado: List<String>,
            val alin: List<Int>,
            val filas: List<List<String>>
        ) : Trozo()
    }

    private val reCelda = Regex("(?<!\\\\)\\|")

    private fun esSeparador(l: String): Boolean {
        val s = l.trim()
        return s.contains("|") && s.contains("-") &&
            s.all { it == '|' || it == '-' || it == ':' || it == ' ' || it == '\t' }
    }

    private fun celdas(l: String): List<String> {
        var s = l.trim()
        if (s.startsWith("|")) s = s.substring(1)
        if (s.endsWith("|") && !s.endsWith("\\|")) s = s.dropLast(1)
        return s.split(reCelda).map { it.replace("\\|", "|").trim() }
    }

    fun trozos(texto: String): List<Trozo> {
        val lineas = texto.split("\n")
        val out = ArrayList<Trozo>()
        val buf = StringBuilder()
        var i = 0
        while (i < lineas.size) {
            val l = lineas[i]
            if (l.contains("|") && i + 1 < lineas.size && esSeparador(lineas[i + 1])) {
                val enc = celdas(l)
                val sep = celdas(lineas[i + 1])
                if (enc.isNotEmpty() && sep.size == enc.size) {
                    if (buf.isNotBlank()) out.add(Trozo.Texto(buf.toString()))
                    buf.setLength(0)
                    val alin = sep.map { c ->
                        when {
                            c.startsWith(":") && c.endsWith(":") -> Gravity.CENTER_HORIZONTAL
                            c.endsWith(":") -> Gravity.END
                            else -> Gravity.START
                        }
                    }
                    val filas = ArrayList<List<String>>()
                    i += 2
                    while (i < lineas.size && lineas[i].isNotBlank() && lineas[i].contains("|")) {
                        val f = celdas(lineas[i])
                        filas.add(List(enc.size) { k -> f.getOrElse(k) { "" } })
                        i++
                    }
                    out.add(Trozo.Tabla(enc, alin, filas))
                    continue
                }
            }
            buf.append(l).append("\n")
            i++
        }
        if (buf.isNotBlank()) out.add(Trozo.Texto(buf.toString()))
        return out
    }

    fun redondear(v: View, radioDp: Int) {
        val r = radioDp * v.resources.displayMetrics.density
        v.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, r)
            }
        }
        v.clipToOutline = true
    }

    fun vistaTabla(ctx: Context, t: Trozo.Tabla, fondoCodigo: Int, colorTexto: Int): View {
        val d = ctx.resources.displayMetrics.density
        fun dp(n: Int): Int = (n * d).toInt()
        val borde = Color.argb(60, 255, 255, 255)
        val unPx = dp(1).coerceAtLeast(1)

        fun divisor(): GradientDrawable {
            val g = GradientDrawable()
            g.setColor(borde)
            g.setSize(unPx, unPx)
            return g
        }

        val tabla = TableLayout(ctx)
        tabla.showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
        tabla.dividerDrawable = divisor()

        fun fila(valores: List<String>, cabecera: Boolean): TableRow {
            val r = TableRow(ctx)
            r.showDividers = LinearLayout.SHOW_DIVIDER_MIDDLE
            r.dividerDrawable = divisor()
            if (cabecera) r.setBackgroundColor(Color.argb(40, 255, 255, 255))
            for ((k, c) in valores.withIndex()) {
                val tv = TextView(ctx)
                tv.text = Formato.render(c, fondoCodigo)
                tv.textSize = 14f
                tv.setTextColor(colorTexto)
                if (cabecera) tv.setTypeface(tv.typeface, Typeface.BOLD)
                tv.gravity = t.alin[k] or Gravity.CENTER_VERTICAL
                tv.setPadding(dp(10), dp(7), dp(10), dp(7))
                tv.maxWidth = dp(220)
                r.addView(
                    tv,
                    TableRow.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }
            return r
        }

        tabla.addView(fila(t.encabezado, true))
        for (f in t.filas) tabla.addView(fila(f, false))

        val caja = LinearLayout(ctx)
        val fondo = GradientDrawable()
        fondo.setColor(Color.argb(40, 0, 0, 0))
        fondo.cornerRadius = dp(10).toFloat()
        fondo.setStroke(unPx, borde)
        caja.background = fondo
        caja.addView(
            tabla,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        redondear(caja, 10)

        val h = HorizontalScrollView(ctx)
        h.isHorizontalScrollBarEnabled = false
        h.addView(
            caja,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(dp(6), dp(6), dp(6), dp(6))
        h.layoutParams = lp
        return h
    }

    fun envolverHtml(codigo: String, lang: String): String {
        val esSvg = lang == "svg" || codigo.trimStart().startsWith("<svg")
        val base = if (esSvg) {
            "<body style='margin:0;background:#fff;display:flex;align-items:center;" +
                "justify-content:center;min-height:100vh'>$codigo</body>"
        } else {
            codigo
        }
        return conMotor3D(base)
    }

    // Si el HTML usa Argon3D, le agrega el motor 3D incluido en la app (no necesita internet).
    fun conMotor3D(html: String): String {
        if (!html.contains("Argon3D")) return html
        val tag = "<script>" + Motor3D.JS + "</script>"
        val cabeza = Regex("<head[^>]*>", RegexOption.IGNORE_CASE).find(html)
        if (cabeza != null) {
            val i = cabeza.range.last + 1
            return html.substring(0, i) + tag + html.substring(i)
        }
        val raiz = Regex("<html[^>]*>", RegexOption.IGNORE_CASE).find(html)
        if (raiz != null) {
            val i = raiz.range.last + 1
            return html.substring(0, i) + tag + html.substring(i)
        }
        return tag + html
    }

    class CajaHtml(val vista: View, val web: WebView, val html: String)

    fun cajaHtml(ctx: Context, html: String): CajaHtml {
        val d = ctx.resources.displayMetrics.density
        fun dp(n: Int): Int = (n * d).toInt()

        val w = WebView(ctx)
        w.settings.javaScriptEnabled = false
        w.settings.blockNetworkLoads = true
        w.settings.allowFileAccess = false
        w.settings.allowContentAccess = false
        w.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = true
        }
        w.setBackgroundColor(Color.WHITE)
        w.setOnTouchListener { v, e ->
            if (e.action == MotionEvent.ACTION_DOWN || e.action == MotionEvent.ACTION_MOVE) {
                if (v.canScrollVertically(-1) || v.canScrollVertically(1) ||
                    v.canScrollHorizontally(-1) || v.canScrollHorizontally(1)
                ) {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                }
            }
            false
        }
        w.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)

        val caja = LinearLayout(ctx)
        val fondo = GradientDrawable()
        fondo.setColor(Color.WHITE)
        fondo.cornerRadius = dp(12).toFloat()
        fondo.setStroke(dp(1).coerceAtLeast(1), Color.argb(60, 255, 255, 255))
        caja.background = fondo
        caja.addView(w, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(300)))
        redondear(caja, 12)
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(dp(6), 0, dp(6), dp(6))
        caja.layoutParams = lp
        return CajaHtml(caja, w, html)
    }

    fun activarJs(c: CajaHtml) {
        c.web.settings.javaScriptEnabled = true
        c.web.loadDataWithBaseURL(null, c.html, "text/html", "utf-8", null)
    }
}
