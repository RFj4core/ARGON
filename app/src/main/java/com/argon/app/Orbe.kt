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
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class Orbe(ctx: Context) : View(ctx) {

    var estado = 0
    var nivel = 0f
    var etiqueta = ""

    private val n = 28
    private val cam = 3.2f
    private val x0 = FloatArray(n)
    private val y0 = FloatArray(n)
    private val z0 = FloatArray(n)
    private val sx = FloatArray(n)
    private val sy = FloatArray(n)
    private val sz = FloatArray(n)
    private val ss = FloatArray(n)
    private val orden = IntArray(n) { it }
    private var angY = 0f
    private var ultimo = 0L
    private var nivelSuave = 0f

    private val pFondo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pLinea = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pNodo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pTxt = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    init {
        val total = 24
        val dorado = (PI * (3.0 - sqrt(5.0))).toFloat()
        val rnd = java.util.Random(42)
        for (i in 0 until total) {
            val y = 1f - 2f * (i + 0.5f) / total
            val r = sqrt(1f - y * y)
            val phi = i * dorado
            // Forma esférica irregular con variaciones suaves
            val ruido = 0.85f + rnd.nextFloat() * 0.25f
            x0[i] = cos(phi) * r * 0.9f * ruido
            y0[i] = y * 0.9f * ruido
            z0[i] = sin(phi) * r * 0.9f * ruido
        }
        for (i in total until n) {
            val x = rnd.nextFloat() * 2f - 1f
            val y = rnd.nextFloat() * 2f - 1f
            val z = rnd.nextFloat() * 2f - 1f
            val len = sqrt(x * x + y * y + z * z)
            if (len > 0f) {
                x0[i] = (x / len) * 0.7f
                y0[i] = (y / len) * 0.7f
                z0[i] = (z / len) * 0.7f
            }
        }

        pLinea.style = Paint.Style.STROKE
        pLinea.strokeCap = Paint.Cap.ROUND
        pTxt.color = Color.parseColor("#B8D4FF")
        pTxt.textAlign = Paint.Align.CENTER
        pTxt.textSize = dp(13f)
    }

    override fun onDraw(c: Canvas) {
        val ahora = System.nanoTime()
        val dt = if (ultimo == 0L) 0f else ((ahora - ultimo) / 1.0e9f).coerceAtMost(0.05f)
        ultimo = ahora
        val t = ahora / 1.0e9
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f - (if (etiqueta.isEmpty()) 0f else dp(8f))
        val radio = min(w, h) / 2f - dp(24f)

        nivelSuave += (nivel - nivelSuave) * 0.2f
        val energia = when (estado) {
            1 -> 0.35f + nivelSuave * 0.9f
            2 -> 0.75f
            3 -> 0.45f + 0.25f * (0.5f + 0.5f * sin(t * 6.0).toFloat())
            else -> 0.2f
        }
        val giro = when (estado) {
            1 -> 0.5f + nivelSuave * 2.2f
            2 -> 1.6f
            3 -> 0.8f
            else -> 0.3f
        }
        angY += dt * giro
        val angX = (0.35 + 0.12 * sin(t * 0.6)).toFloat()
        val cY = cos(angY)
        val sY = sin(angY)
        val cX = cos(angX)
        val sX = sin(angX)

        // Sin burbuja ni aro exterior, solo el brillo ambiental difuso
        pFondo.shader = RadialGradient(
            cx, cy, radio * 1.2f,
            intArrayOf(
                Color.argb((40 + 60 * energia).toInt().coerceIn(0, 180), 61, 123, 234),
                Color.argb(0, 61, 123, 234)
            ),
            floatArrayOf(0.4f, 1f), Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, radio * 1.2f, pFondo)

        for (i in 0 until n) {
            // Movimiento ondulatorio interno dinámico para simular conexiones variables
            val pulsoCelular = sin(t * 2.5 + i * 1.5).toFloat() * 0.08f * (1f + energia)
            val bx = x0[i] * (1f + pulsoCelular)
            val by = y0[i] * (1f + pulsoCelular)
            val bz = z0[i] * (1f + pulsoCelular)

            val xr = bx * cY + bz * sY
            val zr = -bx * sY + bz * cY
            val yr = by * cX - zr * sX
            val zz = by * sX + zr * cX
            
            val esc = cam / (cam - zz)
            sx[i] = cx + xr * esc * radio * 0.95f
            sy[i] = cy + yr * esc * radio * 0.95f
            sz[i] = zz
            ss[i] = esc
        }

        for (i in 1 until n) {
            val v = orden[i]
            var j = i - 1
            while (j >= 0 && sz[orden[j]] > sz[v]) {
                orden[j + 1] = orden[j]
                j--
            }
            orden[j + 1] = v
        }

        // Conexiones dinámicas basadas en proximidad variable en tiempo real
        for (i in 0 until n) {
            for (j in i + 1 until n) {
                val dx = sx[i] - sx[j]
                val dy = sy[i] - sy[j]
                val distSq = dx * dx + dy * dy
                val umbralDist = dp(75f) * dp(75f)

                if (distSq < umbralDist) {
                    val factorDist = 1f - (sqrt(distSq) / dp(75f))
                    val prof = (((sz[i] + sz[j]) / 2f) + 1f) / 2f
                    // Animación de parpadeo para conectar y desconectar dinámicamente
                    val enlaceActivo = sin(t * 3.0 + i + j).toFloat()
                    if (enlaceActivo > -0.2f) {
                        val alfa = (40 * factorDist * prof * (0.4f + energia * 0.6f)).toInt().coerceIn(0, 200)
                        pLinea.color = Color.argb(alfa, 140, 200, 255)
                        pLinea.strokeWidth = dp(0.8f) * prof
                        c.drawLine(sx[i], sy[i], sx[j], sy[j], pLinea)
                    }
                }
            }
        }

        // Dibujar nodos (células) más delgados y estilizados
        for (idx in 0 until n) {
            val i = orden[idx]
            val prof = (sz[i] + 1f) / 2f
            val pulso = 0.5f + 0.5f * sin(t * 4.0 + i).toFloat()
            val r = (dp(0.8f) + dp(1.2f * pulso * (0.5f + energia))) * ss[i]
            
            pNodo.color = Color.argb((40 + 160 * prof * energia).toInt().coerceIn(0, 255), 180, 220, 255)
            c.drawCircle(sx[i], sy[i], r, pNodo)
        }

        if (etiqueta.isNotEmpty()) c.drawText(etiqueta, cx, h - dp(8f), pTxt)
        postInvalidateOnAnimation()
    }
}
