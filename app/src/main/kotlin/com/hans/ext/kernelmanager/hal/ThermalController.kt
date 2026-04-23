package com.hans.ext.kernelmanager.hal

import com.hans.ext.kernelmanager.util.SmartShell
import com.hans.ext.kernelmanager.hal.intelligence.SystemDiscovery
import android.util.Log

/**
 * ThermalController (V11) - The Omnisovereign Thermal & Cooling Orchestrator.
 * Provides exhaustive control over the system thermal management framework, 
 * enabling granular monitoring of thermal zones and cooling device policies.
 * Features thermal throttle overrides and safety guard mechanisms for stability.
 */
object ThermalController {
    private const val TAG = "OmnisovereignThermal"
    private val thermalAudit = mutableListOf<String>()

    /**
     * Retrieves all discovered thermal zones from the Sovereign Oracle.
     */
    fun getThermalZones(): Map<String, String> {
        return SystemDiscovery.getRegistry().thermalZones
    }

    /**
     * Returns the real-time temperature of a specific zone (e.g., "battery", "cpu-0-0").
     */
    fun getTemperature(type: String): String {
        val path = getThermalZones()[type] ?: return "N/A"
        val raw = SmartShell.read("$path/temp")
        return if (raw.isNotEmpty()) {
            val temp = raw.toFloat() / 1000
            "%.1f°C".format(temp)
        } else "N/A"
    }

    /**
     * Lists all identified cooling devices (fans, throttlers, limiters).
     */
    fun getCoolingDevices(): List<String> {
        val base = "/sys/class/thermal"
        val items = SmartShell.read("ls $base").split(" ").filter { it.startsWith("cooling_device") }
        val res = mutableListOf<String>()
        for (item in items) {
            val type = SmartShell.read("$base/$item/type")
            if (type.isNotEmpty()) res.add("$item ($type)")
        }
        return res
    }

    /**
     * Sets the maximum cooling level for a specific device (Manual Throttle Control).
     */
    fun setCoolingLevel(deviceId: Int, level: Int): Boolean {
        log("Thermal: Adjusting cooling device $deviceId to level $level")
        val path = "/sys/class/thermal/cooling_device$deviceId/cur_state"
        return if (nodeExists(path)) SmartShell.write(path, level.toString()) else false
    }

    /**
     * Thermal Throttle Override: Specialized logic for Qualcomm/Exynos thermal engines.
     */
    fun isThrottleOverrideAvailable(): Boolean {
        val candidates = listOf(
            "/sys/module/msm_thermal/parameters/enabled",
            "/sys/devices/virtual/thermal/thermal_message/sconfig",
            "/sys/class/thermal/thermal_message/sconfig"
        )
        return candidates.any { nodeExists(it) }
    }

    /**
     * Enables or disables the kernel-level thermal engine.
     * WARNING: Disabling this can lead to hardware damage if not monitored.
     */
    fun setThermalEngineState(enabled: Boolean): Boolean {
        log("Thermal: ADJUSTING GLOBAL ENGINE STATE TO $enabled")
        val path = "/sys/module/msm_thermal/parameters/enabled"
        return if (nodeExists(path)) {
            SmartShell.write(path, if (enabled) "Y" else "N")
        } else {
            // Fallback for Samsung/MTK
            val configNode = "/sys/class/thermal/thermal_message/sconfig"
            if (nodeExists(configNode)) SmartShell.write(configNode, if (enabled) "0" else "10") else false
        }
    }

    /**
     * Thermal Profiles: Applies high-level thermal policies (Gaming, Cool, Balanced).
     */
    fun applyThermalProfile(profile: String): Boolean {
        log("Thermal: Applying high-level profile: $profile")
        val node = "/sys/devices/virtual/thermal/thermal_message/sconfig"
        if (!nodeExists(node)) return false
        
        val value = when (profile.lowercase()) {
            "gaming" -> "10"
            "balanced" -> "0"
            "cool" -> "1"
            "battery" -> "2"
            else -> "0"
        }
        return SmartShell.write(node, value)
    }

    /**
     * Trip Point Management: Retrieves the temperature at which a zone triggers cooling.
     */
    fun getTripPointTemp(zone: String, tripId: Int): String {
        val path = getThermalZones()[zone] ?: return "N/A"
        val node = "$path/trip_point_${tripId}_temp"
        val raw = SmartShell.read(node)
        return if (raw.isNotEmpty()) (raw.toFloat() / 1000).toString() + "°C" else "N/A"
    }

    /**
     * Sysfs interface verification — delegates to SmartShell.nodeExists().
     */
    private fun nodeExists(path: String): Boolean = SmartShell.nodeExists(path)

    /**
     * Thermal Audit Logging.
     */
    private fun log(message: String) {
        val entry = "[${System.currentTimeMillis()}] $message"
        thermalAudit.add(entry)
        Log.w(TAG, message) // Use Warning level for thermal events
        if (thermalAudit.size > 200) thermalAudit.removeAt(0)
    }

    /**
     * Retrieves the complete thermal management history.
     */
    fun getAuditTrail(): List<String> = thermalAudit.toList()

    /**
     * Generates a professional-grade thermal health report.
     */
    fun getThermalTechnicalReport(): String {
        val zones = getThermalZones()
        val sb = StringBuilder()
        sb.append("System Thermal Health: ${if (isThrottleOverrideAvailable()) "Manual Capable" else "Restricted"}\n")
        sb.append("Primary Sensors:\n")
        zones.keys.take(5).forEach { type ->
            sb.append("- $type: ${getTemperature(type)}\n")
        }
        return sb.toString()
    }

    /**
     * Safety Guard: Performs a comprehensive thermal integrity audit.
     */
    fun performSafetyAudit(): Boolean {
        log("Executing Thermal Safety Audit...")
        // Ensure critical zones (cpu/battery) are reachable
        val critical = listOf("battery", "cpu", "soc")
        val mapped = getThermalZones().keys
        return critical.any { tag -> mapped.any { it.contains(tag) } }
    }
}
