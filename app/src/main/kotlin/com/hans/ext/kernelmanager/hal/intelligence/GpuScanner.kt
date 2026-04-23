package com.hans.ext.kernelmanager.hal.intelligence

import com.hans.ext.kernelmanager.util.SmartShell
import android.util.Log

/**
 * GpuScanner (V12) - The Sovereign GPU Diagnostic & Discovery Engine.
 * A professional-grade module dedicated to the exhaustive identification of 
 * graphics hardware, frequency tables, and vendor-specific scaling features.
 * Supports deep integration for Adreno (Qualcomm), Mali (ARM), and PowerVR architectures.
 */
object GpuScanner {
    private const val TAG = "SovereignGpuScanner"
    private val diagnosticAudit = mutableListOf<String>()

    /**
     * Hardware Features: Encapsulates discovered GPU capabilities.
     */
    data class GpuFeatures(
        val model: String,
        val path: String,
        val architecture: String,
        val hasAdrenoIdler: Boolean,
        val hasAdrenoBoost: Boolean,
        val hasMaliDvfs: Boolean,
        val frequencyCount: Int,
        val governorCount: Int
    )

    /**
     * Performs a deep-spectrum GPU scan using signature-based detection.
     */
    fun scan(): GpuFeatures {
        log("--- Initiating Deep GPU Diagnostic Scan ---")
        
        val primaryPath = resolveGpuPath()
        val model = identifyModel(primaryPath)
        val arch = identifyArchitecture(model)
        
        val freqs = getAvailableFrequencies(primaryPath)
        val govs = getAvailableGovernors(primaryPath)
        
        val features = GpuFeatures(
            model = model,
            path = primaryPath,
            architecture = arch,
            hasAdrenoIdler = nodeExists("/sys/module/adreno_idler/parameters/enabled"),
            hasAdrenoBoost = nodeExists("$primaryPath/adreno_boost"),
            hasMaliDvfs = nodeExists("/sys/devices/platform/mali.0/power_policy"),
            frequencyCount = freqs.size,
            governorCount = govs.size
        )

        log("Scan Complete: Found $model ($arch) at $primaryPath")
        log("Available Performance Tiers: ${features.frequencyCount}")
        
        return features
    }

    /**
     * Recursive Path Resolver: Locates the primary GPU control interface.
     */
    private fun resolveGpuPath(): String {
        val candidates = listOf(
            "/sys/class/kgsl/kgsl-3d0",
            "/sys/devices/platform/mali.0",
            "/sys/class/misc/mali0",
            "/sys/devices/platform/soc/1c00000.gpu",
            "/sys/devices/platform/gpufreq-mali",
            "/sys/kernel/debug/kgsl/kgsl-3d0"
        )
        
        candidates.find { nodeExists(it) }?.let { return it }
        
        // Advanced Recursive Fallback
        val searchRoots = listOf("/sys/class", "/sys/devices/platform")
        for (root in searchRoots) {
            val list = SmartShell.read("ls $root").split(" ").filter { it.isNotEmpty() }
            val match = list.find { it.contains("gpu", true) || it.contains("mali", true) || it.contains("kgsl", true) }
            if (match != null) {
                val p = "$root/$match"
                if (nodeExists("$p/governor") || nodeExists("$p/cur_freq")) return p
            }
        }
        return ""
    }

    /**
     * Identifies the specific GPU model name from kernel strings.
     */
    private fun identifyModel(path: String): String {
        if (path.isEmpty()) return "Generic GPU"
        
        // Try getting model from kgsl if available
        if (path.contains("kgsl")) {
            val gpuId = SmartShell.read("$path/gpu_model")
            if (gpuId.isNotEmpty()) return "Adreno $gpuId"
            
            val chipId = SmartShell.read("$path/chipid")
            if (chipId.isNotEmpty()) return "Adreno (ID: $chipId)"
        }
        
        // Try getting from Mali interfaces
        if (path.contains("mali")) {
            val version = SmartShell.read("/proc/mali/version")
            if (version.isNotEmpty()) return "Mali ${version.substringBefore(" ")}"
            return "Mali-T/G Series"
        }

        return path.substringAfterLast("/")
    }

    /**
     * Maps the GPU model to a known architecture generation.
     */
    private fun identifyArchitecture(model: String): String {
        return when {
            model.contains("Adreno 7") || model.contains("Adreno 8") -> "Qualcomm Adreno (Gen 7/8)"
            model.contains("Adreno 6") -> "Qualcomm Adreno (Gen 6)"
            model.contains("Adreno 5") -> "Qualcomm Adreno (Gen 5)"
            model.contains("Mali-G") -> "ARM Valhall/Bifrost"
            model.contains("Mali-T") -> "ARM Midgard"
            else -> "Standard Mobile GPU"
        }
    }

    /**
     * Parses the GPU frequency table with advanced formatting.
     */
    private fun getAvailableFrequencies(path: String): List<String> {
        if (path.isEmpty()) return emptyList()
        val nodes = listOf("available_frequencies", "gpu_available_frequencies", "devfreq/available_frequencies")
        val node = nodes.find { nodeExists("$path/$it") } ?: "available_frequencies"
        
        val raw = SmartShell.read("$path/$node")
        if (raw.isEmpty()) return emptyList()
        
        return raw.split(" ")
            .filter { it.isNotEmpty() }
            .map { formatFreq(it) }
            .distinct()
            .sortedByDescending { it.substringBefore(" ").toLong() }
    }

    /**
     * Lists available governors for the graphics core.
     */
    private fun getAvailableGovernors(path: String): List<String> {
        if (path.isEmpty()) return emptyList()
        val nodes = listOf("available_governors", "dvfs_governor", "devfreq/available_governors")
        val node = nodes.find { nodeExists("$path/$it") } ?: "available_governors"
        
        val raw = SmartShell.read("$path/$node")
        return raw.split(" ").filter { it.isNotEmpty() }.distinct()
    }

    /**
     * Specialized: Parses Adreno frequency-latency tables if available.
     */
    fun getAdrenoTable(): List<String> {
        val path = "/sys/class/kgsl/kgsl-3d0/freq_table"
        return if (nodeExists(path)) SmartShell.read(path).lines() else emptyList()
    }

    /**
     * Frequency conversion utility for human-readable output.
     */
    private fun formatFreq(raw: String): String {
        return try {
            val f = raw.toLong()
            when {
                f > 1000000000 -> "${f / 1000000000} GHz"
                f > 1000000 -> "${f / 1000000} MHz"
                f > 1000 -> "${f / 1000} MHz"
                else -> "$f MHz"
            }
        } catch (e: Exception) {
            raw
        }
    }

    /**
     * Sysfs interface verification.
     */
    private fun nodeExists(path: String): Boolean {
        return SmartShell.read("if [ -e $path ]; then echo 1; else echo 0; fi") == "1"
    }

    /**
     * Internal diagnostic logging.
     */
    private fun log(message: String) {
        val entry = "[${System.currentTimeMillis()}] $message"
        diagnosticAudit.add(entry)
        Log.i(TAG, message)
        if (diagnosticAudit.size > 500) diagnosticAudit.removeAt(0)
    }

    /**
     * Retrieves the complete GPU diagnostic audit log.
     */
    fun getDiagnosticLog() = diagnosticAudit.toList()

    /**
     * Provides a professional status report of the graphics subsystem.
     */
    fun getGpuModel() = identifyModel(resolveGpuPath())

    /**
     * Performs a health check on the GPU control interface.
     */
    fun verifyIntegrity(): Boolean {
        val path = resolveGpuPath()
        if (path.isEmpty()) return false
        return nodeExists("$path/governor") && nodeExists("$path/cur_freq")
    }

    /**
     * Generates a technical profile for external framework use.
     */
    fun generateTechnicalProfile(): String {
        val f = scan()
        return "GPU Platform: ${f.model}\n" +
               "Architecture: ${f.architecture}\n" +
               "Sysfs Handle: ${f.path}\n" +
               "Feature Set: [Idler:${f.hasAdrenoIdler}, Boost:${f.hasAdrenoBoost}, DVFS:${f.hasMaliDvfs}]"
    }
}
