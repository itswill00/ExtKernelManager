package com.hans.ext.kernelmanager.ui.screens.gpu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hans.ext.kernelmanager.hal.GpuController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GpuState(
    val isLoading: Boolean = true,
    val availableGovernors: List<String> = emptyList(),
    val currentGovernor: String = "",
    val currentFrequency: String = "N/A",
    val availableFrequencies: List<String> = emptyList(),
    val availablePowerPolicies: List<String> = emptyList(),
    val currentPowerPolicy: String = "",
    val minFreq: String = "",
    val maxFreq: String = "",
    val adrenoBoostAvailable: Boolean = false,
    val adrenoIdlerAvailable: Boolean = false,
    // Advanced Meta
    val gpuRenderer: String = "Unknown Renderer",
    val gpuLoad: Int = 0,
    val gpuBusSpeed: String = "N/A",
    val gpuMemoryUsage: String = "N/A",
    val gpuTemp: String = "N/A"
)

class GpuViewModel : ViewModel() {
    private val _state = MutableStateFlow(GpuState())
    val state: StateFlow<GpuState> = _state

    private var telemetryJob: kotlinx.coroutines.Job? = null

    init {
        loadStaticInfo()
        // Telemetry is now started manually via lifecycle
    }

    private fun loadStaticInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val governors = GpuController.getAvailableGovernors()
            val availableFreqs = GpuController.getAvailableFrequencies()
            val availablePolicies = GpuController.getAvailablePowerPolicies()
            val adrenoBoost = GpuController.isAdrenoBoostAvailable()
            val adrenoIdler = GpuController.isAdrenoIdlerAvailable()
            val renderer = GpuController.getRendererName()

            _state.update { it.copy(
                isLoading = false,
                availableGovernors = governors,
                availableFrequencies = availableFreqs,
                availablePowerPolicies = availablePolicies,
                adrenoBoostAvailable = adrenoBoost,
                adrenoIdlerAvailable = adrenoIdler,
                gpuRenderer = renderer,
                minFreq = availableFreqs.lastOrNull() ?: "",
                maxFreq = availableFreqs.firstOrNull() ?: ""
            ) }
        }
    }

    fun startTelemetry() {
        if (telemetryJob?.isActive == true) return
        telemetryJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                val currentFreq = GpuController.getCurrentFrequency()
                val currentGov = GpuController.getCurrentGovernor()
                val currentPolicy = GpuController.getCurrentPowerPolicy()
                val load = GpuController.getLoad()
                val bus = GpuController.getBusSpeed()
                val mem = GpuController.getMemoryUsage()
                val temp = GpuController.getTemperature()

                _state.update { it.copy(
                    currentFrequency = currentFreq,
                    currentGovernor = currentGov,
                    currentPowerPolicy = currentPolicy,
                    gpuLoad = load,
                    gpuBusSpeed = bus,
                    gpuMemoryUsage = mem,
                    gpuTemp = temp
                ) }
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    fun stopTelemetry() {
        telemetryJob?.cancel()
        telemetryJob = null
    }

    fun setGovernor(governor: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = GpuController.setGovernor(governor)
            if (success) _state.value = _state.value.copy(currentGovernor = governor)
        }
    }

    fun setPowerPolicy(policy: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = GpuController.setPowerPolicy(policy)
            if (success) _state.value = _state.value.copy(currentPowerPolicy = policy)
        }
    }

    fun setMinFreq(freq: String) {
        viewModelScope.launch(Dispatchers.IO) {
            GpuController.setMinFrequency(freq)
            _state.value = _state.value.copy(minFreq = freq)
        }
    }

    fun setMaxFreq(freq: String) {
        viewModelScope.launch(Dispatchers.IO) {
            GpuController.setMaxFrequency(freq)
            _state.value = _state.value.copy(maxFreq = freq)
        }
    }

    fun setAdrenoBoost(level: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            GpuController.setAdrenoBoost(level)
        }
    }

    fun setAdrenoIdler(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            GpuController.setAdrenoIdler(enabled)
        }
    }

    fun refresh() {
        loadStaticInfo()
    }
}
