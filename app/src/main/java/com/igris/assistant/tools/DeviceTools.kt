package com.igris.assistant.tools

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import com.igris.assistant.core.TaskClass
import com.igris.assistant.core.ToolResult
import com.igris.assistant.core.UnderstoodInput
import com.igris.assistant.policy.RiskLevel

class FlashlightTool : Tool {
    override val id = "flashlight"
    override val name = "Flashlight"
    override val description = "Toggle the torch without waking a large model (spec §7)."
    override val risk = RiskLevel.NONE
    private var on = false

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.DEVICE_CONTROL &&
        (i.raw.contains("flash") || i.raw.contains("torch") || i.raw.contains("লাইট") || i.raw.contains("batir"))

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val enable = !(i.raw.contains("off") || i.raw.contains("bondho") || i.raw.contains("বন্ধ"))
        val cm = ctx.android.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return ToolResult(false, "Camera service unavailable.")
        val id = cm.cameraIdList.firstOrNull {
            cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return ToolResult(false, "This device has no torch.")
        return try {
            cm.setTorchMode(id, enable)
            on = enable
            ToolResult(true, if (enable) "Flashlight ON." else "Flashlight OFF.",
                speak = if (enable) "Flashlight on." else "Flashlight off.")
        } catch (e: Exception) {
            ToolResult(false, "Could not toggle torch: ${e.message}")
        }
    }
}

class VolumeTool : Tool {
    override val id = "volume"
    override val name = "Volume"
    override val description = "Set or mute media volume."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.DEVICE_CONTROL &&
        (i.raw.contains("volume") || i.raw.contains("shobdo") || i.raw.contains("আওয়াজ"))

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val am = ctx.android.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return when {
            i.raw.contains("mute") || i.raw.contains("nirb") || i.raw.contains("নিঃশব্দ") -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
                ToolResult(true, "Muted.", speak = "Muted.")
            }
            i.raw.contains("max") || i.raw.contains("full") -> {
                am.setStreamVolume(AudioManager.STREAM_MUSIC, max, 0)
                ToolResult(true, "Volume set to maximum.", speak = "Volume maxed.")
            }
            i.numbers.isNotEmpty() -> {
                val pct = i.numbers.first().coerceIn(0.0, 100.0)
                am.setStreamVolume(AudioManager.STREAM_MUSIC, (max * pct / 100).toInt(), 0)
                ToolResult(true, "Volume set to ${pct.toInt()}%.", speak = "Volume set.")
            }
            else -> {
                am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, 0)
                ToolResult(true, "Volume raised.", speak = "Volume up.")
            }
        }
    }
}

class DeviceInfoTool : Tool {
    override val id = "device-info"
    override val name = "Device info"
    override val description = "Battery, storage, RAM, model (spec §42)."
    override val risk = RiskLevel.NONE

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.SYSTEM_INFO

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val p = ctx.loc.device.snapshot()
        val tier = ctx.loc.device.recommendedTier(p)
        val reply = buildString {
            append("Device: ${p.manufacturer} ${p.model} (Android ${p.sdkInt})\n")
            append("RAM: ${p.totalRamMb} MB → suggested model tier: $tier\n")
            append("Free storage: ${p.freeStorageMb} MB\n")
            append("Battery: ${p.batteryPct}% ${if (p.isCharging) "(charging)" else ""}\n")
            append("Thermal: ${p.thermalStatus}, CPU cores: ${p.cpuCores}")
        }
        return ToolResult(true, reply)
    }
}

class AppLaunchTool : Tool {
    override val id = "app-launch"
    override val name = "App launcher"
    override val description = "Find and open an installed app by name."
    override val risk = RiskLevel.LOW

    override fun matches(i: UnderstoodInput) =
        i.raw.contains("open ") || i.raw.contains("launch ") || i.raw.contains("chalu koro") || i.raw.contains("খোলো")

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val wanted = i.raw
            .replace(Regex("(?i)\\b(open|launch|start|chalu koro|খোলো|please)\\b"), "")
            .trim()
        val pm = ctx.android.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0)
            .mapNotNull { it.activityInfo }
            .distinctBy { it.packageName }
        val hit = apps.firstOrNull { ai ->
            val label = pm.getApplicationLabel(ai.applicationInfo).toString().lowercase()
            val pkg = ai.packageName.lowercase()
            wanted.split(" ").filter { it.length > 2 }.any { label.contains(it) || pkg.contains(it) }
        } ?: return ToolResult(false, "Couldn't find an app matching “$wanted”.")
        val launch = pm.getLaunchIntentForPackage(hit.packageName)
            ?: return ToolResult(false, "No launch intent for ${hit.packageName}.")
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.android.startActivity(launch)
            val label = pm.getApplicationLabel(hit.applicationInfo)
            ToolResult(true, "Opening $label.", speak = "Opening $label.")
        } catch (e: Exception) {
            ToolResult(false, "Could not open app: ${e.message}")
        }
    }
}
