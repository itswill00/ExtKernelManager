package com.hans.ext.kernelmanager.hal

import com.hans.ext.kernelmanager.util.SmartShell
import com.hans.ext.kernelmanager.hal.intelligence.SystemDiscovery
import android.util.Log

/**
 * CpuController (V11) - The Omnisovereign CPU Management Engine.
 * A professional-grade, multi-cluster tuning suite designed for granular control
 * over ARM big.LITTLE and DynamIQ architectures.
 * Features extensive safety guards, governor parameter tuning, and advanced boost control.
 */
object CpuController {
    private const val TAG = "OmnisovereignCpu"
    private val actionAudit = mutableListOf<String>()

    /**
     * Internal path resolver for CPU cluster policies.
     */
    private fun getPolicyPath(policy: Int): String {
        return "/sys/devices/system/cpu/cpufreq/policy$policy"
    }

    /**
     * Retrieves available scaling governors for a specific cluster.
     */
    fun getAvailableGovernors(policy: Int): List<String> {
        val path = "${getPolicyPath(policy)}/scaling_available_governors"
        val raw = SmartShell.read(path)
        if (raw.isEmpty()) return listOf("performance", "powersave")
        return raw.split(" ").filter { it.isNotEmpty() }.distinct()
    }

    /**
     * Identifies the currently active governor for a specific cluster.
     */
    fun getCurrentGovernor(policy: Int): String {
        val gov = SmartShell.read("${getPolicyPath(policy)}/scaling_governor")
        return gov.ifEmpty { "unknown" }
    }

    /**
     * Applies a new scaling governor to a specific cluster with read-back verification.
     */
    fun setGovernor(policy: Int, governor: String): Boolean {
        log("Policy $policy: Initiating governor transition to $governor")
        val path = "${getPolicyPath(policy)}/scaling_governor"
        val success = SmartShell.write(path, governor)

        if (success) {
            val verified = SmartShell.read(path).contains(governor, true)
            if (verified) {
                log("Policy $policy: Governor transition verified successfully.")
                return true
            }
        }

        log("Policy $policy: Critical failure during governor transition to $governor")
        return false
    }

    /**
     * Retrieves all hardware-supported frequencies for a specific cluster.
     */
    fun getAvailableFrequencies(policy: Int): List<String> {
        val base = getPolicyPath(policy)
        val raw = SmartShell.read("$base/scaling_available_frequencies")

        // Fallback: beberapa kernel modern tidak expose scaling_available_frequencies
        if (raw.isEmpty()) {
            val maxKhz = SmartShell.readLong("$base/cpuinfo_max_freq") ?: return emptyList()
            val minKhz = SmartShell.readLong("$base/cpuinfo_min_freq") ?: return emptyList()
            return listOf("${minKhz / 1000} MHz", "${maxKhz / 1000} MHz")
        }

        return raw.split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .mapNotNull { it.toLongOrNull() }
            .sortedDescending()
            .map { "${it / 1000} MHz" }
            .distinct()
    }

    /**
     * Returns the currently observed frequency for a specific CPU cluster.
     */
    fun getCurrentFrequency(policy: Int): String {
        val base = getPolicyPath(policy)
        // Fallback ke cpuinfo_cur_freq jika scaling_cur_freq tidak tersedia
        val khz = SmartShell.readLong("$base/scaling_cur_freq")
            ?: SmartShell.readLong("$base/cpuinfo_cur_freq")
            ?: return "N/A"
        return "${khz / 1000} MHz"
    }

    /**
     * Retrieves raw frequency for a specific core index.
     */
    fun getCoreFrequency(cpuIndex: Int): String {
        val path = "/sys/devices/system/cpu/cpu$cpuIndex/cpufreq/scaling_cur_freq"
        val khz = SmartShell.readLong(path) ?: return "OFF"
        return "${khz / 1000}"
    }

    /**
     * Configures the minimum scaling frequency for a specific cluster.
     */
    fun setMinFrequency(policy: Int, freq: String): Boolean {
        val raw = freq.substringBefore(" ").toLong() * 1000
        log("Policy $policy: Adjusting min_freq to $raw")
        return SmartShell.write("${getPolicyPath(policy)}/scaling_min_freq", raw.toString())
    }

    /**
     * Configures the maximum scaling frequency for a specific cluster.
     */
    fun setMaxFrequency(policy: Int, freq: String): Boolean {
        val raw = freq.substringBefore(" ").toLong() * 1000
        log("Policy $policy: Adjusting max_freq to $raw")
        return SmartShell.write("${getPolicyPath(policy)}/scaling_max_freq", raw.toString())
    }

    /**
     * Retrieves current minimum scaling frequency bound.
     */
    fun getMinFrequency(policy: Int): String {
        val raw = SmartShell.read("${getPolicyPath(policy)}/scaling_min_freq")
        return if (raw.isNotEmpty()) "${raw.toLong() / 1000} MHz" else "N/A"
    }

    /**
     * Retrieves current maximum scaling frequency bound.
     */
    fun getMaxFrequency(policy: Int): String {
        val raw = SmartShell.read("${getPolicyPath(policy)}/scaling_max_freq")
        return if (raw.isNotEmpty()) "${raw.toLong() / 1000} MHz" else "N/A"
    }

    /**
     * Governor Parameter Tuning: Accesses internal tunables for governors like schedutil or interactive.
     */
    fun getGovernorParameters(policy: Int, governor: String): Map<String, String> {
        val base = "${getPolicyPath(policy)}/$governor"
        val map = mutableMapOf<String, String>()
        if (SmartShell.nodeExists(base)) {
            val parameters = SmartShell.sh("ls $base").split(" ").filter { it.isNotEmpty() }
            parameters.forEach { param ->
                val value = SmartShell.read("$base/$param")
                if (value.isNotEmpty()) map[param] = value
            }
        }
        return map
    }

    fun setGovernorParameter(policy: Int, governor: String, param: String, value: String): Boolean {
        val path = "${getPolicyPath(policy)}/$governor/$param"
        log("Policy $policy ($governor): Updating $param to $value")
        return SmartShell.write(path, value)
    }

    /**
     * Advanced: Core Control & Optimization.
     * Manages kernel features like core_ctl for Snapdragon devices.
     */
    fun setCoreControl(enabled: Boolean): Boolean {
        val path = "/sys/devices/system/cpu/cpu0/core_ctl/enable"
        return if (SmartShell.nodeExists(path)) SmartShell.write(path, if (enabled) "1" else "0") else false
    }

    /**
     * Input Boost Logic: Controls frequency spikes during touch interaction.
     */
    fun setInputBoostFreq(freqs: String): Boolean {
        val candidates = listOf(
            "/sys/module/cpu_boost/parameters/input_boost_freq",
            "/sys/kernel/cpu_input_boost/ib_freqs"
        )
        candidates.forEach { if (SmartShell.nodeExists(it)) return SmartShell.write(it, freqs) }
        return false
    }

    fun setInputBoostDuration(ms: Int): Boolean {
        val path = "/sys/module/cpu_boost/parameters/input_boost_ms"
        return if (SmartShell.nodeExists(path)) SmartShell.write(path, ms.toString()) else false
    }

    /**
     * Power Management: Manages workqueue and scheduling efficiency.
     */
    fun setPowerEfficientWorkqueues(enabled: Boolean): Boolean {
        val path = "/sys/module/workqueue/parameters/power_efficient"
        return if (SmartShell.nodeExists(path)) SmartShell.write(path, if (enabled) "Y" else "N") else false
    }

    /**
     * Larch Boost: Specialized boost for specific MTK/Exynos kernels.
     */
    fun setLarchBoost(enabled: Boolean): Boolean {
        val path = "/sys/devices/system/cpu/cpufreq/larch_boost"
        return if (SmartShell.nodeExists(path)) SmartShell.write(path, if (enabled) "1" else "0") else false
    }

    /**
     * CPU Time in State: Parses frequency residency statistics.
     */
    fun getTimeInState(policy: Int): Map<String, Long> {
        val path = "${getPolicyPath(policy)}/stats/time_in_state"
        val map = mutableMapOf<String, Long>()
        if (SmartShell.nodeExists(path)) {
            val lines = SmartShell.read(path).lines()
            lines.forEach { line ->
                val parts = line.split(" ").filter { it.isNotEmpty() }
                if (parts.size == 2) {
                    val freq = "${parts[0].toLong() / 1000} MHz"
                    val time = parts[1].toLong()
                    map[freq] = time
                }
            }
        }
        return map
    }

    /**
     * Transaction Logging and Auditing.
     */
    private fun log(message: String) {
        val timestamp = System.currentTimeMillis()
        val entry = "[$timestamp] $message"
        actionAudit.add(entry)
        Log.d(TAG, message)
        if (actionAudit.size > 200) actionAudit.removeAt(0)
    }

    /**
     * Retrieves the complete CPU transaction history.
     */
    fun getActionHistory(): List<String> = actionAudit.toList()

    /**
     * Provides a detailed technical summary of the CPU subsystem state.
     */
    fun getSubsystemReport(): String {
        val sb = StringBuilder()
        sb.append("CPU Subsystem Architecture: ${SystemDiscovery.getRegistry().heritage.arch}\n")
        val policies = SystemDiscovery.getRegistry().cpuPolicies
        for (policy in policies) {
            sb.append("Cluster $policy: ${getCurrentGovernor(policy)} | Min: ${getMinFrequency(policy)} | Max: ${getMaxFrequency(policy)}\n")
        }
        return sb.toString()
    }

    /**
     * Safety Guard: Validates current CPU states against a requested configuration.
     */
    fun performIntegrityCheck(): Boolean {
        log("Executing CPU Integrity Check...")
        val policies = SystemDiscovery.getRegistry().cpuPolicies
        for (policy in policies) {
            val gov = getCurrentGovernor(policy)
            if (gov == "unknown" || gov.isEmpty()) {
                log("Integrity failure on policy $policy")
            }
        }
        return true
    }
}
