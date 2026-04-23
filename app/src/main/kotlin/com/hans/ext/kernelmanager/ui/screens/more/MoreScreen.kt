package com.hans.ext.kernelmanager.ui.screens.more

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hans.ext.kernelmanager.ui.components.AppTopBar

private val cardShape = RoundedCornerShape(16.dp)

@Composable
fun MoreScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val registry = com.hans.ext.kernelmanager.hal.intelligence.SystemDiscovery.getRegistry()
    var showLicense by remember { mutableStateOf(false) }

    if (showLicense) {
        LicenseScreen(onBack = { showLicense = false })
        return
    }

    Scaffold(
        topBar = { AppTopBar(title = "More") },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── About App ──────────────────────────────────────────────────
            item {
                AboutCard()
            }

            // ── Links ──────────────────────────────────────────────────────
            item {
                MoreGroupCard(title = "Resources") {
                    MoreRow(
                        label   = "Open Source Licenses",
                        caption = "Apache 2.0 · MIT · libsu",
                        onClick = { showLicense = true }
                    )
                    MoreDivider()
                    MoreRow(
                        label   = "View on GitHub",
                        caption = "github.com/hans-ext/kernel-manager",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/hans-ext/kernel-manager"))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            // ── Device Information ──────────────────────────────────────────
            item {
                MoreGroupCard(title = "Hardware & Platform") {
                    InfoRow(label = "Device",       value = registry.marketName)
                    MoreDivider()
                    InfoRow(label = "Architecture", value = registry.heritage.arch)
                    MoreDivider()
                    InfoRow(label = "Processor",    value = registry.heritage.model)
                    MoreDivider()
                    InfoRow(label = "Platform",     value = registry.socModel)
                    MoreDivider()
                    InfoRow(label = "Android",      value = "Android ${android.os.Build.VERSION.RELEASE}")
                }
            }

            // ── Kernel Information ──────────────────────────────────────────
            item {
                MoreGroupCard(title = "Kernel information") {
                    val meta = registry.kernelMetadata
                    // Use substringBefore for clean version if needed, but registry usually has it clean
                    InfoRow(label = "Version",      value = meta["version"] ?: "Unknown")
                    MoreDivider()
                    InfoRow(label = "Compiler",     value = meta["compiler"] ?: "N/A")
                    MoreDivider()
                    InfoRow(label = "Build Target", value = meta["build"] ?: "N/A")
                }
            }

            // ── Advanced System ─────────────────────────────────────────────
            item {
                MoreGroupCard(title = "System diagnostics") {
                    InfoRow(label = "Build Number", value = registry.buildNumber)
                    MoreDivider()
                    InfoRow(label = "SELinux",      value = registry.selinuxStatus)
                    MoreDivider()
                    InfoRow(label = "Baseband",     value = registry.basebandVersion)
                    MoreDivider()
                    InfoRow(label = "Bootloader",   value = registry.bootloaderVersion)
                }
            }

            // ── App Info ───────────────────────────────────────────────────
            item {
                MoreGroupCard(title = "App Info") {
                    InfoRow(label = "Version",      value = "1.0.0")
                    MoreDivider()
                    InfoRow(label = "Package",      value = "com.hans.ext.kernelmanager")
                    MoreDivider()
                    InfoRow(label = "Min Android",  value = "Android 11 (API 30)")
                    MoreDivider()
                    InfoRow(label = "Root Method",  value = "Magisk / KernelSU")
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ── About Card ────────────────────────────────────────────────────────────

@Composable
private fun AboutCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = cardShape,
        color    = MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(
            modifier                = Modifier.padding(24.dp),
            horizontalAlignment     = Alignment.CenterHorizontally,
            verticalArrangement     = Arrangement.spacedBy(10.dp)
        ) {
            // App monogram
            Box(
                modifier            = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment    = Alignment.Center
            ) {
                Text(
                    text       = "EK",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text       = "Ext Kernel Manager",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text       = "Deep hardware control, done right.",
                style      = MaterialTheme.typography.bodyMedium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text       = "Version 1.0.0",
                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ── Group Card ────────────────────────────────────────────────────────────

@Composable
private fun MoreGroupCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text     = title,
            style    = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape    = cardShape,
            color    = MaterialTheme.colorScheme.surface,
            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun MoreRow(label: String, caption: String, onClick: () -> Unit) {
    Surface(
        onClick    = onClick,
        color      = MaterialTheme.colorScheme.surface,
        modifier   = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier                = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalArrangement   = Arrangement.SpaceBetween,
            verticalAlignment       = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label,   style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(caption, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun MoreDivider() {
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
}

// ── License Screen ────────────────────────────────────────────────────────

@Composable
private fun LicenseScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Licenses", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Back", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier        = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding  = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { LicenseCard(name = "Ext Kernel Manager", spdx = "Apache-2.0", body = APACHE_LICENSE_TEXT) }
            item { LicenseCard(name = "libsu by topjohnwu", spdx = "Apache-2.0", body = LIBSU_NOTICE) }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun LicenseCard(name: String, spdx: String, body: String) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape    = cardShape,
        color    = MaterialTheme.colorScheme.surface,
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(spdx, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Collapse" else "View", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (expanded) {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Text(
                    text  = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── License text constants ────────────────────────────────────────────────

private const val APACHE_LICENSE_TEXT = """
Apache License
Version 2.0, January 2004
http://www.apache.org/licenses/

TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

1. Definitions. "License" shall mean the terms and conditions for use, reproduction, and distribution. "Licensor" shall mean the copyright owner or entity authorized by the copyright owner. "Legal Entity" shall mean the union of the acting entity and all other entities that control, are controlled by, or are under common control with that entity.

2. Grant of Copyright License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, sublicense, and distribute the Work and such Derivative Works in Source or Object form.

3. Grant of Patent License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable patent license to make, have made, use, offer to sell, sell, import, and otherwise transfer the Work.

4. Redistribution. You may reproduce and distribute copies of the Work or Derivative Works thereof in any medium, with or without modifications, subject to the conditions in Section 4(a)-(d) of the Apache 2.0 license.

For the full license text see: https://www.apache.org/licenses/LICENSE-2.0
"""

private const val LIBSU_NOTICE = """
libsu — https://github.com/topjohnwu/libsu

Copyright 2023 John Wu

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this library except in compliance with the License. You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
"""
