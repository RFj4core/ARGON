package com.argon.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class Voz(private val act: Activity) {

    private var tts: TextToSpeech? = null
    private var ttsListo = false
    private var alTerminarHablar: (() -> Unit)? = null
    private var reco: SpeechRecognizer? = null
    private val ui = Handler(Looper.getMainLooper())

    private fun idioma(): Locale {
        val l = Locale.getDefault()
        return if (l.language == "es") l else Locale("es", "ES")
    }

    init {
        tts = TextToSpeech(act.applicationContext) { estado ->
            val t = tts
            if (estado == TextToSpeech.SUCCESS && t != null) {
                t.setLanguage(idioma())
                t.setPitch(0.88f)
                t.setSpeechRate(1.0f)
                t.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        terminarHablar()
                    }
                    override fun onError(utteranceId: String?) {
                        terminarHablar()
                    }
                })
                ttsListo = true
            }
        }
    }

    private fun terminarHablar() {
        ui.post {
            val cb = alTerminarHablar
            alTerminarHablar = null
            cb?.invoke()
        }
    }

    private fun limpiarParaVoz(t: String): String {
        var s = Regex("```[\\s\\S]*?```").replace(t, " bloque de código. ")
        s = Regex("https?://\\S+").replace(s, " enlace ")
        s = s.replace(Regex("[*_`#>|~]+"), "")
        s = Regex("^\\s*[-•]\\s+", RegexOption.MULTILINE).replace(s, "")
        s = s.replace(Regex("\\s+"), " ").trim()
        return s.take(3500)
    }

    fun hablar(texto: String, alTerminar: () -> Unit) {
        val t = tts
        val limpio = limpiarParaVoz(texto)
        if (t == null || !ttsListo || limpio.isEmpty()) {
            alTerminar()
            return
        }
        alTerminarHablar = alTerminar
        t.speak(limpio, TextToSpeech.QUEUE_FLUSH, null, "argon")
    }

    fun parar() {
        alTerminarHablar = null
        tts?.stop()
    }

    fun disponibleEscuchar(): Boolean = SpeechRecognizer.isRecognitionAvailable(act)

    fun empezarEscuchar(
        alParcial: (String) -> Unit,
        alFinal: (String?) -> Unit,
        alNivel: (Float) -> Unit
    ) {
        cancelarEscuchar()
        val r = SpeechRecognizer.createSpeechRecognizer(act)
        reco = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                alNivel(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                liberarReco()
                alFinal(null)
            }
            override fun onResults(results: Bundle?) {
                val l = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                liberarReco()
                alFinal(l?.firstOrNull())
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val l = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val t = l?.firstOrNull()
                if (!t.isNullOrBlank()) alParcial(t)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, idioma().toLanguageTag())
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        r.startListening(i)
    }

    fun terminarEscuchar() {
        reco?.stopListening()
    }

    private fun liberarReco() {
        val r = reco
        reco = null
        if (r != null) {
            ui.post {
                try {
                    r.destroy()
                } catch (e: Exception) {
                }
            }
        }
    }

    fun cancelarEscuchar() {
        val r = reco
        reco = null
        if (r != null) {
            try {
                r.cancel()
                r.destroy()
            } catch (e: Exception) {
            }
        }
    }

    fun apagar() {
        cancelarEscuchar()
        tts?.stop()
        tts?.shutdown()
    }
}
