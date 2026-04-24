package com.hans.ext.kernelmanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * AppDrawer (V1) - Premium Sidebar Navigation.
 * Provides a flexible hub for advanced system tools and configuration.
 */
@Composable
fun AppDrawer(
    onAction: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerTonalElevation = 2.dp,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(24.dp)
        ) {
            // Header: Brand Identity
            Text(
                "Ext Kernel",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Advanced Orchestrator",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Tools Section
            Text("Tools & Diagnostics", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
            
            DrawerItem("System Logs", Icons.Default.List, "logs", onAction)
            DrawerItem("Terminal Shell", Icons.Default.Edit, "terminal", onAction)
            DrawerItem("App Settings", Icons.Default.Settings, "settings", onAction)
            DrawerItem("About & License", Icons.Default.Info, "license", onAction)
            
            Spacer(Modifier.weight(1f))
            
            // Power Actions Section
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            Text("Power Management", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                
            DrawerItem("Reboot Recovery", Icons.Default.Refresh, "recovery", onAction, isError = true)
            DrawerItem("Reboot System", Icons.Default.Refresh, "reboot", onAction, isError = true)
            DrawerItem("Shut Down", Icons.Default.Close, "shutdown", onAction, isError = true)
            
            Spacer(Modifier.height(16.dp))
            Text(
                "v2.1.5-sovereign",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    id: String,
    onAction: (String) -> Unit,
    isError: Boolean = false
) {
    NavigationDrawerItem(
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        selected = false,
        onClick = { onAction(id) },
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        colors = NavigationDrawerItemDefaults.colors(
            unselectedTextColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
