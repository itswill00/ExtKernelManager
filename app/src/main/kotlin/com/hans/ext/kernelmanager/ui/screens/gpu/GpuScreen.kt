package com.hans.ext.kernelmanager.ui.screens.gpu

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Brush
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hans.ext.kernelmanager.ui.components.AppTopBar

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val cardRadius = RoundedCornerShape(24.dp)

@Composable
fun GpuScreen(
    viewModel: GpuViewModel = viewModel(),
    onMoreClick: () -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    val state by viewModel.state.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startTelemetry()
        onDispose {
            viewModel.stopTelemetry()
        }
    }

    Scaffold(
        topBar         = { AppTopBar(title = "GPU Control", onMoreClick = onMoreClick) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. GPU Hero
            item { GpuHero(state) }

            // 2. Scaling Control
            item {
                SectionLabel("Scaling settings")
                Spacer(Modifier.height(6.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(), shape = cardRadius,
                    colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DropdownField(label = "GPU Governor", selected = state.currentGovernor,
                            options = state.availableGovernors, onSelect = { viewModel.setGovernor(it) })
                            
                        if (state.availablePowerPolicies.isNotEmpty()) {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            DropdownField(label = "Power Policy", selected = state.currentPowerPolicy,
                                options = state.availablePowerPolicies, onSelect = { viewModel.setPowerPolicy(it) })
                        }
                        
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f)) {
                                DropdownField("Min frequency", state.minFreq, state.availableFrequencies.reversed()) { viewModel.setMinFreq(it) }
                            }
                            Column(Modifier.weight(1f)) {
                                DropdownField("Max frequency", state.maxFreq, state.availableFrequencies) { viewModel.setMaxFreq(it) }
                            }
                        }
                    }
                }
            }

            // 3. Technical Diagnostics
            item {
                SectionLabel("Technical Diagnostics")
                Spacer(Modifier.height(6.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(), shape = cardRadius,
                    colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SpecRow("Renderer", state.gpuRenderer)
                        SpecRow("Bus Speed", state.gpuBusSpeed)
                        SpecRow("GPU Load", "${state.gpuLoad}%")
                        SpecRow("Temperature", state.gpuTemp)
                        SpecRow("Memory Alloc", state.gpuMemoryUsage)
                        SpecRow("Available Freqs", "${state.availableFrequencies.size} steps")
                    }
                }
            }

            // 4. Qualcomm Adreno
            if (state.adrenoBoostAvailable || state.adrenoIdlerAvailable) {
                item {
                    SectionLabel("Graphics tweaks")
                    Spacer(Modifier.height(6.dp))
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(), shape = cardRadius,
                        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (state.adrenoBoostAvailable) {
                                val levels = listOf("0 — Off", "1 — Low", "2 — Medium", "3 — Max")
                                var selected by remember { mutableStateOf(levels[0]) }
                                DropdownField("Adreno Boost Level", selected, levels) {
                                    selected = it
                                    viewModel.setAdrenoBoost(it.substringBefore(" ").toIntOrNull() ?: 0)
                                }
                            }
                            if (state.adrenoBoostAvailable && state.adrenoIdlerAvailable) {
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                            if (state.adrenoIdlerAvailable) {
                                var idlerEnabled by remember { mutableStateOf(false) }
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Adreno Idler", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface)
                                        Text("Reduces frequency during inactivity", style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(checked = idlerEnabled, onCheckedChange = { idlerEnabled = it; viewModel.setAdrenoIdler(it) })
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.Black),
        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun DropdownField(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            Surface(
                onClick  = { if (options.isNotEmpty()) expanded = true },
                shape    = RoundedCornerShape(16.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant,
                border   = borderVariant(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selected.ifEmpty { "—" }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(
                expanded = expanded, 
                onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) },
                        onClick = { onSelect(opt); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        border   = borderVariant()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun GpuHero(state: GpuState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = cardRadius,
        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Modern Gradient Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text("Graphics Engine", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary.copy(0.8f))
                    Text(state.gpuRenderer, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            // Hero Vitals Strip
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Clock Speed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.currentFrequency, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("GPU Load", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${state.gpuLoad}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun borderVariant() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
