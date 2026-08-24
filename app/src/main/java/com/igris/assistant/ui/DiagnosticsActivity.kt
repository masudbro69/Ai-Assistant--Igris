package com.igris.assistant.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.igris.assistant.IgrisApp
import com.igris.assistant.core.Connectivity

class DiagnosticsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val loc = (application as IgrisApp).locator
        val ui = PanelUi(this)
        ui.install()
        ui.title("Self Diagnostics")

        val p = loc.device.snapshot()
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val online = Connectivity.isOnline(this)
        val stt = SpeechRecognizer.isRecognitionAvailable(this)
        val tier = loc.device.recommendedTier(p)

        ui.text("AI model available: ✓ (rule engine) ${if (online && loc.settings.cloudProvider != "none") "+ cloud" else ""}")
        ui.text("Microphone permission: ${if (mic) "✓" else "✗ (grant to use voice)"}")
        ui.text("Speech recognition: ${if (stt) "✓" else "✗"}")
        ui.text("TTS: initializing…")
        ui.text("Storage: ${p.freeStorageMb} MB free")
        ui.text("Battery: ${p.batteryPct}% ${if (p.isCharging) "(charging)" else ""}")
        ui.text("Thermal: ${p.thermalStatus}")
        ui.text("RAM: ${p.totalRamMb} MB → model tier: $tier")
        ui.text("Internet: ${if (online) "✓ connected" else "✗ offline (offline mode active)"}")
        ui.text("Offline Fortress: ${if (loc.settings.offlineFortress) "ON" else "OFF"}")
        ui.divider()

        TextToSpeech(this) { status ->
            runOnUiThread {
                ui.text("TTS: ${if (status == TextToSpeech.SUCCESS) "✓ ready" else "✗ unavailable"}")
            }
        }
    }
}
