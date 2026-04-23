package com.hans.ext.kernelmanager.hal

import com.hans.ext.kernelmanager.hal.model.KernelNode
import com.hans.ext.kernelmanager.hal.model.NodeType

object RegistryScanner {
    
    fun scanAll(): List<KernelNode> {
        val nodes = mutableListOf<KernelNode>()
        
        // 1. Scan CPU Policies
        DeviceManager.getCpuPolicies().forEach { policy ->
            nodes.add(KernelNode(
                id = "cpu_gov_$policy",
                name = "Governor (Policy $policy)",
                path = "/sys/devices/system/cpu/cpufreq/policy$policy/scaling_governor",
                type = NodeType.LIST,
                optionsPath = "/sys/devices/system/cpu/cpufreq/policy$policy/scaling_available_governors",
                category = "CPU"
            ))
            // Max Freq, Min Freq, etc.
        }

        // 2. Scan GPU
        val gpuPath = DeviceManager.getGpuPath()
        nodes.add(KernelNode(
            id = "gpu_gov",
            name = "GPU Governor",
            path = "$gpuPath/governor",
            type = NodeType.LIST,
            optionsPath = "$gpuPath/available_governors",
            category = "GPU"
        ))

        // 3. Scan I/O
        val blockPath = DeviceManager.getMainBlockDevice()
        if (blockPath.isNotEmpty()) {
            nodes.add(KernelNode(
                id = "io_sched",
                name = "I/O Scheduler",
                path = "$blockPath/scheduler",
                type = NodeType.LIST,
                optionsPath = "$blockPath/scheduler",
                category = "I/O"
            ))
        }

        return nodes
    }
}
