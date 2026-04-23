package com.hans.ext.kernelmanager.hal

import com.hans.ext.kernelmanager.hal.intelligence.SystemDiscovery
import com.hans.ext.kernelmanager.util.SmartShell
import android.util.Log

/**
 * HardwareMonitor (V12) - The Sovereign Intelligence Probe.
 * Dynamically identifies and normalizes complex hardware sensors like thermal zones,
 * system loads, and device-specific vitals without hardcoded assumptions.
 */
object HardwareMonitor {
    private const val TAG = "HardwareMonitor"

    /**
     * Resolves the most likely "Primary CPU" temperature sensor.
     * Searches for keywords in thermal zone types to avoid hardcoding "thermal_zone0".
     */
    fun getCpuTemp(): String {
        val registry = SystemDiscovery.getRegistry()
        val zones = registry.thermalZones
        
        // Priority list for CPU sensors
        val keywords = listOf("cpu-0-0-usr", "cpu-0-1-usr", "cpu-1-0-usr", "cpu-1-1-usr", "cpu_thermal", "cpu-thermal", "cpu", "soc-thermal")
        
        for (keyword in keywords) {
            val key = zones.keys.find { it.contains(keyword, ignoreCase = true) }
            if (key != null) {
                val temp = ThermalController.getTemperature(key)
                if (temp != "N/A") return temp
            }
        }
        
        // Fallback: pick the first one that has "cpu" in the name
        val fallback = zones.keys.find { it.contains("cpu", ignoreCase = true) }
        return if (fallback != null) ThermalController.getTemperature(fallback) else "N/A"
    }

    /**
     * Resolves the most likely GPU temperature sensor.
     */
    fun getGpuTemp(): String {
        val registry = SystemDiscovery.getRegistry()
        val zones = registry.thermalZones
        
        val keywords = listOf("gpu-usr", "gpu_thermal", "gpu-thermal", "gpu")
        
        for (keyword in keywords) {
            val key = zones.keys.find { it.contains(keyword, ignoreCase = true) }
            if (key != null) {
                val temp = ThermalController.getTemperature(key)
                if (temp != "N/A") return temp
            }
        }
        return "N/A"
    }

    /**
     * Calculates the aggregate CPU load by parsing /proc/stat.
     * Uses the delta between two snapshots for accuracy.
     */
    private var lastTotal = 0L
    private var lastIdle = 0L

    fun getCpuLoad(): Int {
        try {
            val content = SmartShell.read("/proc/stat")
            val aggregateLine = content.split("\n").firstOrNull { it.startsWith("cpu ") } ?: return 0
            val cols = aggregateLine.trim().split(Regex("\\s+"))
            
            if (cols.size < 5) return 0
            
            // Indices: 0:cpu, 1:user, 2:nice, 3:system, 4:idle, 5:iowait, 6:irq, 7:softirq
            val user = cols[1].toLongOrNull() ?: 0L
            val nice = cols[2].toLongOrNull() ?: 0L
            val system = cols[3].toLongOrNull() ?: 0L
            val idle = cols[4].toLongOrNull() ?: 0L
            val iowait = if (cols.size > 5) cols[5].toLongOrNull() ?: 0L else 0L
            val irq = if (cols.size > 6) cols[6].toLongOrNull() ?: 0L else 0L
            val softirq = if (cols.size > 7) cols[7].toLongOrNull() ?: 0L else 0L

            val total = user + nice + system + idle + iowait + irq + softirq
            val diffTotal = total - lastTotal
            val diffIdle = idle - lastIdle
            
            lastTotal = total
            lastIdle = idle

            if (diffTotal <= 0L) return 0
            val usage = (diffTotal - diffIdle) * 100 / diffTotal
            return usage.toInt().coerceIn(0, 100)
        } catch (e: Exception) {
            Log.e(TAG, "CPU load probe failed", e)
            return 0
        }
    }

    /**
     * Retrieves network speed and type information.
     * Uses system properties and connectivity status.
     */
    fun getNetworkInfo(): Pair<String, String> {
        try {
            val netType = SmartShell.sh("getprop gsm.network.type").uppercase()
            val wifiActive = SmartShell.sh("dumpsys wifi | grep 'mWifiInfo'").contains("SSID")
            
            val type = when {
                wifiActive -> "Wi-Fi"
                netType.contains("NR") || netType.contains("5G") -> "5G"
                netType.contains("LTE") -> "4G LTE"
                netType.isNotEmpty() -> netType
                else -> "Mobile"
            }
            
            val state = if (wifiActive || netType.isNotEmpty()) "Connected" else "Offline"
            return Pair(type, state)
        } catch (e: Exception) {
            return Pair("—", "Unknown")
        }
    }

    /**
     * Calculates storage usage for the data partition.
     */
    fun getStorageUsage(): Pair<Float, String> {
        try {
            // 'stat' is often more consistent than 'df' across different toybox/toolbox versions
            // %b = Total blocks, %f = Free blocks, %s = Block size
            val output = SmartShell.sh("stat -f -c \"%b %f %s\" /data").trim()
            val parts = output.split(Regex("\\s+"))
            
            if (parts.size >= 3) {
                val totalBlocks = parts[0].toLongOrNull() ?: 0L
                val freeBlocks = parts[1].toLongOrNull() ?: 0L
                val blockSize = parts[2].toLongOrNull() ?: 4096L
                
                if (totalBlocks > 0) {
                    val usedBlocks = totalBlocks - freeBlocks
                    val pct = usedBlocks.toFloat() / totalBlocks.toFloat()
                    
                    val totalBytes = totalBlocks * blockSize
                    val usedBytes = usedBlocks * blockSize
                    
                    val totalGB = totalBytes / (1024 * 1024 * 1024)
                    val usedGB = usedBytes / (1024 * 1024 * 1024)
                    
                    return Pair(pct, "$usedGB GB / $totalGB GB")
                }
            }
            
            // Fallback to df if stat fails
            val dfOutput = SmartShell.sh("df -k /data").trim().split("\n").last()
            val cols = dfOutput.trim().split(Regex("\\s+")).filter { it.all { c -> c.isDigit() } }
            if (cols.size >= 2) {
                val totalKB = cols[0].toLong()
                val usedKB = cols[1].toLong()
                val pct = usedKB.toFloat() / totalKB.toFloat()
                return Pair(pct, "${usedKB / 1048576} GB / ${totalKB / 1048576} GB")
            }

            return Pair(0f, "0 GB / 0 GB")
        } catch (e: Exception) {
            Log.e(TAG, "Storage probe failed", e)
            return Pair(0f, "—")
        }
    }

    /**
     * Gets display information (Resolution and Refresh Rate).
     * Note: This usually needs a Context, but we can probe sysfs for some devices.
     */
    fun getDisplayStats(): String {
        try {
            val res = SmartShell.sh("wm size").substringAfter(": ").trim()
            val dumpsys = SmartShell.sh("dumpsys display")
            
            val refresh = dumpsys.lineSequence()
                .filter { it.contains("fps", true) || it.contains("refresh", true) }
                .map { line ->
                    line.substringAfter("fps=").substringBefore(",")
                        .ifEmpty { line.substringAfter("mRefreshRate=").substringBefore(",") }
                        .trim()
                }
                .firstOrNull { it.isNotEmpty() && it.first().isDigit() }
                ?.takeWhile { it.isDigit() || it == '.' }
                ?: "60"
            
            return if (res.isNotEmpty()) "$res · ${refresh}Hz" else "Standard · ${refresh}Hz"
        } catch (e: Exception) {
            return "Standard · 60Hz"
        }
    }
}
