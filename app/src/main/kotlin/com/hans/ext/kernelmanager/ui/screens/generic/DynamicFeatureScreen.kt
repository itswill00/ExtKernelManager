package com.hans.ext.kernelmanager.ui.screens.generic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hans.ext.kernelmanager.hal.UniversalController
import com.hans.ext.kernelmanager.hal.model.KernelNode
import com.hans.ext.kernelmanager.hal.model.NodeType
import com.hans.ext.kernelmanager.ui.components.ExtCard

@Composable
fun DynamicFeatureScreen(category: String, nodes: List<KernelNode>) {
    val categoryNodes = nodes.filter { it.category == category }
    
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(categoryNodes) { node ->
            FeatureItem(node)
        }
    }
}

@Composable
fun FeatureItem(node: KernelNode) {
    var currentValue by remember { mutableStateOf(UniversalController.read(node)) }

    ExtCard(title = node.name) {
        when (node.type) {
            NodeType.LIST -> {
                val options = UniversalController.getOptions(node)
                options.forEach { option ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = currentValue == option,
                            onClick = {
                                currentValue = option
                                UniversalController.write(node, option)
                            }
                        )
                        Text(option)
                    }
                }
            }
            NodeType.TOGGLE -> {
                Switch(
                    checked = currentValue == "1",
                    onCheckedChange = {
                        val newValue = if (it) "1" else "0"
                        currentValue = newValue
                        UniversalController.write(node, newValue)
                    }
                )
            }
            else -> {
                Text("Current: $currentValue")
            }
        }
    }
}
