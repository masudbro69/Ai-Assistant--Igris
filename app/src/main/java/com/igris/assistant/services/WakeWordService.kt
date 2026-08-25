package com.igris.assistant.services

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.igris.assistant.R
import com.igris.assistant.ui.OverlayActivity

/**
 * "IGRIS" wake-word (spec §18 live voice). A lightweight foreground listener that pops the
 * IGRIS overlay the moment it hears "IGRIS" — no need to open the app or type.
 */
class WakeWordService : Service() {

    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var active = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stop()
            stopSelf()
        } else {
            start()
        }
        return START_STICKY
    }

    private fun start() {
        if (active) return
        active = true
        NotificationHelper.ensureChannels(this)
        val n = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ACTIONS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("IGRIS")
            .setContentText("Wake word active — say “IGRIS”")
            .setOngoing(true)
            .setSilent(true)
            .build()
        try {
            if (Build.VERSION.SDK_INT >= 29)
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            else
                startForeground(NOTIF_ID, n)
        } catch (_: Exception) {
            startForeground(NOTIF_ID, n)
        }
        listenLoop()
    }

    private fun stop() {
        active = false
        runCatching { recognizer?.stopListening(); recognizer?.destroy() }
        recognizer = null
        stopForeground(true)
    }

    private fun listenLoop() {
        if (!active) return
        if (overlayActive) { handler.postDelayed({ listenLoop() }, 1000); return }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            handler.postDelayed({ listenLoop() }, 3000); return
        }
        val r = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer = r
        val it = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() = scheduleRestart()
            override fun onError(code: Int) = scheduleRestart()
            override fun onResults(res: android.os.Bundle?) {
                val t = res?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (isWake(t)) onWake() else scheduleRestart()
            }
            override fun onPartialResults(p: android.os.Bundle?) {
                val t = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                if (isWake(t)) onWake()
            }
            override fun onEvent(t: Int, p: android.os.Bundle?) {}
        })
        try { r.startListening(it) } catch (_: Exception) { scheduleRestart() }
    }

    private fun isWake(t: String): Boolean {
        val s = t.lowercase()
        return s.contains("igris") || s.contains("ইগ্রিস") || s.contains("eagris")
    }

    private fun scheduleRestart() {
        runCatching { recognizer?.destroy() }
        recognizer = null
        handler.postDelayed({ if (active) listenLoop() }, 700)
    }

    private fun onWake() {
        runCatching { recognizer?.stopListening(); recognizer?.destroy() }
        recognizer = null
        runCatching {
            val vib = if (Build.VERSION.SDK_INT >= 31)
                (getSystemService(VibratorManager::class.java)?.defaultVibrator)
            else @Suppress("DEPRECATION") (getSystemService(VIBRATOR_SERVICE) as Vibrator)
            vib?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        startActivity(Intent(this, OverlayActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        handler.postDelayed({ if (active) listenLoop() }, 2000)
    }

    override fun onDestroy() {
        stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "igris.wake.STOP"
        private const val NOTIF_ID = 911

        @Volatile
        var overlayActive = false
    }
}
