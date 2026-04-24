package com.hans.ext.kernelmanager.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hans.ext.kernelmanager.ui.components.AppTopBar

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val cardRadius = RoundedCornerShape(24.dp)
private val innerRadius = RoundedCornerShape(16.dp)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    onMoreClick: () -> Unit = {},
    bottomPadding: Dp = 0.dp
) {
    val state by viewModel.state.collectAsState()
    var showBatteryDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        viewModel.startMonitoring()
        onDispose {
            viewModel.stopMonitoring()
        }
    }

    if (showBatteryDialog) {
        BatteryDetailDialog(state) { showBatteryDialog = false }
    }

    if (state.showDisplayDialog) {
        DisplayDetailDialog(state.displayDetail) { viewModel.hideDisplayDetail() }
    }

    Scaffold(
        topBar         = { AppTopBar(title = "Overview", onMoreClick = onMoreClick) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Identity Hero
            item { 
                DeviceHero(
                    deviceName = state.deviceName,
                    chipset = state.chipset,
                    phoneModel = state.phoneModel,
                    androidVersion = state.androidVersion
                ) 
            }

            // 2. Vitals Strip
            item {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VitalCard(
                        modifier = Modifier.weight(1f).fillMaxHeight().clickable { showBatteryDialog = true },
                        label    = "Battery",
                        value    = state.batteryCurrent,
                        sub      = "${state.batteryLevel}% · ${state.batteryTemp}",
                        highlight = state.batteryLevel <= 20
                    )
                    val memFree = state.ramDetail.split(" of ").firstOrNull()?.replace(" MB", "")?.toIntOrNull()
                    val totalMem = state.ramDetail.split(" of ").getOrNull(1)?.replace(" MB used", "")?.toIntOrNull()
                    val freeGb = if (memFree != null && totalMem != null) String.format("%.1f GB", (totalMem - memFree) / 1024f) else "N/A"
                    VitalCard(
                        modifier = Modifier.weight(1f),
                        label    = "Free Memory",
                        value    = freeGb,
                        sub      = "Available RAM",
                        highlight = false
                    )
                }
            }

            // 3. Pulse (System Activity)
            item {
                SectionLabel("System")
                Spacer(Modifier.height(6.dp))
                PulseCard(
                    cpuLoad = state.cpuLoad,
                    cpuTemp = state.cpuTemp,
                    gpuFreq = state.gpuFreq,
                    gpuTemp = state.gpuTemp,
                    cpuFrequencies = state.cpuFrequencies
                )
            }

            // 4. Resources (Bento Style)
            item {
                SectionLabel("Resources")
                Spacer(Modifier.height(6.dp))
                ResourceBento(
                    ramDetail = state.ramDetail,
                    ramUsage = state.ramUsage,
                    appCount = state.appCount,
                    displayStats = state.displayStats,
                    storageDetail = state.storageDetail,
                    storageUsage = state.storageUsage,
                    onDisplayClick = { viewModel.showDisplayDetail() }
                )
            }

            // 5. System Details
            item {
                SectionLabel("About")
                Spacer(Modifier.height(6.dp))
                SystemDetails(
                    kernelVersion = state.kernelVersion,
                    buildNumber = state.buildNumber,
                    uptimeFull = state.uptimeFull,
                    deepSleepFull = state.deepSleepFull,
                    selinuxStatus = state.selinuxStatus,
                    baseband = state.baseband,
                    bootloader = state.bootloader,
                    securityPatch = state.securityPatch,
                    ioScheduler = state.ioScheduler,
                    zramEnabled = state.zramEnabled,
                    zramSize = state.zramSize
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun DeviceHero(deviceName: String, chipset: String, phoneModel: String, androidVersion: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = cardRadius,
        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(deviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("$chipset · $phoneModel", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(androidVersion, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun VitalCard(modifier: Modifier, label: String, value: String, sub: String, highlight: Boolean) {
    ElevatedCard(
        modifier = modifier,
        shape    = cardRadius,
        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value, 
                style = MaterialTheme.typography.headlineMedium, 
                fontWeight = FontWeight.Black,
                color = if (highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PulseCard(cpuLoad: Int, cpuTemp: String, gpuFreq: String, gpuTemp: String, cpuFrequencies: Map<Int, String>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = cardRadius,
        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val maxCpuFreqStr = cpuFrequencies.values
                .mapNotNull { it.replace(" MHz", "").toIntOrNull() }
                .maxOrNull()?.let { 
                    if (it >= 1000) String.format("%.1f GHz", it / 1000f) else "$it MHz"
                } ?: "N/A"

            // Main Performance Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("CPU Core", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(maxCpuFreqStr, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text(cpuTemp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text("Graphics", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(gpuFreq, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(gpuTemp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Dynamic Core Grid
            val policies = cpuFrequencies.keys.sorted()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                policies.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { p ->
                            CorePill(
                                modifier = Modifier.weight(1f),
                                label    = when {
                                    policies.size == 2 -> if (p == 0) "Little" else "Big"
                                    policies.size >= 3 -> when (p) { 0 -> "Little"; 1 -> "Big"; else -> "Prime" }
                                    else -> "Cluster $p"
                                },
                                freq     = (cpuFrequencies[p] ?: "—").let {
                                    val f = it.toIntOrNull()
                                    if (f != null) String.format("%.1f GHz", f / 1000f) else it
                                }
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CorePill(modifier: Modifier, label: String, freq: String) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        border   = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(freq, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ResourceBento(
    ramDetail: String, ramUsage: Float, appCount: String, 
    displayStats: String, storageDetail: String, storageUsage: Float,
    onDisplayClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoBox(Modifier.weight(1f).fillMaxHeight(), "Memory", ramDetail, ramUsage)
            BentoBox(Modifier.weight(1f).fillMaxHeight(), "Apps", appCount, 0f, showProgress = false)
        }
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoBox(Modifier.weight(1f).fillMaxHeight().clickable { onDisplayClick() }, "Display", displayStats, 0f, showProgress = false)
            BentoBox(Modifier.weight(1f).fillMaxHeight(), "Storage", storageDetail.ifEmpty { "Scanning..." }, storageUsage)
        }
    }
}

@Composable
private fun BentoBox(modifier: Modifier, label: String, value: String, progress: Float, showProgress: Boolean = true) {
    ElevatedCard(
        modifier = modifier,
        shape    = cardRadius,
        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (showProgress) {
                val animProg by animateFloatAsState(targetValue = progress, animationSpec = tween(800), label = "prog")
                LinearProgressIndicator(
                    progress   = { animProg },
                    modifier   = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color      = if (progress > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SystemDetails(
    kernelVersion: String, buildNumber: String, uptimeFull: String, deepSleepFull: String,
    selinuxStatus: String, baseband: String, bootloader: String, securityPatch: String,
    ioScheduler: String, zramEnabled: Boolean, zramSize: String
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = cardRadius,
        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            SpecRow("Kernel", kernelVersion)
            SpecRow("Build", buildNumber)
            SpecRow("Uptime", uptimeFull)
            SpecRow("Deep sleep", deepSleepFull)
            SpecRow("SELinux", selinuxStatus)
            SpecRow("Baseband", baseband)
            SpecRow("Bootloader", bootloader)
            SpecRow("Security patch", securityPatch)
            SpecRow("I/O scheduler", ioScheduler)
            if (zramEnabled) SpecRow("ZRAM", zramSize)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(iterations = Int.MAX_VALUE),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.8.sp, fontWeight = FontWeight.Black),
        color    = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun BatteryDetailDialog(state: DashboardState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Battery Diagnostics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Hardware Telemetry", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Visual Charge Bar
                val animLevel by animateFloatAsState(targetValue = state.batteryLevel / 100f, animationSpec = tween(1000), label = "bat")
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { animLevel },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(Modifier.height(4.dp))
                InfoRowDetail("Status", state.batteryStatus)
                InfoRowDetail("Current", state.batteryCurrent)
                InfoRowDetail("Voltage", state.batteryVoltage)
                InfoRowDetail("Level", "${state.batteryLevel}%")
                InfoRowDetail("Temperature", state.batteryTemp)
                InfoRowDetail("Health", state.batteryHealth)
                InfoRowDetail("Cycle Count", state.batteryCycleCount)
                InfoRowDetail("Technology", state.batteryTechnology)
            }
        },
        shape = cardRadius,
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun InfoRowDetail(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
@Composable
private fun DisplayDetailDialog(detail: DisplayDetail, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("D", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Text("Display Specifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Precision hardware data polled directly from system services and kernel logs.", 
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
                Surface(
                    shape = innerRadius,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SpecRow("Panel Info", detail.panel)
                        SpecRow("Resolution", detail.resolution)
                        SpecRow("Refresh Rate", detail.refreshRate)
                        SpecRow("Density", detail.density)
                        SpecRow("HDR Support", detail.hdr)
                    }
                }
            }
        },
        shape = cardRadius,
        containerColor = MaterialTheme.colorScheme.surface
    )
}
