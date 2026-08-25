package com.igris.assistant.core

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs

data class DeviceProfile(
    val totalRamMb: Long,
    val freeStorageMb: Long,
    val batteryPct: Int,
    val isCharging: Boolean,
    val thermalStatus: String,
    val cpuCores: Int,
    val sdkInt: Int,
    val manufacturer: String,
    val model: String,
)

class DeviceInspector(private val context: Context) {

    fun snapshot(): DeviceProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        val totalRamMb = mem.totalMem / (1024 * 1024)

        val stat = StatFs(Environment.getDataDirectory().path)
        val freeMb = stat.availableBlocksLong * stat.blockSizeLong / (1024 * 1024)

        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else -1
        val charging = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ==
            BatteryManager.BATTERY_STATUS_CHARGING

        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> "normal"
                PowerManager.THERMAL_STATUS_LIGHT -> "light"
                PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
                else -> "normal"
            }
        } else "unknown"

        return DeviceProfile(
            totalRamMb = totalRamMb,
            freeStorageMb = freeMb,
            batteryPct = pct,
            isCharging = charging,
            thermalStatus = thermal,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            sdkInt = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
        )
    }

    /** Adaptive model selection by device capability (spec §6 / §49). */
    fun recommendedTier(p: DeviceProfile = snapshot()): ModelTier = when {
        p.totalRamMb >= 11_500 -> ModelTier.LARGE
        p.totalRamMb >= 7_500 -> ModelTier.MEDIUM
        p.totalRamMb >= 3_500 -> ModelTier.SMALL
        else -> ModelTier.TINY
    }
}
