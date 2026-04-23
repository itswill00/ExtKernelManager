package com.hans.ext.kernelmanager.ui.screens.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hans.ext.kernelmanager.util.ShellUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LogViewModel : ViewModel() {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private var isMonitoring = false

    fun startLogging() {
        if (isMonitoring) return
        isMonitoring = true
        viewModelScope.launch {
            while (isMonitoring) {
                val output = ShellUtils.read("dmesg | tail -n 100")
                if (output.isNotEmpty()) {
                    _logs.value = output.split("\n").filter { it.isNotEmpty() }
                }
                delay(2000) // Refresh every 2 seconds
            }
        }
    }

    fun stopLogging() {
        isMonitoring = false
    }

    fun clearLogs() {
        ShellUtils.read("dmesg -c") // Requires root
        _logs.value = emptyList()
    }
}
