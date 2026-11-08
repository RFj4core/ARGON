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

    private val n = 34
    private val cam = 3.2f
    private val x0 = FloatArray(n)
    private val y0 = FloatArray(n)
    private val z0 = FloatArray(n)
    private val sx = FloatArray(n)
    private val sy = FloatArray(n)
    private val sz = FloatArray(n)
    private val ss = FloatArray(n)
    private val orden = IntArray(n) { it }
    private val ea: IntArray
    private val eb: IntArray
    private var angY = 0f
    private var ultimo = 0L
    private var nivelSuave = 0f

    private val pFondo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pAro = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pLinea = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pNodo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pTxt = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    init {
        val total = 24
        val dorado = (PI * (3.0 - sqrt(5.0))).toFloat()
        for (i in 0 until total) {
            val y = 1f - 2f * (i + 0.5f) / total
            val r = sqrt(1f - y * y)
            val phi = i * dorado
            x0[i] = cos(phi) * r * 0.92f
            y0[i] = y * 0.92f
            z0[i] = sin(phi) * r * 0.92f
        }
        val rnd = java.util.Random(11)
        for (i in total until n) {
            var x: Float
            var y: Float
            var z: Float
            do {
                x = rnd.nextFloat() * 2f - 1f
                y = rnd.nextFloat() * 2f - 1f
                z = rnd.nextFloat() * 2f - 1f
            } while (x * x + y * y + z * z > 1f)
            x0[i] = x * 0.5f
            y0[i] = y * 0.5f
            z0[i] = z * 0.5f
        }
        val pares = ArrayList<Int>()
        val vistos = HashSet<Int>()
        for (i in 0 until n) {
            val d = FloatArray(n) { j ->
                if (j == i) {
                    Float.MAX_VALUE
                } else {
                    val dx = x0[i] - x0[j]
                    val dy = y0[i] - y0[j]
                    val dz = z0[i] - z0[j]
                    dx * dx + dy * dy + dz * dz
                }
            }
            for (k in 0 until 3) {
                var mejor = -1
                var md = Float.MAX_VALUE
                for (j in 0 until n) {
                    if (d[j] < md) {
                        md = d[j]
                        mejor = j
                    }
                }
                if (mejor >= 0) {
                    d[mejor] = Float.MAX_VALUE
                    val a = min(i, mejor)
                    val b = maxOf(i, mejor)
                    if (vistos.add(a * n + b)) {
                        pares.add(a)
                        pares.add(b)
                    }
                }
            }
        }
        ea = IntArray(pares.size / 2) { pares[it * 2] }
        eb = IntArray(pares.size / 2) { pares[it * 2 + 1] }

        pAro.style = Paint.Style.STROKE
        pAro.strokeWidth = dp(1.5f)
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

        pFondo.shader = RadialGradient(
            cx, cy, radio * 1.3f,
            intArrayOf(
                Color.argb((50 + 70 * energia).toInt().coerceIn(0, 200), 61, 123, 234),
                Color.argb(0, 61, 123, 234)
            ),
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
            val xr = x0[i] * cY + z0[i] * sY
            val zr = -x0[i] * sY + z0[i] * cY
            val yr = y0[i] * cX - zr * sX
            val zz = y0[i] * sX + zr * cX
            val k = 1f + 0.05f * energia * sin(t * 4.0 + i).toFloat()
            val esc = cam / (cam - zz * k)
            sx[i] = cx + xr * k * esc * radio * 0.95f
            sy[i] = cy + yr * k * esc * radio * 0.95f
            sz[i] = zz * k
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

        for (e in ea.indices) {
            val a = ea[e]
            val b = eb[e]
            val prof = (((sz[a] + sz[b]) / 2f) + 1f) / 2f
            val alfa = (30 + 170 * prof * (0.35f + energia)).toInt().coerceIn(0, 255)
            pLinea.color = Color.argb(alfa, 150, 205, 255)
            pLinea.strokeWidth = dp(0.6f) + dp(1.4f) * prof
            c.drawLine(sx[a], sy[a], sx[b], sy[b], pLinea)
        }
        for (e in ea.indices) {
            val a = ea[e]
            val b = eb[e]
            val f = ((t * (0.3 + 0.7 * energia) + e * 0.173) % 1.0).toFloat()
            val prof = (((sz[a] + sz[b]) / 2f) + 1f) / 2f
            val px = sx[a] + (sx[b] - sx[a]) * f
            val py = sy[a] + (sy[b] - sy[a]) * f
            pNodo.color = Color.argb((60 + 195 * prof * energia).toInt().coerceIn(0, 255), 210, 235, 255)
            c.drawCircle(px, py, dp(1.4f) + dp(1.4f) * prof, pNodo)
        }
        for (idx in 0 until n) {
            val i = orden[idx]
            val prof = (sz[i] + 1f) / 2f
            val pulso = 0.5f + 0.5f * sin(t * (2.0 + i * 0.07) + i).toFloat()
            val r = (dp(1.6f) + dp(1.8f) * pulso * (0.5f + energia)) * ss[i]
            pNodo.color = Color.argb((20 + 40 * prof).toInt().coerceIn(0, 255), 120, 180, 255)
            c.drawCircle(sx[i], sy[i], r * 2.4f, pNodo)
            pNodo.color = Color.argb((70 + 185 * prof).toInt().coerceIn(0, 255), 190, 225, 255)
            c.drawCircle(sx[i], sy[i], r, pNodo)
        }

        if (etiqueta.isNotEmpty()) c.drawText(etiqueta, cx, h - dp(8f), pTxt)
        postInvalidateOnAnimation()
    }
}
