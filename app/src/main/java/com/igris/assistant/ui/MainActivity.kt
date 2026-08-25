package com.igris.assistant.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.igris.assistant.IgrisApp
import com.igris.assistant.R
import com.igris.assistant.brain.IgrisBrain
import com.igris.assistant.core.BrainResponse
import com.igris.assistant.databinding.ActivityMainBinding
import com.igris.assistant.util.LanguageDetector
import com.igris.assistant.util.Lang
import com.igris.assistant.voice.VoiceEngine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var loc: com.igris.assistant.ServiceLocator
    private lateinit var brain: IgrisBrain
    private lateinit var voice: VoiceEngine
    private val adapter = MessageAdapter()
    private var pendingConfirm: String? = null
    private var lastText: String? = null
    private var listening = false

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loc = (application as IgrisApp).locator
        brain = IgrisBrain(loc)
        voice = VoiceEngine(this).also { it.initTts() }

        binding.chatList.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.chatList.adapter = adapter

        addBot(getString(R.string.welcome), meta = "offline-first • multilingual • private")
        updateBadge("ROUTE: rule-engine", false)

        buildNavRow()

        binding.btnSend.setOnClickListener { submit(binding.editInput.text.toString()) }
        binding.btnVoice.setOnClickListener { toggleVoice() }
        binding.btnConfirm.setOnClickListener {
            val p = pendingConfirm
            hideConfirm()
            if (p != null) runBrain(p, confirmed = true)
        }
        binding.btnCancel.setOnClickListener {
            hideConfirm()
            addBot("Action cancelled.", meta = "policy")
        }

        requestBasePermissions()

        if (loc.settings.wakeWordEnabled) {
            com.igris.assistant.services.WakeControl.start(this)
        }
        if (loc.settings.dailyBriefing) {
            com.igris.assistant.services.BriefingScheduler.scheduleDaily(this, 8)
        }
        handleShareIntent()
        startKenBurns()
    }

    /** Slow cinematic zoom on the IGRIS hero art. */
    private fun startKenBurns() {
        val v = binding.heroArt
        fun loop(forward: Boolean) {
            val target = if (forward) 1.1f else 1.0f
            v.animate().scaleX(target).scaleY(target).setDuration(7000).withEndAction { loop(!forward) }.start()
        }
        loop(true)
    }

    private fun handleShareIntent() {
        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> null
        }
        if (!text.isNullOrBlank()) submit(text)
    }

    private fun buildNavRow() {
        val targets = listOf(
            "Settings" to SettingsActivity::class.java,
            "Privacy" to PrivacyActivity::class.java,
            "Diagnostics" to DiagnosticsActivity::class.java,
            "Vault" to VaultActivity::class.java,
            "History" to HistoryActivity::class.java,
            "Routines" to RoutinesActivity::class.java,
            "Projects" to ProjectsActivity::class.java,
            "Plugins" to PluginsActivity::class.java,
        )
        binding.navRow.removeAllViews()
        targets.forEach { (label, cls) ->
            val tv = TextView(this).apply {
                text = label
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.igris_primary))
                setPadding(20, 10, 20, 10)
                textSize = 13f
                setOnClickListener { startActivity(Intent(this@MainActivity, cls)) }
            }
            binding.navRow.addView(tv)
        }
    }

    private fun requestBasePermissions() {
        val perms = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }

    private fun submit(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        binding.editInput.setText("")
        addUser(t)
        runBrain(t, confirmed = false)
    }

    private fun runBrain(text: String, confirmed: Boolean) {
        lastText = text
        lifecycleScope.launch {
            val res = brain.handle(text, confirmed)
            render(res)
        }
    }

    private fun render(res: BrainResponse) {
        val routeLabel = when (res.route) {
            com.igris.assistant.core.Route.FAST_INTENT -> "fast-intent"
            com.igris.assistant.core.Route.RULE_ENGINE -> "rule-engine"
            com.igris.assistant.core.Route.CLOUD -> "cloud"
            else -> "offline"
        }
        updateBadge("ROUTE: $routeLabel", res.needsOnline)
        if (res.confirmPending) {
            pendingConfirm = lastText
            addBot(res.reply, meta = "awaiting confirmation")
            showConfirm()
            return
        }
        addBot(res.reply, meta = routeLabel)
        if ((application as IgrisApp).locator.settings.speakResponses) {
            val iso = if (res.speak.orEmpty().any { it.code in 0x0980..0x09FF }) "bn-BD" else "en-US"
            voice.speak(res.speak ?: res.reply, iso)
        }
    }

    private fun toggleVoice() {
        if (listening) {
            voice.stopListening()
            listening = false
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
            return
        }
        listening = true
        voice.listen(
            onPartial = { },
            onFinal = { text ->
                listening = false
                if (text.isNotBlank()) {
                    // replace the listening placeholder
                    submit(text)
                }
            },
            onEnd = { listening = false },
        )
    }

    private fun addUser(t: String) {
        adapter.add(ChatMessage(Role.USER, t))
        loc.conversations.log("user", t)
    }
    private fun addBot(t: String, meta: String = "") {
        adapter.add(ChatMessage(Role.BOT, t, meta))
        loc.conversations.log("igris", t)
    }

    private fun showConfirm() {
        binding.confirmBar.visibility = android.view.View.VISIBLE
    }
    private fun hideConfirm() {
        binding.confirmBar.visibility = android.view.View.GONE
        pendingConfirm = null
    }

    private fun updateBadge(text: String, online: Boolean) {
        binding.routeBadge.text = if (online) "ONLINE" else text
    }

    override fun onDestroy() {
        voice.shutdown()
        super.onDestroy()
    }
}
