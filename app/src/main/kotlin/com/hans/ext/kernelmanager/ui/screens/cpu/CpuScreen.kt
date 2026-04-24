package com.hans.ext.kernelmanager.ui.screens.cpu

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hans.ext.kernelmanager.ui.components.AppTopBar

private val cardRadius = RoundedCornerShape(24.dp)
private val innerRadius = RoundedCornerShape(16.dp)

@Composable
fun CpuScreen(
    viewModel: CpuViewModel = viewModel(),
    onMoreClick: () -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar         = { AppTopBar(title = "Processor Tuning", onMoreClick = onMoreClick) },
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
            // 1. Core Monitor
            item {
                SectionLabel("Realtime core load")
                Spacer(Modifier.height(6.dp))
                CoreMonitorCard(state.coreFrequencies)
            }

            // 2. Clusters
            item {
                SectionLabel("Hardware clusters")
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    state.policies.forEach { p ->
                        ClusterCard(
                            policy        = p,
                            totalPolicies = state.policies.size,
                            coreMap       = state.cpuCoreMap[p] ?: "",
                            governor      = state.currentGov[p] ?: "—",
                            governors     = state.governors[p] ?: emptyList(),
                            minFreq       = state.minFreq[p] ?: "—",
                            maxFreq       = state.maxFreq[p] ?: "—",
                            freqOptions   = state.availableFreqs[p] ?: emptyList(),
                            onSetGovernor = { viewModel.setGovernor(p, it) },
                            onSetMin      = { viewModel.setMinFreq(p, it) },
                            onSetMax      = { viewModel.setMaxFreq(p, it) }
                        )
                    }
                }
            }

            // 3. Maintenance
            item {
                SectionLabel("System tools")
                Spacer(Modifier.height(6.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(), shape = cardRadius,
                    colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ActionChip(label = "Audit System Integrity", modifier = Modifier.fillMaxWidth()) {
                            // Logic placeholder
                        }
                        ActionChip(label = "Restore Factory Defaults", modifier = Modifier.fillMaxWidth()) {
                            for (p in state.policies) {
                                viewModel.setGovernor(p, "schedutil")
                                val freqs = state.availableFreqs[p] ?: continue
                                if (freqs.isNotEmpty()) {
                                    viewModel.setMinFreq(p, freqs.last())
                                    viewModel.setMaxFreq(p, freqs.first())
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

// ── Cluster Card ──────────────────────────────────────────────────────────────

@Composable
private fun ClusterCard(
    policy        : Int,
    totalPolicies : Int,
    coreMap       : String,
    governor      : String,
    governors     : List<String>,
    minFreq       : String,
    maxFreq       : String,
    freqOptions   : List<String>,
    onSetGovernor : (String) -> Unit,
    onSetMin      : (String) -> Unit,
    onSetMax      : (String) -> Unit
) {
    val name = when {
        totalPolicies <= 1 -> "Unified Processor"
        totalPolicies == 2 -> if (policy == 0) "Efficiency cores" else "Performance cores"
        else               -> when (policy) { 0 -> "Efficiency cores"; 1 -> "Performance cores"; else -> "Prime core" }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(), shape = cardRadius,
        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (coreMap.isNotEmpty())
                        Text("Cores: $coreMap", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(12.dp), 
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = borderVariant()
                ) {
                    Text(governor, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            DropdownField(label = "Performance profile", selected = governor, options = governors, onSelect = onSetGovernor)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) { DropdownField("Minimum speed", minFreq, freqOptions.reversed(), onSetMin) }
                Column(Modifier.weight(1f)) { DropdownField("Maximum speed", maxFreq, freqOptions, onSetMax) }
            }
        }
    }
}

// ── Core Monitor ──────────────────────────────────────────────────────────────

@Composable
private fun CoreMonitorCard(freqs: Map<Int, String>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(), shape = cardRadius,
        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            freqs.keys.sorted().chunked(4).forEach { chunk ->
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    chunk.forEach { core -> CoreItem(core, freqs[core] ?: "—", Modifier.weight(1f).fillMaxHeight()) }
                    repeat(4 - chunk.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CoreItem(id: Int, freq: String, modifier: Modifier = Modifier) {
    val isOff = freq == "OFF"
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = if (isOff) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
        border   = borderVariant()
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CPU $id", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(freq.replace(" MHz", ""), 
                style = MaterialTheme.typography.bodySmall, 
                fontWeight = FontWeight.Black,
                color = if (isOff) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
        }
    }
}

// ── Reusable Components ───────────────────────────────────────────────────────

@Composable
private fun DropdownField(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            Surface(
                onClick  = { if (options.isNotEmpty()) expanded = true },
                shape    = innerRadius,
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
private fun ActionChip(label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = innerRadius,
        color    = MaterialTheme.colorScheme.surfaceVariant,
        border   = borderVariant()
    ) {
        Text(label, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.Black),
        color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 4.dp))
}

@Composable
private fun borderVariant() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
