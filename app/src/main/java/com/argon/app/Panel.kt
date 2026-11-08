package com.argon.app

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object Panel {

    class Item(val texto: String, val sub: String, val accion: () -> Unit)

    fun abrir(act: Activity, items: List<Item>, githubUrl: String) {
        val dm = act.resources.displayMetrics
        fun dp(n: Int): Int = (n * dm.density).toInt()
        val ancho = (dm.widthPixels * 0.56f).toInt().coerceAtLeast(dp(230))
        val cTexto = Color.parseColor("#E8EBF4")
        val cGris = Color.parseColor("#8E97AB")

        val d = Dialog(act)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val raiz = LinearLayout(act)
        raiz.orientation = LinearLayout.VERTICAL
        val fondo = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.parseColor("#161C2A"), Color.parseColor("#0C0F15"))
        )
        val r = dp(24).toFloat()
        fondo.cornerRadii = floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
        fondo.setStroke(dp(1), Color.argb(40, 255, 255, 255))
        raiz.background = fondo
        raiz.setPadding(0, dp(28), 0, dp(12))

        fun cerrarYHacer(accion: () -> Unit) {
            raiz.animate().translationX(-ancho.toFloat()).setDuration(170).withEndAction {
                d.dismiss()
                accion()
            }.start()
        }

        fun linea(): View {
            val v = View(act)
            v.setBackgroundColor(Color.argb(36, 255, 255, 255))
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
            lp.setMargins(dp(16), dp(6), dp(16), dp(6))
            v.layoutParams = lp
            return v
        }

        fun fila(t: String, s: String, extra: String, accion: () -> Unit): View {
            val f = LinearLayout(act)
            f.orientation = LinearLayout.HORIZONTAL
            f.gravity = Gravity.CENTER_VERTICAL
            f.setPadding(dp(20), dp(13), dp(16), dp(13))
            val col = LinearLayout(act)
            col.orientation = LinearLayout.VERTICAL
            val a = TextView(act)
            a.text = t
            a.textSize = 16f
            a.setTextColor(cTexto)
            val b = TextView(act)
            b.text = s
            b.textSize = 12f
            b.setTextColor(cGris)
            col.addView(a)
            col.addView(b)
            f.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val c = TextView(act)
            c.text = extra
            c.textSize = 20f
            c.setTextColor(cGris)
            f.addView(c)
            f.background = RippleDrawable(
                ColorStateList.valueOf(Color.argb(46, 255, 255, 255)), null, ColorDrawable(Color.WHITE)
            )
            f.setOnClickListener { cerrarYHacer(accion) }
            return f
        }

        val titulo = TextView(act)
        titulo.text = "Argón"
        titulo.textSize = 24f
        titulo.setTypeface(null, Typeface.BOLD)
        titulo.setTextColor(cTexto)
        titulo.setPadding(dp(20), 0, dp(20), 0)
        raiz.addView(titulo)
        val sub = TextView(act)
        sub.text = "Asistente personal"
        sub.textSize = 12f
        sub.setTextColor(cGris)
        sub.setPadding(dp(20), dp(2), dp(20), dp(14))
        raiz.addView(sub)
        raiz.addView(linea())

        for (it in items) raiz.addView(fila(it.texto, it.sub, "›", it.accion))

        raiz.addView(View(act), LinearLayout.LayoutParams(1, 0, 1f))
        raiz.addView(linea())
        raiz.addView(fila("GitHub", "RFj4core/ARGON", "↗") {
            try {
                act.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)))
            } catch (e: Exception) {
                Toast.makeText(act, "No pude abrir el enlace", Toast.LENGTH_SHORT).show()
            }
        })

        val version = try {
            act.packageManager.getPackageInfo(act.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
        val pie = TextView(act)
        pie.text = "Argón $version"
        pie.textSize = 11f
        pie.setTextColor(cGris)
        pie.setPadding(dp(20), dp(2), dp(20), 0)
        raiz.addView(pie)

        d.setContentView(raiz)
        val w = d.window
        if (w != null) {
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(ancho, ViewGroup.LayoutParams.MATCH_PARENT)
            w.setGravity(Gravity.START or Gravity.TOP)
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            w.setDimAmount(0.6f)
        }
        raiz.translationX = -ancho.toFloat()
        d.show()
        raiz.animate().translationX(0f).setDuration(220).start()
    }
}
