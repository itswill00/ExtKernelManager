package com.hans.ext.kernelmanager.ui.screens.cpu

import androidx.compose.material3.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hans.ext.kernelmanager.hal.CpuController
import com.hans.ext.kernelmanager.hal.intelligence.SystemDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CpuState(
    val isLoading: Boolean = true,
    val policies: List<Int> = emptyList(),
    val governors: Map<Int, List<String>> = emptyMap(),   // policy -> available governors
    val currentGov: Map<Int, String> = emptyMap(),         // policy -> current governor
    val minFreq: Map<Int, String> = emptyMap(),
    val maxFreq: Map<Int, String> = emptyMap(),
    val availableFreqs: Map<Int, List<String>> = emptyMap(),
    val cpuCoreMap: Map<Int, String> = emptyMap(),
    val coreFrequencies: Map<Int, String> = emptyMap()
)

class CpuViewModel : ViewModel() {
    private val _state = MutableStateFlow(CpuState())
    val state: StateFlow<CpuState> = _state

    private var isMonitoring = false

    init {
        loadCpuInfo()
        startMonitoring()
    }

    private fun loadCpuInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val policies = SystemDiscovery.getRegistry().cpuPolicies

            val governors = mutableMapOf<Int, List<String>>()
            val currentGov = mutableMapOf<Int, String>()
            val minFreq = mutableMapOf<Int, String>()
            val maxFreq = mutableMapOf<Int, String>()
            val availableFreqs = mutableMapOf<Int, List<String>>()

            for (p in policies) {
                governors[p] = CpuController.getAvailableGovernors(p)
                currentGov[p] = CpuController.getCurrentGovernor(p)
                minFreq[p] = CpuController.getMinFrequency(p)
                maxFreq[p] = CpuController.getMaxFrequency(p)
                availableFreqs[p] = CpuController.getAvailableFrequencies(p)
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    policies = policies,
                    governors = governors,
                    currentGov = currentGov,
                    minFreq = minFreq,
                    maxFreq = maxFreq,
                    availableFreqs = availableFreqs,
                    cpuCoreMap = SystemDiscovery.getRegistry().cpuCoreMap
                )
            }
        }
    }

    private fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        viewModelScope.launch(Dispatchers.IO) {
            val total = SystemDiscovery.getRegistry().totalCores
            while (isMonitoring) {
                val freqs = mutableMapOf<Int, String>()
                for (i in 0 until total) {
                    freqs[i] = CpuController.getCoreFrequency(i)
                }
                _state.update { it.copy(coreFrequencies = freqs) }
                delay(1200)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        isMonitoring = false
    }

    fun setGovernor(policy: Int, governor: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = CpuController.setGovernor(policy, governor)
            if (success) {
                _state.update { s ->
                    val updated = s.currentGov.toMutableMap().apply { put(policy, governor) }
                    s.copy(currentGov = updated)
                }
            }
        }
    }

    fun setMinFreq(policy: Int, freq: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = CpuController.setMinFrequency(policy, freq)
            if (success) {
                _state.update { s ->
                    val updated = s.minFreq.toMutableMap().apply { put(policy, freq) }
                    s.copy(minFreq = updated)
                }
            }
        }
    }

    fun setMaxFreq(policy: Int, freq: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = CpuController.setMaxFrequency(policy, freq)
            if (success) {
                _state.update { s ->
                    val updated = s.maxFreq.toMutableMap().apply { put(policy, freq) }
                    s.copy(maxFreq = updated)
                }
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(isLoading = true) }
        loadCpuInfo()
    }
}
