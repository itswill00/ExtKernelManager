package com.hans.ext.kernelmanager.hal

import android.os.Build

enum class Chipset {
    SNAPDRAGON,
    MEDIATEK,
    UNKNOWN
}

object DeviceManager {
    val chipset: Chipset by lazy {
        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        when {
            hardware.contains("qcom") || board.contains("msm") || board.contains("sdm") || board.contains("taro") -> Chipset.SNAPDRAGON
            hardware.contains("mt") || board.contains("mt") -> Chipset.MEDIATEK
            else -> Chipset.UNKNOWN
        }
    }

    // Dynamic CPU Policy Detection
    fun getCpuPolicies(): List<Int> {
        val path = "/sys/devices/system/cpu/cpufreq"
        return com.hans.ext.kernelmanager.util.ShellUtils.getFiles(path)
            .filter { it.startsWith("policy") }
            .map { it.removePrefix("policy").toInt() }
            .sorted()
    }

    // Dynamic GPU Path Discovery
    private var cachedGpuPath: String? = null
    fun getGpuPath(): String {
        if (cachedGpuPath != null) return cachedGpuPath!!
        
        val searchRoots = listOf(
            "/sys/class/kgsl/kgsl-3d0",
            "/sys/class/devfreq",
            "/sys/devices/platform",
            "/sys/kernel/gpu"
        )
        
        for (root in searchRoots) {
            val found = findInDirectory(root, listOf("cur_freq", "scaling_cur_freq", "gpu_busy"))
            if (found != null) {
                cachedGpuPath = found.substringBeforeLast("/")
                return cachedGpuPath!!
            }
        }
        return "/sys/class/devfreq"
    }

    // Dynamic I/O Scheduler Discovery
    fun getMainBlockDevice(): String {
        val blocks = com.hans.ext.kernelmanager.util.ShellUtils.getFiles("/sys/block")
        for (block in blocks) {
            if (block.startsWith("loop") || block.startsWith("ram") || block.startsWith("zram")) continue
            val path = "/sys/block/$block/queue"
            if (fileExists("$path/scheduler")) return path
        }
        return ""
    }

    private fun findInDirectory(root: String, targets: List<String>): String? {
        val result = com.hans.ext.kernelmanager.util.ShellUtils.read("find $root -maxdepth 4 \\( -name ${targets.joinToString(" -o -name ")} \\) | head -n 1")
        return if (result.isNotEmpty()) result else null
    }

    private fun fileExists(path: String): Boolean {
        return com.hans.ext.kernelmanager.util.ShellUtils.exists(path)
    }
}
