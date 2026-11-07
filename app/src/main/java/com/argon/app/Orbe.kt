package com.argon.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class Orbe(ctx: Context) : View(ctx) {

    var estado = 0
    var nivel = 0f
    var etiqueta = ""

    private val n = 16
    private val bx = FloatArray(n)
    private val by = FloatArray(n)
    private val fase = FloatArray(n)
    private val vel = FloatArray(n)
    private val px = FloatArray(n)
    private val py = FloatArray(n)
    private var nivelSuave = 0f

    private val pFondo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pAro = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pLinea = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pNodo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pTxt = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    init {
        val r = java.util.Random(7)
        for (i in 0 until n) {
            val ang = r.nextFloat() * (2 * PI).toFloat()
            val rad = sqrt(r.nextFloat()) * 0.78f
            bx[i] = cos(ang) * rad
            by[i] = sin(ang) * rad
            fase[i] = r.nextFloat() * (2 * PI).toFloat()
            vel[i] = 0.4f + r.nextFloat() * 0.8f
        }
        pAro.style = Paint.Style.STROKE
        pAro.strokeWidth = dp(1.5f)
        pLinea.style = Paint.Style.STROKE
        pLinea.strokeCap = Paint.Cap.ROUND
        pTxt.color = Color.parseColor("#B8D4FF")
        pTxt.textAlign = Paint.Align.CENTER
        pTxt.textSize = dp(13f)
    }

    override fun onDraw(c: Canvas) {
        val t = System.nanoTime() / 1.0e9
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f - (if (etiqueta.isEmpty()) 0f else dp(8f))
        val radio = min(w, h) / 2f - dp(22f)
        nivelSuave += (nivel - nivelSuave) * 0.2f
        val energia = when (estado) {
            1 -> 0.35f + nivelSuave * 0.9f
            2 -> 0.7f
            3 -> 0.45f + 0.25f * (0.5f + 0.5f * sin(t * 6.0).toFloat())
            else -> 0.2f
        }

        val alfaResplandor = (50 + 70 * energia).toInt().coerceIn(0, 200)
        pFondo.shader = RadialGradient(
            cx, cy, radio * 1.3f,
            intArrayOf(Color.argb(alfaResplandor, 61, 123, 234), Color.argb(0, 61, 123, 234)),
            floatArrayOf(0.55f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, radio * 1.3f, pFondo)

        pFondo.shader = RadialGradient(
            cx - radio * 0.3f, cy - radio * 0.35f, radio * 1.15f,
            intArrayOf(Color.argb(95, 120, 180, 255), Color.argb(35, 40, 90, 200), Color.argb(75, 20, 50, 130)),
            floatArrayOf(0f, 0.7f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, radio, pFondo)
        pAro.color = Color.argb(150, 140, 190, 255)
        c.drawCircle(cx, cy, radio, pAro)

        for (i in 0 until n) {
            val dx = (sin(t * vel[i] * 0.9 + fase[i]) * 0.07 * (1 + energia)).toFloat()
            val dy = (cos(t * vel[i] * 0.8 + fase[i] * 1.3) * 0.07 * (1 + energia)).toFloat()
            px[i] = cx + (bx[i] + dx) * radio
            py[i] = cy + (by[i] + dy) * radio
        }

        val umbral = radio * 0.62f
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val d = hypot(px[i] - px[j], py[i] - py[j])
                if (d < umbral) {
                    val a = 1f - d / umbral
                    val onda = if (estado == 2) {
                        0.5f + 0.5f * sin(t * 7.0 - (px[i] + py[j]) / radio * 4.0).toFloat()
                    } else {
                        1f
                    }
                    val alfa = (40 + 150 * a * (0.4f + energia) * onda).toInt().coerceIn(0, 255)
                    pLinea.color = Color.argb(alfa, 150, 205, 255)
                    pLinea.strokeWidth = dp(1f) + a * dp(1f)
                    c.drawLine(px[i], py[i], px[j], py[j], pLinea)
                }
            }
        }

        for (i in 0 until n) {
            val pulso = 0.5f + 0.5f * sin(t * (2.0 + vel[i]) + fase[i]).toFloat()
            val r = dp(2.2f) + dp(2.2f) * pulso * (0.5f + energia)
            pNodo.color = Color.argb(40, 120, 180, 255)
            c.drawCircle(px[i], py[i], r * 2.2f, pNodo)
            pNodo.color = Color.argb((140 + 110 * pulso).toInt().coerceIn(0, 255), 190, 225, 255)
            c.drawCircle(px[i], py[i], r, pNodo)
        }

        if (etiqueta.isNotEmpty()) c.drawText(etiqueta, cx, h - dp(8f), pTxt)
        postInvalidateOnAnimation()
    }
}
