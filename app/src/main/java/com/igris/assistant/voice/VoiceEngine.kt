package com.igris.assistant.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Live, interruptible voice (spec §18, §19). STT uses the on-device recognizer when a
 * language pack is present; TTS supports barge-in via [interrupt].
 */
class VoiceEngine(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    fun initTts(onReady: (Boolean) -> Unit = {}) {
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            onReady(ttsReady)
        }
    }

    fun sttAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun listen(onPartial: (String) -> Unit, onFinal: (String) -> Unit, onEnd: () -> Unit, language: String = "en-US") {
        stopListening()
        if (!sttAvailable()) { onEnd(); return }
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { onEnd() }
            override fun onError(code: Int) { onEnd() }
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                onFinal(list?.firstOrNull() ?: "")
            }
            override fun onPartialResults(p: Bundle?) {
                val list = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                onPartial(list?.firstOrNull() ?: "")
            }
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        r.startListening(intent)
    }

    fun stopListening() {
        runCatching { recognizer?.stopListening(); recognizer?.destroy() }
        recognizer = null
    }

    fun speak(text: String, langIso: String = "en-US") {
        if (!ttsReady) return
        tts?.language = Locale.forLanguageTag(langIso) ?: Locale.US
        tts?.speak(cleanForTts(text), TextToSpeech.QUEUE_FLUSH, null, "igris")
    }

    /** Barge-in (spec §19): stop speaking immediately. */
    fun interrupt() { runCatching { tts?.stop() } }

    fun shutdown() {
        stopListening()
        runCatching { tts?.stop(); tts?.shutdown() }
    }

    private fun cleanForTts(text: String) = text
        .replace(Regex("[•■○]"), "")
        .replace(Regex("\n{2,}"), ". ")
        .take(600)
}
