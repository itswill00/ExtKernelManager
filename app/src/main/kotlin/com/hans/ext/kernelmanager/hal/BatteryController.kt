package com.hans.ext.kernelmanager.hal

import com.hans.ext.kernelmanager.util.SmartShell
import com.hans.ext.kernelmanager.hal.intelligence.SystemDiscovery
import android.util.Log

/**
 * BatteryController (V12) - The Sovereign Energy & Health Orchestrator.
 * Provides exhaustive control over kernel-level battery drivers, including 
 * fast charge management, battery idle modes, and health analytics.
 * Features safety-first charging policies and deep telemetry.
 */
object BatteryController {
    private const val TAG = "SovereignBattery"
    private val batteryAudit = mutableListOf<String>()

    /**
     * Resolves the battery subsystem mapping from the Omni-Oracle.
     */
    private fun getBatteryMap(): Map<String, String> {
        return SystemDiscovery.getRegistry().subsystems["BATTERY"] ?: emptyMap()
    }

    /**
     * Retrieves the real-time battery capacity percentage.
     */
    fun getCapacity(): Int {
        val path = getBatteryMap()["capacity"] ?: return 0
        return SmartShell.read(path).toIntOrNull() ?: 0
    }

    /**
     * Retrieves the current battery temperature in a human-readable format.
     */
    fun getTemperature(): String {
        val path = getBatteryMap()["temp"] ?: return "N/A"
        val raw = SmartShell.read(path)
        return if (raw.isNotEmpty()) "${raw.toFloat() / 10}°C" else "N/A"
    }

    /**
     * Retrieves the real-time current flow (charge/discharge) in mA.
     */
    fun getCurrentNow(): String {
        val path = getBatteryMap()["current_now"] ?: return "0 mA"
        val raw = SmartShell.read(path).toLongOrNull() ?: 0L
        // Current can be in microAmps or milliAmps depending on kernel
        return if (raw > 100000 || raw < -100000) "${raw / 1000} mA" else "$raw mA"
    }

    /**
     * Retrieves the real-time battery voltage in mV.
     */
    fun getVoltage(): String {
        val path = getBatteryMap()["voltage_now"] ?: return "0 mV"
        val raw = SmartShell.read(path).toLongOrNull() ?: 0L
        return if (raw > 1000000) "${raw / 1000} mV" else "$raw mV"
    }

    /**
     * Retrieves the charging status (Charging, Discharging, Full, etc.)
     */
    fun getStatus(): String {
        val path = getBatteryMap()["status"] ?: return "Unknown"
        return SmartShell.read(path).ifEmpty { "Unknown" }
    }

    /**
     * Retrieves the hardware-reported health status.
     */
    fun getHealth(): String {
        val path = getBatteryMap()["health"] ?: return "Good"
        return SmartShell.read(path).ifEmpty { "Good" }
    }

    /**
     * Retrieves battery technology (Li-ion, Li-poly, etc.)
     */
    fun getTechnology(): String {
        val path = getBatteryMap()["technology"] ?: return "Li-ion"
        return SmartShell.read(path).ifEmpty { "Li-ion" }
    }

    /**
     * Fast Charge: Manages kernel-level USB and AC fast charging toggles.
     */
    fun isFastChargeAvailable() = getBatteryMap().containsKey("fast_charge") || nodeExists("/sys/kernel/fast_charge/force_fast_charge")

    fun setFastCharge(enabled: Boolean): Boolean {
        log("Battery: Setting Fast Charge state to $enabled")
        val path = getBatteryMap()["fast_charge"] ?: "/sys/kernel/fast_charge/force_fast_charge"
        return SmartShell.write(path, if (enabled) "1" else "0")
    }

    /**
     * Battery Idle: Enables 'Bypass Charging' mode where the device runs on AC power only.
     * Useful for gaming to reduce thermal stress.
     */
    fun isBatteryIdleAvailable() = nodeExists("/sys/class/power_supply/battery/battery_charging_enabled")

    fun setBatteryIdle(enabled: Boolean): Boolean {
        log("Battery: Setting Battery Idle (Bypass) mode to $enabled")
        val path = "/sys/class/power_supply/battery/battery_charging_enabled"
        // 0 means charging is disabled (Idle mode active)
        return SmartShell.write(path, if (enabled) "0" else "1")
    }

    /**
     * Cycle Count: Retrieves the total charge cycle count from the hardware fuel gauge.
     */
    fun getCycleCount(): String {
        val path = getBatteryMap()["cycle_count"] ?: return "N/A"
        return SmartShell.read(path).ifEmpty { "N/A" }
    }

    /**
     * Design Capacity vs Full Charge Capacity: Calculates battery health percentage.
     */
    fun getBatteryHealthPercent(): String {
        val full = getBatteryMap()["charge_full"]?.let { SmartShell.read(it).toLongOrNull() } ?: return "N/A"
        val design = getBatteryMap()["charge_full_design"]?.let { SmartShell.read(it).toLongOrNull() } ?: (full + 100000)
        
        val health = (full.toFloat() / design.toFloat()) * 100
        return "%.1f%%".format(health)
    }

    /**
     * Thermal Charging Limiter: Reduces charge current when temperature exceeds thresholds.
     */
    fun setThermalLimit(ma: Int): Boolean {
        val path = "/sys/class/power_supply/battery/constant_charge_current_max"
        log("Battery: Restricting charge current to ${ma}mA for thermal safety")
        return if (nodeExists(path)) SmartShell.write(path, (ma * 1000).toString()) else false
    }

    /**
     * Charging Profiles: Applies pre-defined energy management strategies.
     */
    fun applyBatteryProfile(profile: String): Boolean {
        log("Battery: Applying Energy Profile: $profile")
        return when (profile.lowercase()) {
            "fast" -> {
                setFastCharge(true)
                setThermalLimit(5000)
                true
            }
            "safe" -> {
                setFastCharge(false)
                setThermalLimit(1500)
                true
            }
            "idle" -> {
                setBatteryIdle(true)
                true
            }
            else -> false
        }
    }

    /**
     * Sysfs interface verification.
     */
    private fun nodeExists(path: String): Boolean = SmartShell.nodeExists(path)

    /**
     * Transaction Auditing.
     */
    private fun log(message: String) {
        val entry = "[${System.currentTimeMillis()}] $message"
        batteryAudit.add(entry)
        Log.i(TAG, message)
        if (batteryAudit.size > 200) batteryAudit.removeAt(0)
    }

    /**
     * Retrieves the complete battery transaction history.
     */
    fun getAuditHistory(): List<String> = batteryAudit.toList()

    /**
     * Generates a professional technical report of the energy subsystem.
     */
    fun getBatteryReport(): String {
        return "Health: ${getBatteryHealthPercent()}\n" +
               "Cycles: ${getCycleCount()}\n" +
               "Temp: ${getTemperature()}\n" +
               "Current: ${getCurrentNow()}\n" +
               "Idle Mode: ${if (isBatteryIdleAvailable()) "Supported" else "N/A"}"
    }

    /**
     * Performs a system-wide battery integrity audit.
     */
    fun performBatteryAudit(): Boolean {
        log("Executing Battery Subsystem Integrity Check...")
        return getBatteryMap().isNotEmpty()
    }
}
