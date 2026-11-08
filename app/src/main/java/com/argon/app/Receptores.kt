package com.argon.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

class RutinaReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val nombre = intent.getStringExtra("rutina") ?: return
        if (!Programador.existe(ctx, nombre)) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel("argon_rutinas", "Rutinas de Argón", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val abrir = Intent(ctx, MainActivity::class.java)
            .putExtra("rutina", nombre)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            ctx, nombre.hashCode(), abrir, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(ctx, "argon_rutinas")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Rutina: $nombre")
            .setContentText("Toca para que Argón la ejecute")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        val permitido = Build.VERSION.SDK_INT < 33 ||
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (permitido) nm.notify(nombre.hashCode(), n)
        Programador.programar(ctx, nombre)
    }
}

class ArranqueReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Programador.programarTodas(ctx)
    }
}
