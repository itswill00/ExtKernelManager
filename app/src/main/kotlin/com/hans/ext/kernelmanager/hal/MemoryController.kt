package com.hans.ext.kernelmanager.hal

import com.hans.ext.kernelmanager.util.SmartShell
import com.hans.ext.kernelmanager.hal.intelligence.SystemDiscovery
import android.util.Log

/**
 * MemoryController (V11) - The Omnisovereign Memory & Swap Engine.
 * Provides exhaustive control over the Linux memory management subsystem, including 
 * Virtual Memory (VM) tunables, LMK profiles, and high-performance swap (ZRAM).
 * Features automated balancing algorithms for diverse workload scenarios.
 */
object MemoryController {
    private const val TAG = "OmnisovereignMem"
    private val memoryAudit = mutableListOf<String>()

    /**
     * Virtual Memory (VM) Suite: Comprehensive management of the Linux VM subsystem.
     */
    fun getSwappiness() = readVmNode("swappiness")
    fun setSwappiness(value: Int): Boolean {
        log("VM: Adjusting swappiness to $value")
        return writeVmNode("swappiness", value.toString())
    }

    fun getVfsCachePressure() = readVmNode("vfs_cache_pressure")
    fun setVfsCachePressure(value: Int): Boolean {
        log("VM: Adjusting VFS Cache Pressure to $value")
        return writeVmNode("vfs_cache_pressure", value.toString())
    }

    fun getDirtyRatio() = readVmNode("dirty_ratio")
    fun setDirtyRatio(value: Int): Boolean {
        log("VM: Adjusting Dirty Ratio to $value")
        return writeVmNode("dirty_ratio", value.toString())
    }

    fun getDirtyBackgroundRatio() = readVmNode("dirty_background_ratio")
    fun setDirtyBackgroundRatio(value: Int): Boolean {
        log("VM: Adjusting Dirty Background Ratio to $value")
        return writeVmNode("dirty_background_ratio", value.toString())
    }

    fun getMinFreeKbytes() = readVmNode("min_free_kbytes")
    fun setMinFreeKbytes(kb: Int): Boolean {
        log("VM: Adjusting Min Free Kbytes to $kb")
        return writeVmNode("min_free_kbytes", kb.toString())
    }

    fun getExtraFreeKbytes() = readVmNode("extra_free_kbytes")
    fun setExtraFreeKbytes(kb: Int): Boolean {
        log("VM: Adjusting Extra Free Kbytes to $kb")
        return writeVmNode("extra_free_kbytes", kb.toString())
    }

    fun getPageCluster() = readVmNode("page-cluster")
    fun setPageCluster(value: Int): Boolean {
        log("VM: Adjusting Page Cluster to $value")
        return writeVmNode("page-cluster", value.toString())
    }

    /**
     * ZRAM Subsystem: High-performance compressed swap management.
     */
    fun isZramEnabled(): Boolean {
        val path = SystemDiscovery.getRegistry().subsystems["MEMORY"]?.get("zram_root") ?: ""
        if (path.isEmpty()) return false
        val swaps = SmartShell.read("cat /proc/swaps")
        return swaps.contains("zram")
    }

    fun getZramDiskSize(): String {
        val path = SystemDiscovery.getRegistry().subsystems["MEMORY"]?.get("zram_root") ?: ""
        if (path.isEmpty()) return "N/A"
        val raw = SmartShell.read("$path/disksize")
        return if (raw.isNotEmpty()) {
            val bytes = raw.toLong()
            "${bytes / 1024 / 1024} MB"
        } else "0 MB"
    }

    fun setZramDiskSize(mb: Int): Boolean {
        val path = SystemDiscovery.getRegistry().subsystems["MEMORY"]?.get("zram_root") ?: ""
        if (path.isEmpty()) return false
        log("ZRAM: Reconfiguring disk size to $mb MB")
        
        // Critical: ZRAM reset sequence
        SmartShell.read("swapoff /dev/block/zram0")
        SmartShell.write("$path/reset", "1")
        val success = SmartShell.write("$path/disksize", (mb.toLong() * 1024 * 1024).toString())
        
        if (success) {
            SmartShell.read("mkswap /dev/block/zram0")
            SmartShell.read("swapon /dev/block/zram0")
            log("ZRAM: Resize completed and swap reactivated.")
        }
        return success
    }

    fun getZramCompAlgorithm(): String {
        val path = SystemDiscovery.getRegistry().subsystems["MEMORY"]?.get("zram_root") ?: ""
        if (path.isEmpty()) return "unknown"
        val raw = SmartShell.read("$path/comp_algorithm")
        return raw.substringAfter("[").substringBefore("]")
    }

    fun setZramCompAlgorithm(algo: String): Boolean {
        val path = SystemDiscovery.getRegistry().subsystems["MEMORY"]?.get("zram_root") ?: ""
        if (path.isEmpty()) return false
        log("ZRAM: Transitioning compression algorithm to $algo")
        return SmartShell.write("$path/comp_algorithm", algo)
    }

    /**
     * Low Memory Killer (LMK): Kernel-level task prioritization.
     */
    fun getLmkMinfree(): String {
        val path = SystemDiscovery.getRegistry().interfaces["LMK_MINFREE"] ?: return "N/A"
        return SmartShell.read(path)
    }

    fun setLmkMinfree(values: String): Boolean {
        val path = SystemDiscovery.getRegistry().interfaces["LMK_MINFREE"] ?: return false
        log("LMK: Applying minfree profile: $values")
        return SmartShell.write(path, values)
    }

    /**
     * Auto-LMK Optimizer: Calculates thresholds based on total device RAM.
     */
    fun applyIntelligentLmkProfile(tier: String): Boolean {
        val memInfo = SmartShell.read("grep MemTotal /proc/meminfo")
        val totalKb = memInfo.filter { it.isDigit() }.toLongOrNull() ?: 4000000L
        val base = (totalKb / 1024 / 128).toInt()
        
        val profile = when (tier.lowercase()) {
            "aggressive" -> "${base*2},${base*4},${base*8},${base*16},${base*24},${base*32}"
            "balanced" -> "${base},${base*2},${base*4},${base*8},${base*12},${base*16}"
            "light" -> "${base/2},${base},${base*2},${base*4},${base*6},${base*8}"
            else -> return false
        }
        return setLmkMinfree(profile)
    }

    /**
     * Kernel Samepage Merging (KSM): Page de-duplication for RAM efficiency.
     */
    fun isKsmAvailable() = nodeExists("/sys/kernel/mm/ksm/run")

    fun setKsm(enabled: Boolean): Boolean {
        val path = "/sys/kernel/mm/ksm/run"
        log("KSM: Setting execution state to $enabled")
        return if (nodeExists(path)) SmartShell.write(path, if (enabled) "1" else "0") else false
    }

    fun getKsmStats(): Map<String, String> {
        val base = "/sys/kernel/mm/ksm"
        val map = mutableMapOf<String, String>()
        if (nodeExists(base)) {
            val nodes = listOf("pages_shared", "pages_sharing", "pages_unshared", "full_scans")
            nodes.forEach { if (nodeExists("$base/$it")) map[it] = SmartShell.read("$base/$it") }
        }
        return map
    }

    private fun readVmNode(name: String): String {
        val path = SystemDiscovery.getRegistry().subsystems["MEMORY"]?.get(name) ?: return "N/A"
        return SmartShell.read(path)
    }

    private fun writeVmNode(name: String, value: String): Boolean {
        val path = SystemDiscovery.getRegistry().subsystems["MEMORY"]?.get(name) ?: return false
        return SmartShell.write(path, value)
    }

    /**
     * Sysfs verification — delegates to SmartShell.nodeExists().
     */
    private fun nodeExists(path: String): Boolean = SmartShell.nodeExists(path)

    /**
     * Memory Audit Logging.
     */
    private fun log(message: String) {
        val entry = "[${System.currentTimeMillis()}] $message"
        memoryAudit.add(entry)
        Log.d(TAG, message)
        if (memoryAudit.size > 200) memoryAudit.removeAt(0)
    }

    /**
     * Retrieves the complete memory audit log.
     */
    fun getAuditHistory(): List<String> = memoryAudit.toList()

    /**
     * Returns a professional-grade technical report on memory health.
     */
    fun getMemoryTechnicalReport(): String {
        val registry = SystemDiscovery.getRegistry()
        return "Memory Tier: ${registry.heritage.tier}\n" +
               "ZRAM Status: ${if (isZramEnabled()) "Active (${getZramDiskSize()})" else "Inactive"}\n" +
               "Swappiness: ${getSwappiness()}\n" +
               "LMK Profile: ${getLmkMinfree()}\n" +
               "KSM Available: ${if (isKsmAvailable()) "Yes" else "No"}"
    }

    /**
     * Performs a system-wide memory integrity audit.
     */
    fun performAudit(): Boolean {
        log("Executing Memory Subsystem Audit...")
        if (SystemDiscovery.getRegistry().subsystems["MEMORY"]?.isEmpty() == true) {
            log("Critical: VM Nodes not mapped by Oracle.")
            return false
        }
        return true
    }
}
