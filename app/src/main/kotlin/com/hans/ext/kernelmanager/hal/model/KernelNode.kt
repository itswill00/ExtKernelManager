package com.hans.ext.kernelmanager.hal.model

enum class NodeType {
    TOGGLE,   // 0 or 1
    RANGE,    // Min to Max
    LIST,     // One of available_options
    INFO      // Read-only
}

data class KernelNode(
    val id: String,           // Unique identifier (e.g. "cpu0_gov")
    val name: String,         // Display name
    val path: String,         // Discovered sysfs path
    val type: NodeType,
    val optionsPath: String? = null, // For LIST type
    val category: String,     // CPU, GPU, Battery, etc.
    val min: Long = 0,        // For RANGE
    val max: Long = 0         // For RANGE
)
