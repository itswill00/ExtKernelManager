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
     * Retrieves the precise GPU Renderer name (e.g., Adreno (TM) 740).
     */
    fun getRendererName(): String {
        // Stage 0: Direct Kernel Probing (Most Accurate if available)
        val kernelPaths = listOf(
            "/sys/kernel/debug/mali0/gpu_name",
            "/sys/kernel/debug/mali/gpu_name",
            "/sys/class/kgsl/kgsl-3d0/gpu_model",
            "/sys/class/kgsl/kgsl-3d0/devfreq/gpu_model",
            "/sys/devices/platform/mali.0/gpu_name",
            "/sys/devices/platform/mali.0/gpuinfo",
            "/sys/class/devfreq/gpumcu/device/gpuinfo",
            "/sys/class/devfreq/gpumcu/device/gpu_name",
            "/sys/kernel/gpu/gpu_model",
            "/sys/kernel/gpu/gpu_name",
            "/sys/module/pvrsrvkm/parameters/gpu_name",
            "/sys/devices/platform/pvr/gpu_name",
            "/sys/devices/gpu/gpu_name",
            "/sys/class/drm/card0/device/gpu_name"
        )
        for (path in kernelPaths) {
            if (nodeExists(path)) {
                val content = SmartShell.read(path).trim()
                if (content.isNotEmpty() && content.length < 50) return content
            }
        }

        // Stage 1: Standard SurfaceFlinger probe
        val sfLines = SmartShell.shLines("dumpsys SurfaceFlinger")
        val glesLine = sfLines.find { it.contains("GLES:", ignoreCase = true) || it.contains("Renderer:", ignoreCase = true) }
        if (glesLine != null) {
            val name = glesLine.substringAfter(":").substringBefore(",").trim()
            if (name.isNotEmpty() && name != "null") return name
        }

        // Stage 1.2: Dumpsys GpuService (Newer Androids)
        val gpuSvcLines = SmartShell.shLines("dumpsys GpuService")
        val vendorLine = gpuSvcLines.find { it.contains("GPU Renderer", ignoreCase = true) || it.contains("Renderer:", ignoreCase = true) }
        if (vendorLine != null) {
            val name = vendorLine.substringAfter(":").trim()
            if (name.isNotEmpty() && name != "null" && name != "unknown") return name
        }

        // Stage 1.5: Dumpsys gfxinfo
        val gfxLines = SmartShell.shLines("dumpsys gfxinfo")
        val glLine = gfxLines.find { it.contains("GL_RENDERER", ignoreCase = true) }
        if (glLine != null) {
            val name = glLine.substringAfter("=").trim()
            if (name.isNotEmpty() && name != "null") return name
        }

        // Stage 1.7: Logcat parsing (Aggressive fallback)
        val logcat = SmartShell.sh("logcat -d -s Adreno-GSL Mali PVR EGL-main | grep -i 'Renderer' | tail -n 1").trim()
        if (logcat.isNotEmpty()) {
            val name = logcat.substringAfter("Renderer").substringAfter(":").trim()
            if (name.isNotEmpty() && name != "null") return name
        }

        // Stage 2: Vendor Library Check (Very Reliable)
        val libs = SmartShell.sh("ls /vendor/lib/egl /system/lib/egl /vendor/lib64/egl /system/lib64/egl 2>/dev/null").lowercase()
        val libBrand = when {
            libs.contains("mali")  -> "Mali Graphics Core"
            libs.contains("adreno") -> "Adreno Graphics Core"
            libs.contains("pvr")   -> "PowerVR Graphics Core"
            libs.contains("tegra") -> "Nvidia Tegra GPU"
            else -> ""
        }
        if (libBrand.isNotEmpty()) return libBrand

        // Stage 3.5: Extremely Aggressive Prop Hunting
        val props = listOf(
            "ro.hardware.egl", "ro.hardware.vulkan", 
            "ro.board.platform", "ro.product.board", 
            "ro.soc.model", "ro.mediatek.platform", 
            "ro.chipname", "ro.hardware"
        )
        val fallbackProp = props.firstNotNullOfOrNull { 
            SmartShell.sh("getprop $it").trim().takeIf { res -> res.isNotEmpty() && res != "null" && res != "unknown" }
        }
        if (fallbackProp != null) return "GPU ($fallbackProp)"

        // Stage 4: Registry Fallback
        val reg = SystemDiscovery.getRegistry()
        if (reg.socModel.isNotEmpty()) return "GPU (${reg.socModel})"
        
        return "Generic Graphics Core"
    }

    /**
     * Retrieves real-time GPU Load percentage.
     */
    fun getLoad(): Int {
        val registryPath = SystemDiscovery.getRegistry().subsystems["GPU"]?.get("load")
        val raw = if (!registryPath.isNullOrEmpty()) SmartShell.read(registryPath).trim() else ""
        
        if (raw.isEmpty()) {
            // Fallback 1: Manual probe relative to discovered root
            val root = getGpuPath()
            val nodes = listOf("gpu_busy_percentage", "gpubusy", "utilization", "load", "busy_percent", "device/load", "device/gpu_busy", "devfreq/gpu_load")
            var fallbackRaw = nodes.firstNotNullOfOrNull { node ->
                if (nodeExists("$root/$node")) SmartShell.read("$root/$node").trim().takeIf { it.isNotEmpty() }
                else null
            }

            // Fallback 2: Absolute paths for heavily restricted kernels (DebugFS, MTK GED, Exynos, Unisoc)
            if (fallbackRaw == null) {
                val absolutePaths = listOf(
                    // Mali / ARM
                    "/sys/kernel/debug/mali0/utilization",
                    "/sys/kernel/debug/mali/utilization",
                    "/sys/devices/platform/mali.0/utilization",
                    "/sys/devices/13000000.mali/utilization",
                    "/sys/devices/11400000.mali/utilization",
                    "/sys/devices/1c000000.mali/utilization",
                    "/sys/devices/14ac0000.mali/utilization",
                    "/sys/devices/18800000.mali/utilization",
                    "/sys/devices/2d00000.mali/utilization",
                    "/sys/class/devfreq/mali/device/load",
                    "/sys/class/devfreq/mali0/device/load",
                    "/sys/class/devfreq/gpumcu/device/load",
                    "/sys/kernel/debug/ged/hal/gpu_utilization",
                    "/sys/module/mali_kbase/parameters/gpu_utilization",
                    "/sys/module/mali_base/parameters/gpu_utilization",
                    
                    // Adreno / Qualcomm
                    "/sys/class/kgsl/kgsl-3d0/gpubusy",
                    "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
                    "/sys/class/kgsl/kgsl-3d0/devfreq/gpu_load",
                    "/sys/devices/platform/soc/5000000.qcom,kgsl-3d0/kgsl/kgsl-3d0/gpubusy",
                    "/sys/devices/platform/soc/3d00000.qcom,kgsl-3d0/kgsl/kgsl-3d0/gpubusy",
                    "/sys/class/devfreq/soc:qcom,kgsl-3d0/device/gpu_busy_percentage",
                    "/sys/class/devfreq/soc:qcom,kgsl-3d0/device/load",
                    
                    // PowerVR / Imagination
                    "/sys/module/pvrsrvkm/parameters/gpu_utilization",
                    "/sys/kernel/debug/pvr/gpu_utilization",
                    "/sys/devices/platform/pvrsrvkm.0/gpu_utilization",
                    "/sys/devices/platform/pvr/gpu_utilization",
                    
                    // Exynos / Samsung
                    "/sys/kernel/gpu/gpu_busy",
                    "/sys/class/devfreq/exynos-bus/device/load",
                    
                    // Nvidia Tegra
                    "/sys/devices/platform/host1x/15810000.nvdisplay/load",
                    "/sys/devices/57000000.gpu/load",
                    
                    // Generic DRM & Misc
                    "/sys/class/devfreq/gpu/device/load",
                    "/sys/class/drm/card0/device/gpu_busy",
                    "/sys/class/drm/card0/device/load"
                )
                fallbackRaw = absolutePaths.firstNotNullOfOrNull { path ->
                    if (nodeExists(path)) SmartShell.read(path).trim().takeIf { it.isNotEmpty() }
                    else null
                }
            }
            
            // Fallback 3: Try reading freq to guess load (Hack for entirely locked kernels)
            if (fallbackRaw == null) {
                val freq = getCurrentFrequency()
                val minFreq = getAvailableFrequencies().lastOrNull()?.replace(" MHz", "")?.toIntOrNull() ?: 0
                val maxFreq = getAvailableFrequencies().firstOrNull()?.replace(" MHz", "")?.toIntOrNull() ?: 1
                val curFreqNum = freq.replace(" MHz", "").toIntOrNull() ?: 0
                if (maxFreq > minFreq && curFreqNum > 0) {
                    val percent = ((curFreqNum - minFreq).toFloat() / (maxFreq - minFreq).toFloat() * 100).toInt()
                    return percent.coerceIn(0, 100)
                }
            }
            
            return parseLoad(fallbackRaw ?: "0")
        }
        return parseLoad(raw)
    }

    private fun parseLoad(raw: String): Int {
        return try {
            when {
                // Qualcomm gpubusy: "busy total" -> busy * 100 / total
                raw.contains(" ") -> {
                    val parts = raw.split(Regex("\\s+")).filter { it.isNotEmpty() }
                    if (parts.size >= 2) {
                        val busy = parts[0].toLongOrNull() ?: 0L
                        val total = parts[1].toLongOrNull() ?: 1L
                        if (total > 0) ((busy * 100) / total).toInt() else 0
                    } else 0
                }
                raw.contains("%") -> raw.substringBefore("%").trim().toIntOrNull() ?: 0
                else -> raw.trim().toIntOrNull() ?: 0
            }.coerceIn(0, 100)
        } catch (e: Exception) { 0 }
    }

    /**
     * Retrieves GPU Bus Speed / Memory Clock if available.
     * Incorporates deep memory controller interconnect probes.
     */
    fun getBusSpeed(): String {
        val candidates = listOf(
            // Qualcomm BIMC/LLCC/Memlat
            "/sys/class/devfreq/soc:qcom,gpubw/cur_freq",
            "/sys/class/devfreq/soc:qcom,cpubw/cur_freq",
            "/sys/class/devfreq/soc:qcom,memlat-cpu0/cur_freq",
            "/sys/class/devfreq/soc:qcom,kgsl-busmon/cur_freq",
            "/sys/devices/platform/soc/soc:qcom,gpubw/devfreq/soc:qcom,gpubw/cur_freq",
            
            // MediaTek DVFSRC & GPUMCU
            "/sys/class/devfreq/mtk-dvfsrc-devfreq/cur_freq",
            "/sys/class/devfreq/gpumcu/cur_freq",
            "/sys/class/devfreq/mali_bw/cur_freq",
            "/sys/kernel/debug/ged/hal/gpu_bw",
            
            // Exynos MIF/INT
            "/sys/class/devfreq/17000000.devfreq_mif/cur_freq",
            "/sys/class/devfreq/17000000.devfreq_int/cur_freq",
            "/sys/class/devfreq/exynos-bus/cur_freq",
            "/sys/devices/platform/exynos-bus/devfreq/exynos-bus/cur_freq",
            
            // Generic Mali
            "/sys/class/devfreq/18800000.mali/cur_freq",
            "/sys/class/devfreq/13000000.mali/cur_freq",
            "/sys/class/devfreq/11400000.mali/cur_freq",
            "/sys/class/devfreq/1c000000.mali/cur_freq",
            "/sys/class/devfreq/2d00000.mali/cur_freq",
            "/sys/class/devfreq/gpu_bw/cur_freq",
            "/sys/kernel/debug/mali0/bw"
        )
        val raw = candidates.firstNotNullOfOrNull { path ->
            if (nodeExists(path)) SmartShell.read(path).trim().takeIf { it.isNotEmpty() } else null
        } ?: ""
        return if (raw.isNotEmpty()) formatFreq(raw) else "—"
    }

    /**
     * Resolves the actual Thermal Zone representing the GPU to provide accurate temperatures.
     */
    fun getTemperature(): String {
        // Fallback literal paths
        val literals = listOf(
            "/sys/class/kgsl/kgsl-3d0/temp",
            "/sys/class/kgsl/kgsl-3d0/gpu_temp",
            "/sys/kernel/debug/mali0/temp",
            "/sys/devices/virtual/thermal/thermal_zone_gpu/temp",
            "/sys/class/thermal/thermal_zone_gpu/temp",
            "/sys/class/thermal/thermal_zone_gpuss/temp",
            "/sys/devices/virtual/thermal/thermal_zone_gpuss/temp",
            "/sys/devices/virtual/thermal/thermal_zone1/temp" // Often GPU on MTK
        )
        for (path in literals) {
            if (nodeExists(path)) {
                val rawTemp = SmartShell.read(path).trim().toFloatOrNull()
                if (rawTemp != null) {
                    return if (rawTemp > 1000) String.format("%.1f°C", rawTemp / 1000f)
                    else "${rawTemp.toInt()}°C"
                }
            }
        }
        return "N/A"
    }

    /**
     * Retrieves GPU Memory Usage natively from KGSL or Memory Controllers.
     */
    fun getMemoryUsage(): String {
        val paths = listOf(
            "/sys/class/kgsl/kgsl-3d0/page_alloc",
            "/sys/devices/platform/soc/5000000.qcom,kgsl-3d0/kgsl/kgsl-3d0/page_alloc",
            "/sys/kernel/debug/mali0/memory_usage",
            "/sys/devices/platform/mali.0/memory_usage",
            "/sys/module/pvrsrvkm/parameters/gpu_memory_usage",
            "/sys/kernel/gpu/gpu_memory"
        )
        for (path in paths) {
            if (nodeExists(path)) {
                val raw = SmartShell.read(path).trim()
                val bytes = raw.toLongOrNull()
                if (bytes != null) return String.format("%.1f MB", bytes / 1024f / 1024f)
            }
        }
        return "N/A"
    }

    /**
     * Lists available Power Policies (e.g., Coarse, Fine, Always On, default_pwrlevel).
     */
    fun getAvailablePowerPolicies(): List<String> {
        val root = getGpuPath()
        if (nodeExists("$root/default_pwrlevel")) {
            // Adreno typically has power levels based on frequencies, but as an abstraction:
            return listOf("0 (Max Performance)", "1", "2", "3", "4", "5 (Max Power Save)")
        }
        if (nodeExists("$root/power_policy")) {
            // Mali has specific policies
            val res = SmartShell.read("$root/available_power_policies").trim()
            if (res.isNotEmpty()) return res.split(" ").filter { it.isNotEmpty() }
            return listOf("coarse_demand", "always_on", "fine_demand")
        }
        return emptyList()
    }

    /**
     * Identifies the current Power Policy.
     */
    fun getCurrentPowerPolicy(): String {
        val root = getGpuPath()
        if (nodeExists("$root/default_pwrlevel")) {
            val level = SmartShell.read("$root/default_pwrlevel").trim()
            return when (level) {
                "0" -> "0 (Max Performance)"
                "5" -> "5 (Max Power Save)"
                else -> level
            }
        }
        if (nodeExists("$root/power_policy")) {
            return SmartShell.read("$root/power_policy").trim()
        }
        return "N/A"
    }

    /**
     * Applies the selected Power Policy.
     */
    fun setPowerPolicy(policy: String): Boolean {
        log("Initiating GPU power policy transition to: $policy")
        val root = getGpuPath()
        val parsedPolicy = policy.substringBefore(" (").trim()
        
        if (nodeExists("$root/default_pwrlevel")) {
            return SmartShell.write("$root/default_pwrlevel", parsedPolicy)
        }
        if (nodeExists("$root/power_policy")) {
            return SmartShell.write("$root/power_policy", parsedPolicy)
        }
        return false
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

        val root = getGpuPath()
        if (root.isNotEmpty()) {
            val nodes = listOf(
                "devfreq/kgsl-3d0/cur_freq", "devfreq/cur_freq", "cur_freq", 
                "gpuclk", "clock", "clock_rate", "device/cur_freq", "kgsl/kgsl-3d0/devfreq/cur_freq", "freq"
            )
            var raw = nodes.firstNotNullOfOrNull { node ->
                if (nodeExists("$root/$node")) SmartShell.read("$root/$node").trim().takeIf { it.isNotEmpty() }
                else null
            }
            
            // Fallback Absolute Paths
            if (raw == null) {
                val absolutePaths = listOf(
                    "/sys/class/kgsl/kgsl-3d0/gpuclk",
                    "/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq",
                    "/sys/devices/platform/mali.0/clock",
                    "/sys/class/devfreq/gpumcu/cur_freq",
                    "/sys/class/devfreq/mali/cur_freq"
                )
                raw = absolutePaths.firstNotNullOfOrNull { path ->
                    if (nodeExists(path)) SmartShell.read(path).trim().takeIf { it.isNotEmpty() }
                    else null
                }
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
            val freq = raw.trim().toLong()
            when {
                // If it's in Hz (Adreno/Mali standard)
                freq > 1000000000 -> "${freq / 1000000} MHz" // usually shown in MHz even if high
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
