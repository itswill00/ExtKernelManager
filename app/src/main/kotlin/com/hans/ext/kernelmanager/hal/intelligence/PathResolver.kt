package com.hans.ext.kernelmanager.hal.intelligence

import com.hans.ext.kernelmanager.util.ShellUtils
import java.io.File

object PathResolver {
    private val cache = mutableMapOf<String, String>()

    fun resolveBatteryPath(): String {
        return cache.getOrPut("battery") {
            val base = "/sys/class/power_supply"
            val candidates = listOf("battery", "mtk-battery", "main", "usb", "ac")
            
            candidates.forEach { name ->
                val path = "$base/$name"
                if (File("$path/temp").exists() || File("$path/capacity").exists()) {
                    return@getOrPut path
                }
            }
            "/sys/class/power_supply/battery" // Fallback
        }
    }

    fun resolveThermalZone(typePrefix: String): String {
        val key = "thermal_$typePrefix"
        return cache.getOrPut(key) {
            val base = "/sys/class/thermal"
            File(base).listFiles()?.filter { it.name.startsWith("thermal_zone") }?.forEach { zone ->
                val type = ShellUtils.read("${zone.absolutePath}/type").lowercase()
                if (type.contains(typePrefix.lowercase())) {
                    return@getOrPut zone.absolutePath
                }
            }
            "/sys/class/thermal/thermal_zone0" // Fallback
        }
    }

    fun resolveCpuPath(policy: Int): String {
        return "/sys/devices/system/cpu/cpufreq/policy$policy"
    }

    fun clearCache() {
        cache.clear()
    }
}
