package com.argon.app

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class Local(private val ctx: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var primera = true

    fun archivo(): File = File(ctx.filesDir, "modelo.litertlm")

    fun hayModelo(): Boolean = archivo().exists() && archivo().length() > 1_000_000L

    fun preguntar(texto: String, sistema: String): Pair<String?, String?> {
        return try {
            executor.submit(Callable { preguntarInterno(texto, sistema) }).get()
        } catch (e: Exception) {
            Pair(null, "Error del modelo sin conexión: ${e.message}")
        }
    }

    private fun preguntarInterno(texto: String, sistema: String): Pair<String?, String?> {
        try {
            if (conversation == null) {
                liberarInterno()
                val config = EngineConfig(
                    modelPath = archivo().absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = ctx.cacheDir.path
                )
                val e = Engine(config)
                e.initialize()
                engine = e
                conversation = e.createConversation()
                primera = true
            }
            val c = conversation ?: return Pair(null, "No pude iniciar el modelo")
            val mensaje = if (primera) {
                "Instrucciones para ti: $sistema\n\nMensaje de rafachafa: $texto"
            } else {
                texto
            }
            val respuesta = c.sendMessage(mensaje).toString()
            primera = false
            val limpia = Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL)
                .replace(respuesta, "").trim()
            return if (limpia.isEmpty()) Pair(null, "El modelo no dio respuesta") else Pair(limpia, null)
        } catch (e: Throwable) {
            liberarInterno()
            return Pair(null, "Error del modelo sin conexión: ${e.message}")
        }
    }

    private fun liberarInterno() {
        try {
            conversation?.close()
        } catch (e: Throwable) {
        }
        conversation = null
        try {
            engine?.close()
        } catch (e: Throwable) {
        }
        engine = null
    }

    fun liberar() {
        executor.execute { liberarInterno() }
    }
}
