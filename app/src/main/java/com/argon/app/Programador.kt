package com.argon.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Calendar

object Programador {

    private const val ARCHIVO = "programaciones.json"

    // Secreto local: solo las notificaciones de Argón lo llevan, así otra app no puede lanzar rutinas.
    fun token(ctx: Context): String {
        val pr = ctx.getSharedPreferences("argon", Context.MODE_PRIVATE)
        var t = pr.getString("tok_rutina", null)
        if (t == null) {
            t = java.util.UUID.randomUUID().toString()
            pr.edit().putString("tok_rutina", t).apply()
        }
        return t
    }

    fun cargar(ctx: Context): JSONArray =
        try {
            JSONArray(File(ctx.filesDir, ARCHIVO).readText())
        } catch (e: Exception) {
            JSONArray()
        }

    private fun guardar(ctx: Context, a: JSONArray) {
        File(ctx.filesDir, ARCHIVO).writeText(a.toString())
    }

    fun existe(ctx: Context, nombre: String): Boolean {
        val a = cargar(ctx)
        for (i in 0 until a.length()) if (a.getJSONObject(i).optString("n") == nombre) return true
        return false
    }

    fun proximo(h: Int, m: Int, dias: Set<Int>): Long {
        val ahora = System.currentTimeMillis()
        for (i in 0..7) {
            val d = Calendar.getInstance()
            d.add(Calendar.DAY_OF_YEAR, i)
            d.set(Calendar.HOUR_OF_DAY, h)
            d.set(Calendar.MINUTE, m)
            d.set(Calendar.SECOND, 0)
            d.set(Calendar.MILLISECOND, 0)
            if (d.timeInMillis > ahora + 1000 &&
                (dias.isEmpty() || dias.contains(d.get(Calendar.DAY_OF_WEEK)))
            ) {
                return d.timeInMillis
            }
        }
        return ahora + 24L * 3600L * 1000L
    }

    private fun pending(ctx: Context, nombre: String): PendingIntent {
        val i = Intent(ctx, RutinaReceiver::class.java)
        i.data = Uri.parse("argon://rutina/" + Uri.encode(nombre))
        i.putExtra("rutina", nombre)
        return PendingIntent.getBroadcast(
            ctx, nombre.hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun programar(ctx: Context, nombre: String) {
        val a = cargar(ctx)
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            if (o.optString("n") != nombre) continue
            val dias = HashSet<Int>()
            val d = o.optJSONArray("d")
            if (d != null) for (k in 0 until d.length()) dias.add(d.getInt(k))
            val cuando = proximo(o.getInt("h"), o.getInt("m"), dias)
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val mostrar = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(cuando, mostrar), pending(ctx, nombre))
            return
        }
    }

    fun programarTodas(ctx: Context) {
        val a = cargar(ctx)
        for (i in 0 until a.length()) programar(ctx, a.getJSONObject(i).optString("n"))
    }

    fun agregar(ctx: Context, nombre: String, h: Int, m: Int, dias: Set<Int>) {
        quitar(ctx, nombre)
        val a = cargar(ctx)
        val d = JSONArray()
        for (x in dias) d.put(x)
        a.put(JSONObject().put("n", nombre).put("h", h).put("m", m).put("d", d))
        guardar(ctx, a)
        programar(ctx, nombre)
    }

    fun quitar(ctx: Context, nombre: String): Boolean {
        val a = cargar(ctx)
        val nuevo = JSONArray()
        var quitada = false
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            if (o.optString("n") == nombre) quitada = true else nuevo.put(o)
        }
        if (quitada) {
            guardar(ctx, nuevo)
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(pending(ctx, nombre))
        }
        return quitada
    }

    fun renombrar(ctx: Context, viejo: String, nuevo: String) {
        val a = cargar(ctx)
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            if (o.optString("n") == viejo) {
                val d = HashSet<Int>()
                val arr = o.optJSONArray("d")
                if (arr != null) for (k in 0 until arr.length()) d.add(arr.getInt(k))
                val h = o.getInt("h")
                val m = o.getInt("m")
                quitar(ctx, viejo)
                agregar(ctx, nuevo, h, m, d)
                return
            }
        }
    }
}
