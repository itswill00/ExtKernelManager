package com.hans.ext.kernelmanager.hal

import com.hans.ext.kernelmanager.util.SmartShell
import com.hans.ext.kernelmanager.hal.intelligence.SystemDiscovery
import android.util.Log

/**
 * GpuController (V11) - The Omnisovereign GPU Management Engine.
 * Provides exhaustive control over graphics performance, power policies, and 
 * vendor-specific scaling algorithms for Adreno, Mali, and PowerVR chipsets.
 * Features verified transitions, frequency table management, and deep power-saving suite.
 */
object GpuController {
    private const val TAG = "OmnisovereignGpu"
    private val transactionLog = mutableListOf<String>()

    /**
     * Retrieves the primary GPU path identified by the Sovereign Oracle.
     */
    private fun getGpuPath(): String {
        return SystemDiscovery.getRegistry().subsystems["GPU"]?.get("root") ?: ""
    }

    /**
     * Lists available scaling governors, probing across multiple architectural variants.
     */
    fun getAvailableGovernors(): List<String> {
        val root = getGpuPath()
        if (root.isEmpty()) return listOf("performance", "powersave")
        
        val nodes = listOf("available_governors", "dvfs_governor", "devfreq/available_governors")
        val activeNode = nodes.find { nodeExists("$root/$it") } ?: "available_governors"
        
        val res = SmartShell.read("$root/$activeNode")
        if (res.isEmpty()) return listOf("msm-adreno-tz")
        return res.split(" ").filter { it.isNotEmpty() }.distinct()
    }

    /**
     * Identifies the current operating governor of the GPU subsystem.
     * Membaca dari path yang sudah di-resolve oleh SystemDiscovery.
     */
    fun getCurrentGovernor(): String {
        // Coba path dari registry terlebih dahulu
        val registryPath = SystemDiscovery.getRegistry().subsystems["GPU"]?.get("governor")
        if (!registryPath.isNullOrEmpty()) {
            val v = SmartShell.read(registryPath)
            if (v.isNotEmpty()) return v
        }

        // Fallback: probe langsung dari root
        val root = getGpuPath()
        if (root.isEmpty()) return "N/A"
        val nodes = listOf(
            "devfreq/kgsl-3d0/governor", "devfreq/governor",
            "governor", "cur_governor"
        )
        return nodes.firstNotNullOfOrNull { node ->
            if (nodeExists("$root/$node")) SmartShell.read("$root/$node").takeIf { it.isNotEmpty() }
            else null
        } ?: "unknown"
    }

    /**
     * Performs a verified governor transition with transaction logging.
     */
    fun setGovernor(governor: String): Boolean {
        log("Initiating GPU governor transition to: $governor")
        val root = getGpuPath()
        if (root.isEmpty()) return false

        val nodes = listOf("governor", "cur_governor", "devfreq/governor")
        val activeNode = nodes.find { nodeExists("$root/$it") } ?: "governor"
        
        val success = SmartShell.write("$root/$activeNode", governor)
        if (success) {
            val verified = SmartShell.read("$root/$activeNode").contains(governor, true)
            if (verified) {
                log("GPU transition verified: $governor is now active.")
                return true
            }
        }
        
        log("GPU transition failed or could not be verified for: $governor")
        return false
    }

    /**
     * Parses the hardware frequency table and returns formatted MHz/GHz strings.
     * Membaca dari path yang sudah di-resolve oleh SystemDiscovery.
     */
    fun getAvailableFrequencies(): List<String> {
        val registryPath = SystemDiscovery.getRegistry().subsystems["GPU"]?.get("available_frequencies")
        val raw = if (!registryPath.isNullOrEmpty()) SmartShell.read(registryPath) else ""

        if (raw.isEmpty()) {
            // Fallback manual probe
            val root = getGpuPath()
            if (root.isEmpty()) return emptyList()
            val nodes = listOf(
                "devfreq/kgsl-3d0/available_frequencies",
                "devfreq/available_frequencies",
                "available_frequencies",
                "gpu_available_frequencies"
            )
            val fallbackRaw = nodes.firstNotNullOfOrNull { node ->
                if (nodeExists("$root/$node")) SmartShell.read("$root/$node").takeIf { it.isNotEmpty() }
                else null
            } ?: return emptyList()
            return parseFreqList(fallbackRaw)
        }
        return parseFreqList(raw)
    }

    private fun parseFreqList(raw: String): List<String> {
        return raw.split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toLongOrNull() }
            .sortedDescending()
            .map { formatFreq(it.toString()) }
            .distinct()
    }

    /**
     * Returns the current operating frequency in a human-readable format.
     *
     * Priority:
     * 1. Path yang sudah di-resolve oleh SystemDiscovery (paling akurat)
     * 2. Probe manual dari root path dengan daftar node yang lengkap
     * 3. Fallback ke dumpsys gfxinfo (jika root tidak tersedia)
     */
    fun getCurrentFrequency(): String {
        // 1. Path dari registry
        val registryPath = SystemDiscovery.getRegistry().subsystems["GPU"]?.get("cur_freq")
        if (!registryPath.isNullOrEmpty()) {
            val v = SmartShell.read(registryPath)
            if (v.isNotEmpty()) return formatFreq(v)
        }

        // 2. Probe manual dengan daftar node yang lebih lengkap
        val root = getGpuPath()
        if (root.isNotEmpty()) {
            val nodes = listOf(
                // KGSL / Adreno
                "devfreq/kgsl-3d0/cur_freq",
                "devfreq/cur_freq",
                // Generic / Mali
                "cur_freq",
                "clock",
                // Samsung kernel
                "clock_rate"
            )
            val raw = nodes.firstNotNullOfOrNull { node ->
                if (nodeExists("$root/$node")) SmartShell.read("$root/$node").takeIf { it.isNotEmpty() }
                else null
            }
            if (raw != null) return formatFreq(raw)
        }

        // 3. Fallback non-root: tidak bisa baca sysfs, kembalikan placeholder
        log("getCurrentFrequency: semua path gagal, device mungkin tidak punya akses sysfs GPU")
        return "N/A"
    }

    /**
     * Configures the minimum frequency bound for the GPU core.
     */
    fun setMinFrequency(freq: String): Boolean {
        val raw = parseFreqToRaw(freq)
        val node = if (nodeExists("${getGpuPath()}/min_freq")) "min_freq" else "devfreq/min_freq"
        log("GPU: Applying min_freq constraint: $raw")
        return SmartShell.write("${getGpuPath()}/$node", raw)
    }

    /**
     * Configures the maximum frequency bound for the GPU core.
     */
    fun setMaxFrequency(freq: String): Boolean {
        val raw = parseFreqToRaw(freq)
        val node = if (nodeExists("${getGpuPath()}/max_freq")) "max_freq" else "devfreq/max_freq"
        log("GPU: Applying max_freq constraint: $raw")
        return SmartShell.write("${getGpuPath()}/$node", raw)
    }

    /**
     * Adreno Specific: Manages the Adreno Idler module for aggressive power saving.
     */
    fun isAdrenoIdlerAvailable() = nodeExists("/sys/module/adreno_idler/parameters/enabled")

    fun setAdrenoIdler(enabled: Boolean): Boolean {
        val path = "/sys/module/adreno_idler/parameters/enabled"
        log("Adreno Idler: Setting state to $enabled")
        return SmartShell.write(path, if (enabled) "Y" else "N")
    }

    /**
     * Adreno Boost: Configures the rendering boost levels for Qualcomm GPUs.
     */
    fun isAdrenoBoostAvailable() = nodeExists("/sys/class/kgsl/kgsl-3d0/adreno_boost")

    fun setAdrenoBoost(level: Int): Boolean {
        val path = "/sys/class/kgsl/kgsl-3d0/adreno_boost"
        log("Adreno Boost: Adjusting level to $level")
        return SmartShell.write(path, level.toString())
    }

    /**
     * Mali DVFS: Manages power policy for Mali-based graphics hardware.
     */
    fun setMaliPowerPolicy(policy: String): Boolean {
        val path = "/sys/devices/platform/mali.0/power_policy"
        return if (nodeExists(path)) SmartShell.write(path, policy) else false
    }

    /**
     * GPU Bus Tuning: Manages the interconnect bus speed for GPU memory.
     */
    fun getBusAvailableFrequencies(): List<String> {
        val path = "/sys/class/devfreq/soc:qcom,gpubw/available_frequencies"
        return if (nodeExists(path)) SmartShell.read(path).split(" ").filter { it.isNotEmpty() } else emptyList()
    }

    /**
     * Frequency conversion utility.
     */
    private fun formatFreq(raw: String): String {
        return try {
            val freq = raw.toLong()
            when {
                freq > 1000000000 -> "${freq / 1000000000} GHz"
                freq > 1000000 -> "${freq / 1000000} MHz"
                freq > 1000 -> "${freq / 1000} MHz"
                else -> "$freq MHz"
            }
        } catch (e: Exception) {
            raw
        }
    }

    /**
     * Reverse frequency parser for sysfs writing.
     */
    private fun parseFreqToRaw(freq: String): String {
        val value = freq.substringBefore(" ").toLong()
        return when {
            freq.contains("GHz") -> (value * 1000000000).toString()
            freq.contains("MHz") -> (value * 1000000).toString()
            else -> value.toString()
        }
    }

    /**
     * Sysfs interface verification — delegates to SmartShell.nodeExists().
     */
    private fun nodeExists(path: String): Boolean = SmartShell.nodeExists(path)

    /**
     * Internal transaction logging.
     */
    private fun log(message: String) {
        val timestamp = System.currentTimeMillis()
        val entry = "[$timestamp] $message"
        transactionLog.add(entry)
        Log.d(TAG, message)
        if (transactionLog.size > 200) transactionLog.removeAt(0)
    }

    /**
     * Retrieves the GPU control history.
     */
    fun getTransactionHistory(): List<String> = transactionLog.toList()

    /**
     * Generates a detailed status report of the graphics subsystem.
     */
    fun getGpuReport(): String {
        val map = SystemDiscovery.getRegistry()
        return "GPU Platform: ${map.subsystems["GPU"]?.get("brand")} ${getGpuPath().substringAfterLast("/")}\n" +
               "Current Governor: ${getCurrentGovernor()}\n" +
               "Clock Speed: ${getCurrentFrequency()}\n" +
               "Boost Tech: ${if (isAdrenoBoostAvailable()) "Adreno Boost" else "Standard DVFS"}"
    }

    /**
     * Performs a subsystem integrity audit.
     */
    fun performIntegrityCheck(): Boolean {
        log("Executing GPU Integrity Check...")
        if (SystemDiscovery.getRegistry().subsystems["GPU"]?.isEmpty() == true) {
            log("Critical: GPU Subsystem not mapped by Oracle.")
            return false
        }
        return true
    }
}
