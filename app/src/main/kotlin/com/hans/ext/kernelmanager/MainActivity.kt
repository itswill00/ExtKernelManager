package com.hans.ext.kernelmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.ExperimentalFoundationApi
import android.content.Context
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.hans.ext.kernelmanager.hal.intelligence.SystemDiscovery
import com.hans.ext.kernelmanager.ui.screens.cpu.CpuScreen
import com.hans.ext.kernelmanager.ui.screens.dashboard.DashboardScreen
import com.hans.ext.kernelmanager.ui.screens.gpu.GpuScreen
import com.hans.ext.kernelmanager.ui.screens.iomem.IoMemScreen
import com.hans.ext.kernelmanager.ui.screens.more.MoreScreen
import com.hans.ext.kernelmanager.ui.screens.onboarding.OnboardingScreen
import com.hans.ext.kernelmanager.ui.theme.ExtKernelManagerTheme
import com.topjohnwu.superuser.Shell
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var isRootAvailable by mutableStateOf<Boolean?>(null)

        Shell.getShell { shell ->
            isRootAvailable = shell.isRoot
            if (isRootAvailable == true) {
                lifecycleScope.launch(Dispatchers.IO) {
                    SystemDiscovery.discover()
                }
            }
        }

        val sharedPref    = getSharedPreferences("ext_prefs", Context.MODE_PRIVATE)
        var showOnboarding by mutableStateOf(!sharedPref.getBoolean("onboarding_finished", false))

        setContent {
            ExtKernelManagerTheme {
                when {
                    showOnboarding        -> OnboardingScreen {
                        sharedPref.edit().putBoolean("onboarding_finished", true).apply()
                        showOnboarding = false
                    }
                    isRootAvailable == false -> RootErrorScreen()
                    isRootAvailable == true  -> MainContent()
                    else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// ── Root Error ────────────────────────────────────────────────────────────

@Composable
fun RootErrorScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier              = Modifier
                .fillMaxSize()
                .padding(40.dp),
            verticalArrangement   = Arrangement.Center,
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            Text(
                text       = "!",
                style      = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text       = "Root access required",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text       = "To control CPU frequencies, GPU governors, and memory tuning, this app needs root. Please grant permissions in Magisk or KernelSU and relaunch.",
                style      = MaterialTheme.typography.bodyMedium,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign  = TextAlign.Center
            )
        }
    }
}

// ── Navigation ────────────────────────────────────────────────────────────

private data class NavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainContent() {
    val coroutineScope = rememberCoroutineScope()
    val navItems = listOf(
        NavItem("dashboard", "Home")      { Icon(Icons.Default.Home,     "Home") },
        NavItem("cpu",       "CPU")       { Icon(Icons.Default.Build,    "CPU") },
        NavItem("gpu",       "GPU")       { Icon(Icons.Default.Settings, "GPU") },
        NavItem("iomem",     "I/O & Mem") { Icon(Icons.Default.Refresh,  "I/O") },
        NavItem("more",      "More")      { Icon(Icons.Default.Menu,     "More") }
    )
    
    val pagerState = rememberPagerState(pageCount = { navItems.size })

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick  = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon  = { item.icon() },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = MaterialTheme.colorScheme.primary,
                            selectedTextColor   = MaterialTheme.colorScheme.primary,
                            indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state    = pagerState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { page ->
            when (page) {
                0 -> DashboardScreen()
                1 -> CpuScreen()
                2 -> GpuScreen()
                3 -> IoMemScreen()
                4 -> MoreScreen()
            }
        }
    }
}
