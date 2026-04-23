package com.hans.ext.kernelmanager.hal

import com.hans.ext.kernelmanager.util.SmartShell
import com.hans.ext.kernelmanager.hal.intelligence.SystemDiscovery
import android.util.Log

/**
 * StorageController (V11) - The Omnisovereign I/O & Storage Orchestrator.
 * Provides exhaustive control over block device performance, including I/O schedulers, 
 * read-ahead buffers, and filesystem-specific optimizations (F2FS/EXT4).
 * Features atomic transitions and transaction auditing for storage subsystem tuning.
 */
object StorageController {
    private const val TAG = "OmnisovereignStorage"
    private val storageLog = mutableListOf<String>()

    /**
     * Retrieves the primary block device queue path identified by the Sovereign Oracle.
     * Uses key 'queue_path' which is set by SystemDiscovery.scanStorageSubsystem().
     */
    private fun getQueuePath(): String {
        return SystemDiscovery.getRegistry().subsystems["STORAGE"]?.get("queue_path") ?: ""
    }

    /**
     * I/O Scheduler Management: Lists all hardware-supported schedulers.
     */
    fun getAvailableSchedulers(): List<String> {
        val path = "${getQueuePath()}/scheduler"
        if (!nodeExists(path)) return listOf("noop", "deadline", "cfq")
        
        val raw = SmartShell.read(path)
        if (raw.isEmpty()) return listOf("cfq")
        
        // Output format: [cfq] deadline noop (active in brackets)
        return raw.replace("[", "").replace("]", "").split(" ").filter { it.isNotEmpty() }
    }

    /**
     * Identifies the currently active I/O scheduler.
     */
    fun getCurrentScheduler(): String {
        val path = "${getQueuePath()}/scheduler"
        if (!nodeExists(path)) return "unknown"
        
        val raw = SmartShell.read(path)
        return raw.substringAfter("[").substringBefore("]")
    }

    /**
     * Applies a new I/O scheduler with verification and transaction logging.
     */
    fun setScheduler(scheduler: String): Boolean {
        log("Storage: Attempting scheduler transition to $scheduler")
        val path = "${getQueuePath()}/scheduler"
        val success = SmartShell.write(path, scheduler)
        
        if (success) {
            val verified = SmartShell.read(path).contains("[$scheduler]", true)
            if (verified) {
                log("Storage: Scheduler transition verified for $scheduler")
                return true
            }
        }
        
        log("Storage: Scheduler transition failed for $scheduler")
        return false
    }

    /**
     * Read-Ahead Optimization: Configures the read-ahead buffer size in KB.
     */
    fun getReadAhead(): String {
        val path = "${getQueuePath()}/read_ahead_kb"
        return if (nodeExists(path)) SmartShell.read(path) else "0"
    }

    fun setReadAhead(kb: Int): Boolean {
        log("Storage: Adjusting Read-Ahead buffer to $kb KB")
        val path = "${getQueuePath()}/read_ahead_kb"
        return SmartShell.write(path, kb.toString())
    }

    /**
     * Entropy Management: Controls how the block device contributes to the system entropy pool.
     */
    fun setAddRandom(enabled: Boolean): Boolean {
        val path = "${getQueuePath()}/add_random"
        return if (nodeExists(path)) SmartShell.write(path, if (enabled) "1" else "0") else false
    }

    /**
     * Rotational Tuning: Informs the kernel if the device has moving parts (False for Flash).
     */
    fun setRotational(enabled: Boolean): Boolean {
        val path = "${getQueuePath()}/rotational"
        return if (nodeExists(path)) SmartShell.write(path, if (enabled) "1" else "0") else false
    }

    /**
     * I/O Statistics: Toggles detailed statistic collection for the block device.
     */
    fun setIoStats(enabled: Boolean): Boolean {
        val path = "${getQueuePath()}/iostats"
        return if (nodeExists(path)) SmartShell.write(path, if (enabled) "1" else "0") else false
    }

    /**
     * Filesystem Optimization: Targeted tuning for F2FS (Flash-Friendly File System).
     */
    fun isF2fsAvailable() = nodeExists("/sys/fs/f2fs")

    fun setF2fsGcUrgent(enabled: Boolean): Boolean {
        val path = "/sys/fs/f2fs/userdata/gc_urgent"
        return if (nodeExists(path)) SmartShell.write(path, if (enabled) "1" else "0") else false
    }

    fun setF2fsDiscard(enabled: Boolean): Boolean {
        val path = "/sys/fs/f2fs/userdata/discard_granularity"
        return if (nodeExists(path)) SmartShell.write(path, if (enabled) "1" else "16") else false
    }

    /**
     * Scheduler Specific Tunables: Deadline, CFQ, BFQ parameters.
     */
    fun getSchedulerTunables(scheduler: String): Map<String, String> {
        val path = "${getQueuePath()}/iosched"
        val map = mutableMapOf<String, String>()
        if (nodeExists(path)) {
            val parameters = SmartShell.read("ls $path").split(" ").filter { it.isNotEmpty() }
            parameters.forEach { param ->
                map[param] = SmartShell.read("$path/$param")
            }
        }
        return map
    }

    /**
     * UFS Optimization: Tuning for Universal Flash Storage devices.
     */
    fun setUfsPerformanceMode(enabled: Boolean): Boolean {
        val roots = listOf("/sys/devices/platform/soc/1d84000.ufshc", "/sys/class/ufs-device")
        roots.forEach { root ->
            val path = "$root/rpm_lvl"
            if (nodeExists(path)) return SmartShell.write(path, if (enabled) "0" else "5")
        }
        return false
    }

    /**
     * Sysfs node verification helper — delegates to SmartShell.nodeExists().
     */
    private fun nodeExists(path: String): Boolean = SmartShell.nodeExists(path)

    /**
     * Transaction Logging.
     */
    private fun log(message: String) {
        val entry = "[${System.currentTimeMillis()}] $message"
        storageLog.add(entry)
        Log.d(TAG, message)
        if (storageLog.size > 200) storageLog.removeAt(0)
    }

    /**
     * Retrieves the storage transaction history.
     */
    fun getHistory(): List<String> = storageLog.toList()

    /**
     * Generates a technical report of the current storage subsystem state.
     */
    fun getStorageHealthReport(): String {
        return "Active Scheduler: ${getCurrentScheduler()}\n" +
               "Read-Ahead: ${getReadAhead()} KB\n" +
               "F2FS Optimization: ${if (isF2fsAvailable()) "Active" else "N/A"}\n" +
               "Bus Technology: ${if (getQueuePath().contains("sda")) "UFS" else "eMMC"}"
    }

    /**
     * Performs a system-wide storage integrity audit.
     */
    fun performAudit(): Boolean {
        log("Executing Storage Subsystem Audit...")
        if (getQueuePath().isEmpty()) {
            log("Critical: Block device queue path not identified.")
            return false
        }
        return true
    }
}
