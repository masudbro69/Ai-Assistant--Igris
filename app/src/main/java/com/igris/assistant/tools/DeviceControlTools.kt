package com.igris.assistant.tools

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.igris.assistant.R
import com.igris.assistant.core.TaskClass
import com.igris.assistant.core.ToolResult
import com.igris.assistant.core.UnderstoodInput
import com.igris.assistant.policy.RiskLevel

/** Wi-Fi on/off (below API 29) or opens the Wi-Fi panel on modern Android. */
class WifiTool : Tool {
    override val id = "wifi"
    override val name = "Wi-Fi control"
    override val description = "Toggle Wi-Fi or open Wi-Fi settings."
    override val risk = RiskLevel.LOW

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.DEVICE_CONTROL &&
        (i.raw.contains("wifi") || i.raw.contains("ওয়াইফাই"))

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val on = !(i.raw.contains("off") || i.raw.contains("bondho") || i.raw.contains("বন্ধ"))
        val wm = ctx.android.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return ToolResult(false, "Wi-Fi service unavailable.")
        return try {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT < 29) {
                wm.setWifiEnabled(on)
                ToolResult(true, if (on) "Wi-Fi ON." else "Wi-Fi OFF.", speak = if (on) "Wi-Fi on." else "Wi-Fi off.")
            } else {
                ctx.android.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                ToolResult(true, "Android ${Build.VERSION.SDK_INT} needs manual Wi-Fi toggle — I opened Wi-Fi settings for you.")
            }
        } catch (e: Exception) {
            ToolResult(false, "Couldn't change Wi-Fi: ${e.message}")
        }
    }
}

/** Bluetooth toggle with graceful fallback to settings. */
class BluetoothTool : Tool {
    override val id = "bluetooth"
    override val name = "Bluetooth control"
    override val description = "Toggle Bluetooth or open its settings."
    override val risk = RiskLevel.LOW

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.DEVICE_CONTROL &&
        (i.raw.contains("bluetooth") || i.raw.contains("ব্লুটুথ"))

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val on = !(i.raw.contains("off") || i.raw.contains("bondho") || i.raw.contains("বন্ধ"))
        val ba = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
            ?: return ToolResult(false, "No Bluetooth on this device.")
        return try {
            if (Build.VERSION.SDK_INT < 31) {
                @Suppress("DEPRECATION")
                if (on) ba.enable() else ba.disable()
                ToolResult(true, if (on) "Bluetooth ON." else "Bluetooth OFF.")
            } else {
                ctx.android.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                ToolResult(true, "Android 12+ needs manual Bluetooth toggle — settings opened.")
            }
        } catch (e: Exception) {
            ToolResult(false, "Couldn't change Bluetooth: ${e.message}")
        }
    }
}

/** Screen brightness via system settings (needs Write Settings grant). */
class BrightnessTool : Tool {
    override val id = "brightness"
    override val name = "Brightness control"
    override val description = "Set screen brightness (0-100%)."
    override val risk = RiskLevel.LOW

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.DEVICE_CONTROL &&
        (i.raw.contains("brightness") || i.raw.contains("brightness") || i.raw.contains("উজ্জ্বলতা"))

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        if (!Settings.System.canWrite(ctx.android)) {
            runCatching {
                ctx.android.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    android.net.Uri.parse("package:${ctx.android.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            return ToolResult(true, "Grant “Modify system settings” once, then ask me again — I'll set brightness for you.")
        }
        val pct = i.numbers.firstOrNull()?.coerceIn(0.0, 100.0) ?: 50.0
        val v = (pct / 100.0 * 255).toInt()
        Settings.System.putInt(ctx.android.contentResolver, Settings.System.SCREEN_BRIGHTNESS, v)
        return ToolResult(true, "Brightness set to ${pct.toInt()}%.", speak = "Brightness set.")
    }
}

/** Sets an IGRIS artwork as the device wallpaper. */
class WallpaperTool : Tool {
    override val id = "wallpaper"
    override val name = "IGRIS wallpaper"
    override val description = "Apply an IGRIS artwork as wallpaper."
    override val risk = RiskLevel.MEDIUM

    override fun matches(i: UnderstoodInput) = i.task == TaskClass.DEVICE_CONTROL &&
        (i.raw.contains("wallpaper") || i.raw.contains("ওয়ালপেপার"))

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val res = when {
            i.raw.contains("2") || i.raw.contains("dui") -> R.drawable.igris_art_2
            i.raw.contains("3") || i.raw.contains("tin") -> R.drawable.igris_art_3
            else -> R.drawable.igris_art_1
        }
        return try {
            val bmp = BitmapFactory.decodeResource(ctx.android.resources, res)
            android.app.WallpaperManager.getInstance(ctx.android).setBitmap(bmp)
            ToolResult(true, "IGRIS wallpaper applied. 🗡️", speak = "Wallpaper applied.")
        } catch (e: Exception) {
            ToolResult(false, "Couldn't set wallpaper: ${e.message}")
        }
    }
}

/** Kills another app's background processes (Android-sanctioned kill only). */
class KillAppTool : Tool {
    override val id = "kill-app"
    override val name = "Close app"
    override val description = "Close (kill background) another app."
    override val risk = RiskLevel.MEDIUM

    override fun matches(i: UnderstoodInput) =
        i.raw.contains("close ") || i.raw.contains("kill ") || i.raw.contains("bondho kore de")

    override suspend fun execute(i: UnderstoodInput, ctx: ToolContext): ToolResult {
        val wanted = i.raw.replace(Regex("(?i)\\b(close|kill|please|app|bondho kore de)\\b"), "").trim()
        val pm = ctx.android.packageManager
        val main = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val hit = pm.queryIntentActivities(main, 0).mapNotNull { it.activityInfo }.distinctBy { it.packageName }
            .firstOrNull { ai ->
                val label = pm.getApplicationLabel(ai.applicationInfo).toString().lowercase()
                wanted.split(" ").filter { it.length > 2 }.any { label.contains(it) || ai.packageName.lowercase().contains(it) }
            } ?: return ToolResult(false, "Couldn't find an app matching “$wanted”.")
        val am = ctx.android.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        am.killBackgroundProcesses(hit.packageName)
        val label = pm.getApplicationLabel(hit.applicationInfo)
        return ToolResult(true, "Closed $label (background processes killed).", speak = "Closed $label.")
    }
}
