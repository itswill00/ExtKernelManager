package com.hans.ext.kernelmanager.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

private val cardRadius = RoundedCornerShape(24.dp)
private val innerRadius = RoundedCornerShape(16.dp)

@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar         = { AppTopBar(title = "Overview", onRefresh = { viewModel.startMonitoring() }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Identity Hero
            item { DeviceHero(state) }

            // 2. Vitals Strip
            item {
                Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    VitalCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label    = "Power",
                        value    = "${state.batteryLevel}%",
                        sub      = if (state.isCharging) "Charging" else state.batteryTemp,
                        highlight = state.batteryLevel <= 20
                    )
                    VitalCard(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        label    = "System",
                        value    = state.uptime,
                        sub      = "Deep sleep: ${state.deepSleep}",
                        highlight = false
                    )
                }
            }

            // 3. Pulse (System Activity)
            item {
                SectionLabel("System activity")
                Spacer(Modifier.height(6.dp))
                PulseCard(state)
            }

            // 4. Resources (Bento Style)
            item {
                SectionLabel("Resource overview")
                Spacer(Modifier.height(6.dp))
                ResourceBento(state)
            }

            // 5. System Details
            item {
                SectionLabel("About system")
                Spacer(Modifier.height(6.dp))
                SystemDetails(state)
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun DeviceHero(state: DashboardState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = cardRadius,
        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier              = Modifier.padding(20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier         = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    state.deviceName.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(state.deviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text("${state.chipset} · ${state.phoneModel} · arm64-v8a", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(state.androidVersion, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
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
private fun PulseCard(state: DashboardState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = cardRadius,
        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Main Performance Row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Total Load", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("${state.cpuLoad}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text(state.cpuTemp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text("Graphics", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(state.gpuFreq, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text(state.gpuTemp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // Dynamic Core Grid
            val policies = state.cpuFrequencies.keys.sorted()
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
                                freq     = state.cpuFrequencies[p] ?: "—"
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
            Text(freq.replace(" MHz", ""), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ResourceBento(state: DashboardState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoBox(Modifier.weight(1f).fillMaxHeight(), "Memory", state.ramDetail, state.ramUsage)
            BentoBox(Modifier.weight(1f).fillMaxHeight(), "Apps", state.appCount, 0f, showProgress = false)
        }
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoBox(Modifier.weight(1f).fillMaxHeight(), "Display", state.displayStats, 0f, showProgress = false)
            BentoBox(Modifier.weight(1f).fillMaxHeight(), "Storage", state.storageDetail.ifEmpty { "Scanning..." }, state.storageUsage)
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
private fun SystemDetails(state: DashboardState) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = cardRadius,
        colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            SpecRow("Kernel", state.kernelVersion)
            SpecRow("Build", state.buildNumber)
            SpecRow("Uptime", state.uptimeFull)
            SpecRow("Deep sleep", state.deepSleepFull)
            SpecRow("SELinux", state.selinuxStatus)
            SpecRow("Baseband", state.baseband)
            SpecRow("Bootloader", state.bootloader)
            SpecRow("Security patch", state.securityPatch)
            SpecRow("I/O scheduler", state.ioScheduler)
            if (state.zramEnabled) SpecRow("ZRAM", state.zramSize)
        }
    }
}

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
            modifier = Modifier.weight(1f),
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
