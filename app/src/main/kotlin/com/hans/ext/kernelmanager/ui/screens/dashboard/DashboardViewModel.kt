package com.hans.ext.kernelmanager.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hans.ext.kernelmanager.hal.*
import com.hans.ext.kernelmanager.hal.intelligence.*
import com.hans.ext.kernelmanager.hal.tuning.*
import com.hans.ext.kernelmanager.util.SmartShell
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

/**
 * DashboardState (V11) - Comprehensive system health and performance state.
 */
data class DashboardState(
    val cpuFrequencies: Map<Int, String> = emptyMap(),
    val cpuHistory: Map<Int, List<Float>> = emptyMap(),
    val batteryTemp: String = "0.0°C",
    val batteryCurrent: String = "0 mA",
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val ramUsage: Float = 0f,
    val ramDetail: String = "",
    val ramTotal: String = "",
    val storageUsage: Float = 0f,
    val storageDetail: String = "",
    val kernelVersion: String = "",
    val chipset: String = "",
    val socModel: String = "",
    val gpuModel: String = "",
    val gpuFreq: String = "0 MHz",
    val socSpecs: SocScanner.SocInfo? = null,
    val uptime: String = "",
    val deepSleep: String = "0%",
    val activeProfile: Profile = Profile.BALANCED,
    val zramEnabled: Boolean = false,
    val zramSize: String = "N/A",
    val thermalReport: String = "",
    val storageReport: String = "",
    val ioScheduler: String = "",
    val securityPatch: String = "",
    // Device identity
    val deviceName: String = "",
    val androidVersion: String = "",
    val phoneModel: String = "",
    // New Pulse metrics
    val cpuTemp: String = "0°C",
    val gpuTemp: String = "0°C",
    val cpuLoad: Int = 0,
    val appCount: String = "—",
    val displayStats: String = "—",
    val networkType: String = "—",
    // Advanced Meta
    val selinuxStatus: String = "",
    val buildNumber: String = "",
    val bootloader: String = "",
    val baseband: String = "",
    val uptimeFull: String = "",
    val deepSleepFull: String = ""
)

/**
 * DashboardViewModel (V11) - The Omnisovereign Telemetry Engine.
 * Responsible for high-frequency polling of hardware metrics and state synchronization.
 */
class DashboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state
    
    private val TAG = "OmnisovereignViewModel"

    init {
        log("Omnisovereign Dashboard Engine Initialized.")
        startMonitoring()
    }

    /**
     * Applies a performance profile via the Grand Orchestrator.
     */
    fun setProfile(profile: Profile) {
        viewModelScope.launch {
            log("User requested profile transition: $profile")
            val report = TuningOrchestrator.applyProfile(profile)
            if (report.success) {
                _state.value = _state.value.copy(activeProfile = profile)
            }
        }
    }

    /**
     * The main telemetry loop: Polls hardware metrics every 1000ms.
     * IMPORTANT: Waits for SystemDiscovery to complete before starting polling,
     * to prevent querying an empty registry and getting all-null data.
     */
    fun startMonitoring() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // Guard: If registry is empty (discovery not yet done), run it now.
            // This handles the race condition between MainActivity's async discover()
            // and ViewModel initialization.
            if (SystemDiscovery.getRegistry().cpuPolicies.isEmpty() &&
                SystemDiscovery.getRegistry().interfaces.isEmpty()) {
                log("Registry is empty — triggering SystemDiscovery before polling.")
                SystemDiscovery.discover()
                log("Discovery complete — starting telemetry loop.")
            }

            var slowTick = 0
            while (true) {
                try {
                    val registry = SystemDiscovery.getRegistry()
                    val heritage = registry.heritage

                    val freqs = mutableMapOf<Int, String>()
                    val newHistory = _state.value.cpuHistory.toMutableMap()

                    // 1. CPU Telemetry
                    val policies = registry.cpuPolicies
                    for (policy in policies) {
                        val freqStr = CpuController.getCurrentFrequency(policy)
                        freqs[policy] = freqStr

                        val freqValue = freqStr.replace(" MHz", "").replace(" GHz", "").toFloatOrNull() ?: 0f
                        val history = (_state.value.cpuHistory[policy] ?: emptyList()).takeLast(29) + freqValue
                        newHistory[policy] = history
                    }

                    // 2. Battery Telemetry
                    val batteryMap = registry.subsystems["BATTERY"] ?: emptyMap()
                    val temp = BatteryController.getTemperature()
                    val current = BatteryController.getCurrentNow()

                    // 3. Memory Telemetry — readLines() dari /proc/meminfo lalu parse per field
                    val memLines = SmartShell.readLines("/proc/meminfo")
                    val totalMem = memLines.find { it.startsWith("MemTotal:") }
                        ?.split(Regex("\\s+"))?.getOrNull(1)?.toLongOrNull() ?: 1L
                    val availMem = memLines.find { it.startsWith("MemAvailable:") }
                        ?.split(Regex("\\s+"))?.getOrNull(1)?.toLongOrNull() ?: 0L
                    val usedMem = totalMem - availMem
                    val ramPercent = usedMem.toFloat() / totalMem.toFloat()

                    // 4. System Uptime & Deep Sleep
                    val elapsed = android.os.SystemClock.elapsedRealtime()
                    val active = android.os.SystemClock.uptimeMillis()
                    val sleepMs = elapsed - active
                    val sleepPercent = if (elapsed > 0) (sleepMs.toFloat() / elapsed.toFloat() * 100).toInt() else 0
                    
                    val uptimeSec = (elapsed / 1000).toInt()
                    val uptimeStr = "${uptimeSec / 3600}h ${(uptimeSec % 3600) / 60}m"
                    val uptimeFull = "${uptimeSec / 3600}h ${(uptimeSec % 3600) / 60}m ${uptimeSec % 60}s"
                    
                    val sleepSec = (sleepMs / 1000).toInt()
                    val deepSleepStr = "$sleepPercent%"
                    val deepSleepFull = "${sleepSec / 3600}h ${(sleepSec % 3600) / 60}m ${sleepSec % 60}s ($sleepPercent%)"

                    // 4.1 System Advanced Meta
                    val selinux = SmartShell.sh("getenforce").ifEmpty { "Enforcing" }
                    val buildNum = SmartShell.sh("getprop ro.build.display.id")
                    val bootloader = SmartShell.sh("getprop ro.bootloader")
                    val baseband = SmartShell.sh("getprop gsm.version.baseband")

                    // 5. GPU & Thermal
                    val gpuFreq = GpuController.getCurrentFrequency()
                    val thermalHealth = ThermalController.getThermalTechnicalReport()

                    // 6. Storage Telemetry
                    val storageData = HardwareMonitor.getStorageUsage()
                    val storagePercent = storageData.first
                    val storageDetail = storageData.second

                    // 8. Charging Status
                    val batteryStatusPath = registry.subsystems["BATTERY"]?.get("status") ?: ""
                    val isCharging = if (batteryStatusPath.isNotEmpty())
                        SmartShell.read(batteryStatusPath).equals("Charging", ignoreCase = true)
                    else false

                    // 9. Kernel & System Meta
                    val kernelFull = SmartShell.read("/proc/version")
                    // Clean kernel: 5.10.247-android12-9-g95... -> 5.10.247
                    val kernelRaw = kernelFull.split(Regex("\\s+")).getOrNull(2) ?: "Unknown"
                    val kernelVersion = kernelRaw.substringBefore("-").ifEmpty { kernelRaw }
                    val securityPatch = SmartShell.sh("getprop ro.build.version.security_patch")
                    
                    val socModel = registry.socModel

                    // 10. New Pulse Telemetries (No Hardcoding)
                    val cpuTemp = HardwareMonitor.getCpuTemp()
                    val gpuTemp = HardwareMonitor.getGpuTemp()
                    val cpuLoad = HardwareMonitor.getCpuLoad()
                    val display = HardwareMonitor.getDisplayStats()
                    val appTotal = SmartShell.sh("pm list packages | wc -l").trim()
                    val userApps = SmartShell.sh("pm list packages -3 | wc -l").trim()
                          // 11. State Update (Partial - Realtime)
                    _state.value = _state.value.copy(
                        cpuFrequencies = freqs,
                        cpuHistory = newHistory,
                        batteryTemp = temp,
                        batteryCurrent = current,
                        batteryLevel = BatteryController.getCapacity(),
                        isCharging = isCharging,
                        ramUsage = ramPercent,
                        ramDetail = "${usedMem / 1024} MB of ${totalMem / 1024} MB used",
                        ramTotal = "${(totalMem + 512000) / 1024 / 1024} GB",
                        gpuFreq = gpuFreq,
                        uptime = uptimeStr,
                        uptimeFull = uptimeFull,
                        deepSleep = deepSleepStr,
                        deepSleepFull = deepSleepFull,
                        cpuTemp = cpuTemp,
                        gpuTemp = gpuTemp,
                        cpuLoad = cpuLoad,
                        displayStats = display,
                        deviceName = registry.marketName,
                        androidVersion = "Android ${android.os.Build.VERSION.RELEASE}",
                        phoneModel = registry.deviceCodename.uppercase(),
                        chipset = heritage.model,
                        socModel = socModel
                    )

                    // 12. Slow Telemetry (Every 30s or first run)
                    if (slowTick % 30 == 0) {
                        val storageData = HardwareMonitor.getStorageUsage()
                        val appTotal = SmartShell.sh("pm list packages | wc -l").trim()
                        val userApps = SmartShell.sh("pm list packages -3 | wc -l").trim()
                        val selinux = SmartShell.sh("getenforce").ifEmpty { "Enforcing" }
                        val buildNum = SmartShell.sh("getprop ro.build.display.id")
                        val baseband = SmartShell.sh("getprop gsm.version.baseband")
                        val networkInfo = HardwareMonitor.getNetworkInfo()

                        _state.value = _state.value.copy(
                            storageUsage = storageData.first,
                            storageDetail = storageData.second,
                            appCount = "$appTotal Total · $userApps User",
                            selinuxStatus = selinux,
                            buildNumber = buildNum,
                            baseband = baseband,
                            bootloader = registry.bootloaderVersion,
                            securityPatch = SmartShell.sh("getprop ro.build.version.security_patch"),
                            kernelVersion = kernelVersion,
                            ioScheduler = StorageController.getCurrentScheduler(),
                            networkType = networkInfo.first
                        )
                    }
                    
                    slowTick++
                } catch (e: Exception) {
                    log("Telemetry Error: ${e.message}")
                }
                delay(1000)
            }
        }
    }

    /**
     * Diagnostic Logging.
     */
    private fun log(message: String) {
        Log.d(TAG, message)
    }

    /**
     * Refreshes the hardware mapping (Emergency Re-Discovery).
     */
    fun triggerReDiscovery() {
        viewModelScope.launch {
            log("Emergency hardware re-discovery initiated.")
            SystemDiscovery.discover()
        }
    }

    /**
     * Performs a health audit and logs the result.
     */
    fun performHealthAudit() {
        viewModelScope.launch {
            val success = TuningOrchestrator.performOmniAudit()
            log("System Health Audit Result: ${if (success) "PASS" else "FAIL"}")
        }
    }
}
