package com.hans.ext.kernelmanager.ui.screens.iomem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hans.ext.kernelmanager.hal.StorageController
import com.hans.ext.kernelmanager.hal.MemoryController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class IoMemState(
    val isLoading: Boolean = true,
    val availableSchedulers: List<String> = emptyList(),
    val currentScheduler: String = "",
    val readAhead: String = "0",
    val swappiness: String = "N/A",
    val vfsCachePressure: String = "N/A",
    val isZramEnabled: Boolean = false,
    val zramSize: String = "N/A",
    val zramAlgo: String = "N/A",
    val availableAlgos: List<String> = emptyList(),
    val lmkMinfree: String = "N/A",
    val isKsmAvailable: Boolean = false,
    val isKsmEnabled: Boolean = false,
    val ksmStats: Map<String, String> = emptyMap()
)

class IoMemViewModel : ViewModel() {
    private val _state = MutableStateFlow(IoMemState())
    val state: StateFlow<IoMemState> = _state

    init {
        loadIoMemInfo()
    }

    private fun loadIoMemInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val schedulers = StorageController.getAvailableSchedulers()
            val currentSched = StorageController.getCurrentScheduler()
            val readAhead = StorageController.getReadAhead()
            val swappiness = MemoryController.getSwappiness()
            val vfsCache = MemoryController.getVfsCachePressure()
            val zramEnabled = MemoryController.isZramEnabled()
            val zramSize = MemoryController.getZramDiskSize()
            val zramAlgo = MemoryController.getZramCompAlgorithm()
            val lmk = MemoryController.getLmkMinfree()
            
            val ksmAvail = MemoryController.isKsmAvailable()
            val ksmRun = if (ksmAvail) com.hans.ext.kernelmanager.util.SmartShell.read("/sys/kernel/mm/ksm/run") == "1" else false
            val ksmStats = if (ksmAvail) MemoryController.getKsmStats() else emptyMap()

            _state.value = IoMemState(
                isLoading = false,
                availableSchedulers = schedulers,
                currentScheduler = currentSched,
                readAhead = readAhead,
                swappiness = swappiness,
                vfsCachePressure = vfsCache,
                isZramEnabled = zramEnabled,
                zramSize = zramSize,
                zramAlgo = zramAlgo,
                lmkMinfree = lmk,
                isKsmAvailable = ksmAvail,
                isKsmEnabled = ksmRun,
                ksmStats = ksmStats
            )
        }
    }

    fun setScheduler(scheduler: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = StorageController.setScheduler(scheduler)
            if (success) _state.value = _state.value.copy(currentScheduler = scheduler)
        }
    }

    fun setSwappiness(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = MemoryController.setSwappiness(value)
            if (success) _state.value = _state.value.copy(swappiness = value.toString())
        }
    }

    fun setReadAhead(kb: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = StorageController.setReadAhead(kb.toIntOrNull() ?: 128)
            if (success) _state.value = _state.value.copy(readAhead = kb)
        }
    }

    fun setVfsCachePressure(value: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = MemoryController.setVfsCachePressure(value)
            if (success) _state.value = _state.value.copy(vfsCachePressure = value.toString())
        }
    }

    fun setZramAlgo(algo: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = MemoryController.setZramCompAlgorithm(algo)
            if (success) _state.value = _state.value.copy(zramAlgo = algo)
        }
    }

    fun applyLmkProfile(tier: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = MemoryController.applyIntelligentLmkProfile(tier)
            if (success) {
                val newLmk = MemoryController.getLmkMinfree()
                _state.value = _state.value.copy(lmkMinfree = newLmk)
            }
        }
    }

    fun toggleKsm(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = MemoryController.setKsm(enabled)
            if (success) _state.value = _state.value.copy(isKsmEnabled = enabled)
        }
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true)
        loadIoMemInfo()
    }
}
