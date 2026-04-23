package com.hans.ext.kernelmanager.hal

import com.hans.ext.kernelmanager.hal.model.KernelNode
import com.hans.ext.kernelmanager.hal.model.NodeType
import com.hans.ext.kernelmanager.util.ShellUtils

object UniversalController {
    
    fun read(node: KernelNode): String {
        return ShellUtils.read(node.path)
    }

    fun write(node: KernelNode, value: String): Boolean {
        return ShellUtils.write(node.path, value)
    }

    fun getOptions(node: KernelNode): List<String> {
        if (node.type != NodeType.LIST || node.optionsPath == null) return emptyList()
        val raw = ShellUtils.read(node.optionsPath)
        // Handle common formats: "noop deadline [cfq]" or "100 200 400"
        return raw.replace("[", "").replace("]", "").split(" ").filter { it.isNotEmpty() }
    }

    // Batched read for performance
    fun readBatch(nodes: List<KernelNode>): Map<String, String> {
        if (nodes.isEmpty()) return emptyMap()
        val cmd = nodes.joinToString(" && ") { "echo -n \"${it.id}:\" && cat ${it.path}" }
        val results = ShellUtils.read(cmd).split("\n")
        
        val map = mutableMapOf<String, String>()
        results.forEach { line ->
            val parts = line.split(":", limit = 2)
            if (parts.size == 2) {
                map[parts[0]] = parts[1].trim()
            }
        }
        return map
    }
}
