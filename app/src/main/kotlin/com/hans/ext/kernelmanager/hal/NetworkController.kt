package com.hans.ext.kernelmanager.hal

import com.hans.ext.kernelmanager.util.SmartShell
import android.util.Log

object NetworkController {
    private const val TAG = "NetworkController"

    /**
     * Diagnostic Logger
     */
    private fun log(message: String) {
        Log.d(TAG, message)
    }

    /**
     * Lists all available TCP Congestion Control algorithms compiled in the kernel.
     */
    fun getAvailableTcpAlgorithms(): List<String> {
        val raw = SmartShell.read("/proc/sys/net/ipv4/tcp_available_congestion_control").trim()
        if (raw.isNotEmpty()) {
            return raw.split(" ").filter { it.isNotEmpty() }.sorted()
        }
        // Fallback common algorithms
        return listOf("cubic", "reno", "bbr", "westwood").sorted()
    }

    /**
     * Identifies the current TCP Congestion Control algorithm.
     */
    fun getCurrentTcpAlgorithm(): String {
        return SmartShell.read("/proc/sys/net/ipv4/tcp_congestion_control").trim().ifEmpty { "N/A" }
    }

    /**
     * Sets the TCP Congestion Control algorithm.
     */
    fun setTcpAlgorithm(algorithm: String): Boolean {
        log("Setting TCP Congestion Control to: $algorithm")
        return SmartShell.write("/proc/sys/net/ipv4/tcp_congestion_control", algorithm)
    }

    /**
     * Checks if Explicit Congestion Notification (ECN) is enabled.
     * 0 = disabled, 1 = enabled, 2 = enabled when requested by incoming packets.
     */
    fun getEcnState(): String {
        return SmartShell.read("/proc/sys/net/ipv4/tcp_ecn").trim().ifEmpty { "0" }
    }

    /**
     * Sets ECN state.
     */
    fun setEcnState(state: String): Boolean {
        log("Setting TCP ECN to: $state")
        return SmartShell.write("/proc/sys/net/ipv4/tcp_ecn", state)
    }
}
