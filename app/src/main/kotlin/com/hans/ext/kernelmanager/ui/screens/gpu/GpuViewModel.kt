package com.hans.ext.kernelmanager.ui.screens.gpu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hans.ext.kernelmanager.hal.GpuController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class GpuState(
    val isLoading: Boolean = true,
    val availableGovernors: List<String> = emptyList(),
    val currentGovernor: String = "",
    val currentFrequency: String = "N/A",
    val availableFrequencies: List<String> = emptyList(),
    val minFreq: String = "",
    val maxFreq: String = "",
    val adrenoBoostAvailable: Boolean = false,
    val adrenoIdlerAvailable: Boolean = false
)

class GpuViewModel : ViewModel() {
    private val _state = MutableStateFlow(GpuState())
    val state: StateFlow<GpuState> = _state

    init {
        loadGpuInfo()
    }

    private fun loadGpuInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val governors = GpuController.getAvailableGovernors()
            val currentGov = GpuController.getCurrentGovernor()
            val currentFreq = GpuController.getCurrentFrequency()
            val availableFreqs = GpuController.getAvailableFrequencies()
            val adrenoBoost = GpuController.isAdrenoBoostAvailable()
            val adrenoIdler = GpuController.isAdrenoIdlerAvailable()

            _state.value = GpuState(
                isLoading = false,
                availableGovernors = governors,
                currentGovernor = currentGov,
                currentFrequency = currentFreq,
                availableFrequencies = availableFreqs,
                minFreq = availableFreqs.lastOrNull() ?: "",
                maxFreq = availableFreqs.firstOrNull() ?: "",
                adrenoBoostAvailable = adrenoBoost,
                adrenoIdlerAvailable = adrenoIdler
            )
        }
    }

    fun setGovernor(governor: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = GpuController.setGovernor(governor)
            if (success) _state.value = _state.value.copy(currentGovernor = governor)
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
        _state.value = _state.value.copy(isLoading = true)
        loadGpuInfo()
    }
}
