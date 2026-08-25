package com.igris.assistant.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.igris.assistant.IgrisApp
import com.igris.assistant.R
import com.igris.assistant.brain.IgrisBrain
import com.igris.assistant.services.WakeWordService
import com.igris.assistant.voice.VoiceEngine
import kotlinx.coroutines.launch

/**
 * The "IGRIS" popup. Appears over anything when the wake word is heard; speak a command and
 * it executes + answers with the deep voice. Ultra-minimal, animated, professional.
 */
class OverlayActivity : AppCompatActivity() {

    private lateinit var brain: IgrisBrain
    private lateinit var voice: VoiceEngine
    private lateinit var status: TextView
    private lateinit var reply: TextView
    private lateinit var pulse: PulseRingView
    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WakeWordService.overlayActive = true
        val loc = (application as IgrisApp).locator
        brain = IgrisBrain(loc)
        voice = VoiceEngine(this).also { it.initTts() }

        val root = FrameLayout(this)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(24), dp(26), dp(20))
            setBackgroundResource(R.drawable.popup_card)
        }
        val cardLp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            setMargins(dp(18), dp(18), dp(18), dp(22))
        }
        root.addView(card, cardLp)

        // emblem + pulse aura
        pulse = PulseRingView(this)
        val emblem = ImageView(this).apply {
            setImageResource(R.drawable.igris_emblem)
            scaleType = ImageView.ScaleType.CENTER_INSIDE
        }
        val ring = FrameLayout(this)
        ring.addView(pulse, FrameLayout.LayoutParams(dp(168), dp(168)))
        ring.addView(emblem, FrameLayout.LayoutParams(dp(118), dp(118)).apply { gravity = Gravity.CENTER })
        card.addView(ring)

        val title = TextView(this).apply {
            text = "IGRIS"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 6f
            setTextColor(0xFF39D3F5.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, 0)
        }
        card.addView(title)

        status = TextView(this).apply {
            text = "Listening…"
            textSize = 13f
            setTextColor(ContextCompat.getColor(this@OverlayActivity, R.color.igris_text_dim))
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, dp(8))
        }
        card.addView(status)

        reply = TextView(this).apply {
            textSize = 15f
            setTextColor(ContextCompat.getColor(this@OverlayActivity, R.color.igris_text))
            maxLines = 7
            movementMethod = ScrollingMovementMethod()
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(10))
        }
        card.addView(reply)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val mic = ImageButton(this).apply {
            setBackgroundResource(R.drawable.popup_mic_bg)
            setImageResource(R.drawable.ic_mic)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setOnClickListener { toggleListen() }
        }
        val close = Button(this).apply {
            text = "✕"
            setTextColor(0xFF93A4C3.toInt())
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { finish() }
        }
        row.addView(mic, LinearLayout.LayoutParams(dp(56), dp(56)))
        row.addView(close)
        card.addView(row)

        setContentView(root)

        // entrance animation
        card.translationY = dp(320).toFloat()
        card.alpha = 0f
        card.animate().translationY(0f).alpha(1f).setDuration(380)
            .withEndAction { startListening() }.start()
    }

    private fun dp(v: Int): Int = (resources.displayMetrics.density * v).toInt()

    private fun toggleListen() {
        if (listening) stopListening() else startListening()
    }

    private fun startListening() {
        stopListening()
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Voice unavailable on this device"
            return
        }
        listening = true
        pulse.setPulsing(true)
        status.text = "Listening… say your command"
        val r = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer = r
        val it = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?) { status.text = "Listening…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() { listening = false; pulse.setPulsing(false) }
            override fun onError(code: Int) {
                listening = false; pulse.setPulsing(false)
                status.text = "Didn't catch that — tap the mic"
            }
            override fun onResults(res: Bundle?) {
                listening = false; pulse.setPulsing(false)
                val t = res?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (t.isNotBlank()) handle(t)
            }
            override fun onPartialResults(p: Bundle?) {
                val t = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (t.isNotBlank()) status.text = "“$t”"
            }
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        try { r.startListening(it) } catch (_: Exception) { listening = false; pulse.setPulsing(false) }
    }

    private fun stopListening() {
        listening = false
        pulse.setPulsing(false)
        runCatching { recognizer?.stopListening(); recognizer?.destroy() }
        recognizer = null
    }

    private fun handle(text: String) {
        val t = text.trim()
        if (t.lowercase().let { it.contains("stop") || it.contains("bandho") || it.contains("থামো") }) {
            voice.interrupt()
            status.text = "Stopped."
            return
        }
        status.text = "Thinking…"
        reply.text = "“$t”"
        (application as IgrisApp).locator.conversations.log("user", t)
        lifecycleScope.launch {
            val res = brain.handle(t)
            reply.text = res.reply
            (application as IgrisApp).locator.conversations.log("igris", res.reply)
            status.text = "Listening again… (continuous conversation)"
            if ((application as IgrisApp).locator.settings.speakResponses) {
                val iso = if (res.speak.orEmpty().any { it.code in 0x0980..0x09FF }) "bn-BD" else "en-US"
                voice.speak(res.speak ?: res.reply, iso) { startListening() }
            } else {
                startListening()
            }
        }
    }

    override fun onDestroy() {
        stopListening()
        voice.shutdown()
        WakeWordService.overlayActive = false
        super.onDestroy()
    }
}
