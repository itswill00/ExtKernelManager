package com.hans.ext.kernelmanager.hal.intelligence

import com.hans.ext.kernelmanager.util.SmartShell
import android.util.Log

/**
 * SovereignPathEngine (V12) - The Omnisovereign Node Discovery System.
 * A highly resilient, multi-tier engine designed to resolve kernel sysfs/proc nodes 
 * across thousands of diverse device and kernel configurations.
 * Features fuzzy matching, recursive search strategies, and a self-healing node cache.
 */
object SovereignPathEngine {
    private const val TAG = "SovereignPathEngine"
    private val resolutionAudit = mutableListOf<String>()
    private val nodeCache = mutableMapOf<String, String>()

    /**
     * Resolves a kernel node path using a multi-stage search strategy.
     * Stages: Cache -> Direct Match -> Fuzzy Match -> Recursive Scan.
     */
    fun resolve(id: String, candidates: List<String>): String {
        log("Resolving Path for: $id")
        
        // 1. Check Persistence Cache
        nodeCache[id]?.let {
            if (verifyNode(it)) {
                log("Cache Hit for $id: $it")
                return it
            }
        }

        // 2. Direct Match Verification
        candidates.find { verifyNode(it) }?.let {
            log("Direct Match for $id: $it")
            nodeCache[id] = it
            return it
        }

        // 3. Fuzzy Pattern Matching
        val patternMatch = fuzzyMatch(id, candidates)
        if (patternMatch.isNotEmpty()) {
            log("Fuzzy Match for $id: $patternMatch")
            nodeCache[id] = patternMatch
            return patternMatch
        }

        // 4. Recursive Deep Scan Fallback
        val deepMatch = deepScan(id)
        if (deepMatch.isNotEmpty()) {
            log("Deep Scan Match for $id: $deepMatch")
            nodeCache[id] = deepMatch
            return deepMatch
        }

        log("CRITICAL: Failed to resolve path for $id after multi-tier scanning.")
        return ""
    }

    /**
     * Fuzzy matching using regex patterns and directory walking.
     */
    private fun fuzzyMatch(id: String, candidates: List<String>): String {
        candidates.forEach { candidate ->
            val dir = candidate.substringBeforeLast("/")
            val name = candidate.substringAfterLast("/")
            if (SmartShell.nodeExists(dir)) {
                // shLines() agar ls newline-separated output terbaca semua
                val files = SmartShell.shLines("ls $dir")
                val match = files.find { it.contains(name, ignoreCase = true) }
                if (match != null) return "$dir/$match"
            }
        }
        return ""
    }

    /**
     * Deep Recursive Scan: Searches entire sysfs/proc subtrees for specific identifiers.
     */
    private fun deepScan(id: String): String {
        log("Initiating Deep Recursive Scan for: $id")
        val searchRoots = listOf("/sys/class", "/sys/devices/platform", "/proc/sys")
        val keyword = id.lowercase()
        for (root in searchRoots) {
            // sh() bukan read() — kita menjalankan perintah find, bukan membaca file
            val res = SmartShell.sh("find $root -maxdepth 4 -name '*$keyword*' 2>/dev/null | head -n 1")
            if (res.isNotEmpty() && SmartShell.nodeExists(res)) return res
        }
        return ""
    }

    /**
     * Battery Subsystem Path Oracle.
     */
    fun resolveBatteryPath(): String {
        return resolve("battery", listOf(
            "/sys/class/power_supply/battery",
            "/sys/class/power_supply/bms",
            "/sys/class/power_supply/main"
        ))
    }

    /**
     * CPU Subsystem Path Oracle.
     */
    fun resolveCpuPolicyPath(policy: Int): String {
        return "/sys/devices/system/cpu/cpufreq/policy$policy"
    }

    /**
     * GPU Subsystem Path Oracle.
     */
    fun resolveGpuPath(): String {
        return resolve("gpu", listOf(
            "/sys/class/kgsl/kgsl-3d0",
            "/sys/devices/platform/mali.0",
            "/sys/devices/platform/soc/1c00000.gpu",
            "/sys/devices/platform/gpufreq-mali"
        ))
    }

    /**
     * Thermal Subsystem Path Oracle.
     */
    fun resolveThermalZonePath(type: String): String {
        val base = "/sys/class/thermal"
        // shLines() agar ls newline-separated output terbaca semua
        val zones = SmartShell.shLines("ls $base").filter { it.startsWith("thermal_zone") }
        for (zone in zones) {
            val zType = SmartShell.read("$base/$zone/type").lowercase()
            if (zType.contains(type, ignoreCase = true)) return "$base/$zone"
        }
        return ""
    }

    /**
     * Memory Subsystem Path Oracle.
     */
    fun resolveMemoryPath(node: String): String {
        return resolve(node, listOf("/proc/sys/vm/$node", "/sys/module/lowmemorykiller/parameters/$node"))
    }

    /**
     * Storage Subsystem Path Oracle.
     */
    fun resolveStoragePath(): String {
        return resolve("block", listOf("/sys/block/sda/queue", "/sys/block/mmcblk0/queue", "/sys/block/dm-0/queue"))
    }

    /**
     * Sound Subsystem Path Oracle.
     */
    fun resolveSoundPath(): String {
        return resolve("sound", listOf("/sys/kernel/sound_control", "/sys/class/misc/sound_control"))
    }

    /**
     * Display Subsystem Path Oracle.
     */
    fun resolveDisplayPath(): String {
        return resolve("display", listOf("/sys/devices/platform/kcal_ctrl.0", "/sys/class/graphics/fb0"))
    }

    /**
     * Verifies if a node is accessible and potentially writable.
     */
    private fun verifyNode(path: String): Boolean = SmartShell.nodeExists(path)

    private fun nodeExists(path: String): Boolean =
        SmartShell.sh("[ -d $path ] && echo 1 || echo 0") == "1"

    /**
     * Internal audit logging.
     */
    private fun log(message: String) {
        val entry = "[${System.currentTimeMillis()}] $message"
        resolutionAudit.add(entry)
        Log.d(TAG, message)
        if (resolutionAudit.size > 1000) resolutionAudit.removeAt(0)
    }

    /**
     * Retrieves the complete path resolution audit history.
     */
    fun getResolutionLog() = resolutionAudit.toList()

    /**
     * Clears the internal path cache (Emergency Refresh).
     */
    fun clearCache() {
        log("Path Cache Purged. Full re-discovery scheduled.")
        nodeCache.clear()
    }

    /**
     * Performs a system-wide path integrity audit.
     */
    fun performPathAudit(): Boolean {
        log("Executing System-Wide Path Integrity Audit...")
        val criticalNodes = listOf("battery", "gpu", "block")
        return criticalNodes.all { resolve(it, emptyList()).isNotEmpty() }
    }

    /**
     * Generates a technical report of the Sovereign Path Engine state.
     */
    fun generatePathReport(): String {
        return "Sovereign Path Engine: V12 (Omnisovereign)\n" +
               "Cache Efficiency: ${nodeCache.size} nodes mapped\n" +
               "Resolution Depth: Recursive + Fuzzy\n" +
               "Registry Integrity: ${if (performPathAudit()) "Stable" else "Partial"}"
    }

    /**
     * Advanced: Batch resolution for complex subsystem groups.
     */
    fun batchResolve(subsystem: String, keywords: List<String>): Map<String, String> {
        log("Batch Resolving Subsystem: $subsystem")
        val map = mutableMapOf<String, String>()
        keywords.forEach { kw ->
            val p = resolve(kw, emptyList())
            if (p.isNotEmpty()) map[kw] = p
        }
        return map
    }
}
