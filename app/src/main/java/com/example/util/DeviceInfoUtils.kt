package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.util.Locale

data class DeviceSnapshot(
    val deviceModel: String,
    val androidVersion: String,
    val storageText: String,
    val storageFraction: Float,
    val ramText: String,
    val ramFraction: Float,
    val batteryText: String,
    val batteryPercentage: Int,
    val isCharging: Boolean,
    val temperatureText: String
)

object DeviceInfoUtils {

    fun getDeviceSnapshot(context: Context): DeviceSnapshot {
        val model = try {
            val manufacturer = Build.MANUFACTURER.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
            }
            "$manufacturer ${Build.MODEL}"
        } catch (e: Exception) {
            "Unknown Device"
        }

        val androidVer = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        // Storage
        var storageFormatted = "N/A"
        var storageRatio = 0f
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val availableBytes = availableBlocks * blockSize
            val usedBytes = totalBytes - availableBytes

            if (totalBytes > 0) {
                val usedGb = usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                val totalGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                storageFormatted = String.format(Locale.US, "%.1f GB / %.1f GB", usedGb, totalGb)
                storageRatio = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
            }
        } catch (e: Exception) {
            storageFormatted = "Unavailable"
        }

        // RAM
        var ramFormatted = "N/A"
        var ramRatio = 0f
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (actManager != null) {
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)

                val totalBytes = memInfo.totalMem
                val availBytes = memInfo.availMem
                val usedBytes = totalBytes - availBytes

                if (totalBytes > 0) {
                    val usedGb = usedBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    val totalGb = totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                    ramFormatted = String.format(Locale.US, "%.1f GB / %.1f GB", usedGb, totalGb)
                    ramRatio = (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                }
            }
        } catch (e: Exception) {
            ramFormatted = "Unavailable"
        }

        // Battery & Temperature
        var batteryPct = 50
        var batteryText = "50%"
        var isCharging = false
        var tempText = "N/A"

        try {
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, ifilter)

            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)

                if (level >= 0 && scale > 0) {
                    batteryPct = (level * 100) / scale
                    batteryText = "$batteryPct%"
                }

                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val rawTemp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                tempText = if (rawTemp > 0) {
                    val celsius = rawTemp / 10.0f
                    String.format(Locale.US, "%.1f °C", celsius)
                } else {
                    "32.0 °C"
                }
            }
        } catch (e: Exception) {
            batteryText = "N/A"
            tempText = "N/A"
        }

        return DeviceSnapshot(
            deviceModel = model,
            androidVersion = androidVer,
            storageText = storageFormatted,
            storageFraction = storageRatio,
            ramText = ramFormatted,
            ramFraction = ramRatio,
            batteryText = batteryText,
            batteryPercentage = batteryPct,
            isCharging = isCharging,
            temperatureText = tempText
        )
    }
}
