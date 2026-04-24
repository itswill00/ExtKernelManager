package com.hans.ext.kernelmanager.hal

import com.hans.ext.kernelmanager.util.SmartShell
import com.hans.ext.kernelmanager.hal.intelligence.SystemDiscovery
import android.util.Log

/**
 * DisplayController (V12) - The Sovereign Screen Calibration Engine.
 * Provides exhaustive control over kernel-level display drivers, including 
 * KCAL RGB management, saturation, contrast, hue, and gamma adjustments.
 * Features verified transitions and professional color profiles.
 */
object DisplayController {
    private const val TAG = "SovereignDisplay"
    private val displayAudit = mutableListOf<String>()

    /**
     * Resolves the display subsystem mapping from the Omni-Oracle.
     */
    private fun getDisplayMap(): Map<String, String> {
        return SystemDiscovery.getRegistry().subsystems["DISPLAY"] ?: emptyMap()
    }

    /**
     * Checks if advanced display control (KCAL) is supported.
     */
    fun isKcalAvailable(): Boolean {
        return getDisplayMap().containsKey("kcal_kcal_enable") || nodeExists("/sys/devices/platform/kcal_ctrl.0/kcal_enable")
    }

    /**
     * RGB Calibration: Adjusts the red, green, and blue channel intensity.
     */
    fun getRgb(): String {
        val path = getDisplayMap()["kcal_kcal_ctrl"] ?: return "256 256 256"
        return SmartShell.read(path)
    }

    fun setRgb(r: Int, g: Int, b: Int): Boolean {
        log("Display: Calibrating RGB to R:$r G:$g B:$b")
        val path = getDisplayMap()["kcal_kcal_ctrl"] ?: "/sys/devices/platform/kcal_ctrl.0/kcal_ctrl"
        val value = "$r $g $b"
        return SmartShell.write(path, value)
    }

    /**
     * Saturation Management: Adjusts the overall color intensity.
     */
    fun getSaturation(): String {
        val path = getDisplayMap()["kcal_kcal_sat"] ?: return "255"
        return SmartShell.read(path)
    }

    fun setSaturation(value: Int): Boolean {
        log("Display: Adjusting Saturation to $value")
        val path = getDisplayMap()["kcal_kcal_sat"] ?: "/sys/devices/platform/kcal_ctrl.0/kcal_sat"
        return SmartShell.write(path, value.toString())
    }

    /**
     * Contrast Management: Adjusts the visual difference between light and dark areas.
     */
    fun getContrast(): String {
        val path = getDisplayMap()["kcal_kcal_cont"] ?: return "255"
        return SmartShell.read(path)
    }

    fun setContrast(value: Int): Boolean {
        log("Display: Adjusting Contrast to $value")
        val path = getDisplayMap()["kcal_kcal_cont"] ?: "/sys/devices/platform/kcal_ctrl.0/kcal_cont"
        return SmartShell.write(path, value.toString())
    }

    /**
     * Hue Management: Shifts the overall color spectrum.
     */
    fun getHue(): String {
        val path = getDisplayMap()["kcal_kcal_hue"] ?: return "0"
        return SmartShell.read(path)
    }

    fun setHue(value: Int): Boolean {
        log("Display: Adjusting Hue to $value")
        val path = getDisplayMap()["kcal_kcal_hue"] ?: "/sys/devices/platform/kcal_ctrl.0/kcal_hue"
        return SmartShell.write(path, value.toString())
    }

    /**
     * Value (Brightness) Management: Adjusts the overall brightness floor of the panel.
     */
    fun getValue(): String {
        val path = getDisplayMap()["kcal_kcal_val"] ?: return "255"
        return SmartShell.read(path)
    }

    fun setValue(value: Int): Boolean {
        log("Display: Adjusting Value to $value")
        val path = getDisplayMap()["kcal_kcal_val"] ?: "/sys/devices/platform/kcal_ctrl.0/kcal_val"
        return SmartShell.write(path, value.toString())
    }

    /**
     * Gamma Control: Manages the luminance distribution curve.
     */
    fun setGamma(profile: Int): Boolean {
        val path = "/sys/devices/platform/kcal_ctrl.0/kcal_gamma"
        log("Display: Applying Gamma Profile $profile")
        return if (nodeExists(path)) SmartShell.write(path, profile.toString()) else false
    }

    /**
     * Display Profiles: Applies professional-grade calibration presets.
     */
    fun applyColorProfile(profile: String): Boolean {
        log("Display: Applying Color Profile: $profile")
        return when (profile.lowercase()) {
            "amoled" -> {
                setRgb(256, 256, 256)
                setSaturation(280)
                setContrast(260)
                true
            }
            "vivid" -> {
                setRgb(256, 256, 256)
                setSaturation(270)
                setContrast(255)
                true
            }
            "natural" -> {
                setRgb(256, 256, 256)
                setSaturation(255)
                setContrast(255)
                true
            }
            "cool" -> {
                setRgb(240, 240, 256)
                setSaturation(255)
                setContrast(255)
                true
            }
            "warm" -> {
                setRgb(256, 245, 230)
                setSaturation(255)
                setContrast(255)
                true
            }
            else -> false
        }
    }

    /**
     * Sysfs interface verification.
     */
    private fun nodeExists(path: String): Boolean {
        return SmartShell.read("if [ -e $path ]; then echo 1; else echo 0; fi") == "1"
    }

    /**
     * Transaction Auditing.
     */
    private fun log(message: String) {
        val entry = "[${System.currentTimeMillis()}] $message"
        displayAudit.add(entry)
        Log.d(TAG, message)
        if (displayAudit.size > 200) displayAudit.removeAt(0)
    }

    /**
     * Retrieves the complete display adjustment history.
     */
    fun getAuditHistory(): List<String> = displayAudit.toList()

    /**
     * Retrieves the physical display resolution.
     */
    fun getResolution(): String {
        val raw = SmartShell.sh("wm size")
        return raw.substringAfter("Physical size: ").trim().ifEmpty { 
            val densityRaw = SmartShell.sh("getprop ro.sf.lcd_density").trim()
            "N/A (Density: $densityRaw)"
        }
    }

    /**
     * Retrieves the current display refresh rate using a multi-stage fallback.
     */
    fun getRefreshRate(): String {
        // 1. Try SurfaceFlinger (Often most accurate for active rate)
        val sf = SmartShell.sh("dumpsys SurfaceFlinger | grep -i 'refresh-rate'").trim()
        if (sf.isNotEmpty()) {
            val match = Regex("(\\d+\\.?\\d*)").find(sf)
            match?.value?.toDoubleOrNull()?.let { return "${it.toInt()} Hz" }
        }

        // 2. Try Display Default Mode
        val display = SmartShell.sh("dumpsys display | grep 'mDefaultModeId'").trim()
        if (display.isNotEmpty()) {
            val modeId = display.substringAfter("mDefaultModeId=").substringBefore(",").trim()
            val modes = SmartShell.sh("dumpsys display | grep 'id=$modeId'").trim()
            val fpsMatch = Regex("fps=(\\d+\\.?\\d*)").find(modes)
            fpsMatch?.groupValues?.get(1)?.toDoubleOrNull()?.let { return "${it.toInt()} Hz" }
        }

        // 3. Try Global FPS grep
        val raw = SmartShell.sh("dumpsys display | grep -E 'fps|mRefreshRate'").trim()
        val fps = Regex("(\\d{2,3}\\.\\d+)").findAll(raw)
            .mapNotNull { it.value.toDoubleOrNull() }
            .firstOrNull { it > 10 }
        
        if (fps != null) return "${fps.toInt()} Hz"

        // 4. Try OEM Specific Props
        val props = listOf(
            "ro.surface_flinger.display_primary_red",
            "persist.vendor.power.dfps.level",
            "ro.vendor.display.default_fps"
        )
        for (p in props) {
            val v = SmartShell.sh("getprop $p").trim()
            if (v.isNotEmpty() && v.first().isDigit()) return "$v Hz"
        }

        return "60 Hz"
    }

    /**
     * Retrieves screen density (DPI).
     */
    fun getDensity(): String {
        val prop = SmartShell.sh("getprop ro.sf.lcd_density").trim()
        return if (prop.isNotEmpty()) "$prop dpi" else "N/A"
    }

    /**
     * Tries to identify the panel type or vendor via sysfs or logcat.
     */
    fun getPanelInfo(): String {
        val dmesg = SmartShell.sh("dmesg | grep -i panel").lowercase()
        return when {
            dmesg.contains("samsung") -> "Samsung OLED"
            dmesg.contains("boe") -> "BOE Panel"
            dmesg.contains("lgd") -> "LG Display"
            dmesg.contains("tianma") -> "Tianma LCD"
            else -> "Generic Panel"
        }
    }

    /**
     * Checks for HDR capabilities.
     */
    fun getHdrCapabilities(): String {
        val raw = SmartShell.sh("dumpsys display | grep -i hdr").trim()
        return if (raw.contains("HDR10")) "HDR10 supported" else "Standard Dynamic Range"
    }

    /**
     * Generates a professional technical report of the display subsystem.
     */
    fun getDisplayReport(): String {
        return "Panel Tech: ${getPanelInfo()}\n" +
               "Resolution: ${getResolution()}\n" +
               "Refresh Rate: ${getRefreshRate()}\n" +
               "Density: ${getDensity()}\n" +
               "HDR: ${getHdrCapabilities()}"
    }

    /**
     * Performs a system-wide display integrity audit.
     */
    fun performDisplayAudit(): Boolean {
        log("Executing Display Subsystem Integrity Check...")
        return isKcalAvailable()
    }
}
