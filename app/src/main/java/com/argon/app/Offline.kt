package com.argon.app

import org.json.JSONObject
import java.text.Normalizer

object Offline {

    private fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    // Resuelve órdenes simples sin modelo. Todas las herramientas pasan por `ejecutar`,
    // que respeta las conexiones activas y las confirmaciones de permisos.
    fun comandoSimple(texto: String, ejecutar: (String, JSONObject) -> String): String? {
        val t = norm(texto)
        val corto = t.trim().split(Regex("\\s+")).size <= 8

        if (corto && t.contains("bateria")) {
            return ejecutar("ver_bateria", JSONObject())
        }
        if (corto && (t.contains("ubicacion") || t.contains("donde estoy"))) {
            return ejecutar("ver_ubicacion", JSONObject())
        }

        val dur = Regex("(\\d{1,6})\\s*(segundos?|seg|minutos?|min|horas?|h)\\b").find(t)
        val pideTimer = t.contains("temporizador") || t.contains("cronometro") ||
            t.contains("timer") || (t.contains("alarma") && t.contains(" en ") && dur != null)
        if (pideTimer && dur != null) {
            val n = dur.groupValues[1].toIntOrNull() ?: return null
            val unidad = dur.groupValues[2]
            val seg = when {
                unidad.startsWith("h") -> n * 3600
                unidad.startsWith("m") -> n * 60
                else -> n
            }
            return ejecutar("poner_temporizador", JSONObject().put("segundos", seg).put("mensaje", "Argón"))
        }

        if (t.contains("alarma")) {
            val m = Regex("(?:a\\s+las?|las?)\\s+(\\d{1,2})(?:[:.](\\d{2}))?").find(t)
                ?: Regex("\\b(\\d{1,2}):(\\d{2})\\b").find(t)
            if (m != null) {
                var hora = m.groupValues[1].toIntOrNull() ?: return null
                val min = m.groupValues[2].ifEmpty { "0" }.toIntOrNull() ?: return null
                val tarde = t.contains("tarde") || t.contains("noche") ||
                    Regex("\\bp\\.?m\\b").containsMatchIn(t)
                if (tarde && hora in 1..11) hora += 12
                if (t.contains("noche") && hora == 12) hora = 0
                if (hora in 0..23 && min in 0..59) {
                    return ejecutar(
                        "poner_alarma",
                        JSONObject().put("hora", hora).put("minutos", min).put("mensaje", "Argón")
                    )
                }
            }
        }

        if (corto) {
            val c = Regex("(?i)^(?:dame\\s+|busca\\s+)?(?:el\\s+)?(?:contacto|n[uú]mero de|tel[eé]fono de)\\s+(.+)$")
                .find(texto.trim().trimEnd('.', '?', '!'))
            if (c != null) {
                return ejecutar("leer_contactos", JSONObject().put("nombre", c.groupValues[1].trim()))
            }
        }

        return null
    }
}
