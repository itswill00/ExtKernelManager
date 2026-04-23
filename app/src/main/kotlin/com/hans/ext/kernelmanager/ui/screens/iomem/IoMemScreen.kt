package com.hans.ext.kernelmanager.ui.screens.iomem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hans.ext.kernelmanager.ui.components.AppTopBar

private val cardRadius = RoundedCornerShape(24.dp)
private val innerRadius = RoundedCornerShape(16.dp)

@Composable
fun IoMemScreen(viewModel: IoMemViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar         = { AppTopBar(title = "I/O & Memory", onRefresh = { viewModel.refresh() }) },
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
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Storage ──────────────────────────────────────────────────────
            item {
                SectionLabel("Storage performance")
                Spacer(Modifier.height(6.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(), shape = cardRadius,
                    colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DropdownField(
                            label    = "I/O priority scheduler",
                            selected = state.currentScheduler,
                            options  = state.availableSchedulers,
                            onSelect = { viewModel.setScheduler(it) }
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = borderVariant()
                        ) {
                            Text(
                                getSchedulerInfo(state.currentScheduler),
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        DropdownField(
                            label    = "Read-ahead buffer size",
                            selected = "${state.readAhead} KB",
                            options  = listOf("128 KB", "256 KB", "512 KB", "1024 KB", "2048 KB", "4096 KB"),
                            onSelect = { viewModel.setReadAhead(it.substringBefore(" ")) }
                        )
                    }
                }
            }

            // ── Memory Management ────────────────────────────────────────────
            item {
                SectionLabel("Memory settings")
                Spacer(Modifier.height(6.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(), shape = cardRadius,
                    colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        val swapInt = state.swappiness.toIntOrNull() ?: 60
                        TuningSlider(
                            label    = "Swap aggression (Swappiness)",
                            value    = swapInt.toFloat(),
                            range    = 0f..200f,
                            onChange = { viewModel.setSwappiness(it.toInt()) },
                            minLabel = "Favor RAM",
                            maxLabel = "Favor Swap"
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        val vfsInt = state.vfsCachePressure.toIntOrNull() ?: 100
                        TuningSlider(
                            label    = "Filesystem cache pressure",
                            value    = vfsInt.toFloat(),
                            range    = 1f..500f,
                            onChange = { viewModel.setVfsCachePressure(it.toInt()) },
                            minLabel = "Keep cache",
                            maxLabel = "Purge cache"
                        )
                    }
                }
            }

            // ── Low Memory Killer ────────────────────────────────────────────
            item {
                SectionLabel("App management")
                Spacer(Modifier.height(6.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(), shape = cardRadius,
                    colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Low Memory Killer profile", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        Text("Determines when the system should terminate background apps to free up resources.",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LmkChip("Light", Modifier.weight(1f).fillMaxHeight()) { viewModel.applyLmkProfile("light") }
                            LmkChip("Standard", Modifier.weight(1f).fillMaxHeight()) { viewModel.applyLmkProfile("balanced") }
                            LmkChip("Strict", Modifier.weight(1f).fillMaxHeight()) { viewModel.applyLmkProfile("aggressive") }
                        }
                        if (state.lmkMinfree.isNotEmpty() && state.lmkMinfree != "N/A") {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Text("Current termination thresholds", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                state.lmkMinfree.split(",").joinToString("  ·  ") {
                                    it.trim().toLongOrNull()?.let { pages -> "${pages / 256} MB" } ?: it.trim()
                                },
                                style      = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Black,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // ── ZRAM ─────────────────────────────────────────────────────────
            item {
                SectionLabel("Virtual memory")
                Spacer(Modifier.height(6.dp))
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(), shape = cardRadius,
                    colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("ZRAM Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text(if (state.isZramEnabled) "Active · Virtual size: ${state.zramSize}" else "Inactive",
                                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp), 
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = borderVariant()
                            ) {
                                Text(if (state.isZramEnabled) "Enabled" else "Disabled",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        if (state.isZramEnabled) {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            DropdownField(
                                label    = "Compression algorithm",
                                selected = state.zramAlgo,
                                options  = listOf("lzo", "lz4", "zstd", "lzo-rle"),
                                onSelect = { viewModel.setZramAlgo(it) }
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
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
                shape    = innerRadius,
                color    = MaterialTheme.colorScheme.surfaceVariant,
                border   = borderVariant(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selected.ifEmpty { "—" }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { opt ->
                    DropdownMenuItem(text = { Text(opt, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium) },
                        onClick = { onSelect(opt); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun TuningSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>,
                          onChange: (Float) -> Unit, minLabel: String, maxLabel: String) {
    var sliderVal by remember(value) { mutableFloatStateOf(value) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(sliderVal.toInt().toString(), style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = sliderVal, onValueChange = { sliderVal = it },
            onValueChangeFinished = { onChange(sliderVal) }, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(minLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(maxLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LmkChip(label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = innerRadius,
        color    = MaterialTheme.colorScheme.surfaceVariant,
        border   = borderVariant()
    ) {
        Box(modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun borderVariant() = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

private fun getSchedulerInfo(scheduler: String): String = when (scheduler.lowercase()) {
    "mq-deadline" -> "Reliable flash storage scheduling for modern UFS devices."
    "bfq"         -> "Prioritizes system responsiveness and eliminates UI stutter."
    "kyber"       -> "Optimized for extreme throughput on NVMe/UFS storage."
    "none"        -> "Direct pass-through with minimal kernel overhead."
    "cfq"         -> "Balanced scheduling for general-purpose workloads."
    else          -> "General storage scheduler."
}
