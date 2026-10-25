package com.argon.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ResultadoApi(
    val texto: String?,
    val error: String?,
    val modelo: String?,
    val herramientas: List<String>
)

object Api {

    private class RespApi(val content: JSONObject?, val error: String?, val modelo: String?)

    fun llamar(
        clave: String,
        modelos: List<String>,
        sistema: String,
        contenido: JSONArray,
        tools: JSONArray,
        ejecutar: (String, JSONObject) -> String,
        aviso: (String) -> Unit
    ): ResultadoApi {
        val trabajo = JSONArray()
        for (i in 0 until contenido.length()) trabajo.put(contenido.get(i))
        val usadas = ArrayList<String>()
        var modeloUsado: String? = null

        for (ronda in 0 until 6) {
            val r = pedir(clave, modelos, sistema, trabajo, tools)
            if (r.error != null) return ResultadoApi(null, r.error, r.modelo, usadas)
            modeloUsado = r.modelo
            val content = r.content ?: JSONObject()
            val partes = content.optJSONArray("parts") ?: JSONArray()
            val llamadas = ArrayList<JSONObject>()
            val sb = StringBuilder()
            for (k in 0 until partes.length()) {
                val p = partes.getJSONObject(k)
                if (p.has("functionCall")) {
                    llamadas.add(p.getJSONObject("functionCall"))
                } else {
                    sb.append(p.optString("text", ""))
                }
            }
            if (llamadas.isEmpty()) {
                val texto = sb.toString().trim()
                if (texto.isEmpty()) {
                    return ResultadoApi(
                        null, "Argón no devolvió texto (Google pudo haberlo bloqueado).", modeloUsado, usadas
                    )
                }
                return ResultadoApi(texto, null, modeloUsado, usadas)
            }

            trabajo.put(content)
            val respuestas = JSONArray()
            for (c in llamadas) {
                val nombre = c.getString("name")
                aviso(nombre)
                usadas.add(nombre)
                val salida = ejecutar(nombre, c.optJSONObject("args") ?: JSONObject())
                respuestas.put(
                    JSONObject().put(
                        "functionResponse",
                        JSONObject().put("name", nombre)
                            .put("response", JSONObject().put("result", salida))
                    )
                )
            }
            trabajo.put(JSONObject().put("role", "user").put("parts", respuestas))
        }
        return ResultadoApi(
            null, "Argón usó demasiadas herramientas seguidas. Intenta de nuevo.", modeloUsado, usadas
        )
    }

    private fun pedir(
        clave: String, modelos: List<String>, sistema: String,
        contenido: JSONArray, tools: JSONArray
    ): RespApi {
        var ultimoError = "Error desconocido"
        for (modelo in modelos) {
            for (intento in 0 until 2) {
                try {
                    val cuerpo = JSONObject()
                        .put(
                            "system_instruction",
                            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", sistema)))
                        )
                        .put("contents", contenido)
                        .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", tools)))
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
                        val content = cand.optJSONObject("content")
                            ?: JSONObject().put("role", "model").put("parts", JSONArray())
                        return RespApi(content, null, modelo)
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
                    return RespApi(null, "Error $codigo: $msg", modelo)
                } catch (e: Exception) {
                    ultimoError = "Sin conexión o error de red: ${e.message}"
                    break
                }
            }
        }
        return RespApi(null, ultimoError, null)
    }
}
