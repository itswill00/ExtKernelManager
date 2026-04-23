package com.hans.ext.kernelmanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hans.ext.kernelmanager.data.AppDatabase
import com.hans.ext.kernelmanager.hal.CpuController
import com.hans.ext.kernelmanager.hal.GpuController
import com.hans.ext.kernelmanager.hal.StorageController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                val settings = db.settingDao().getBootSettings()
                settings.forEach { setting ->
                    applySetting(setting.key, setting.value)
                }
            }
        }
    }

    private fun applySetting(key: String, value: String) {
        when {
            key.startsWith("cpu_governor_") -> {
                val cluster = key.removePrefix("cpu_governor_").toInt()
                CpuController.setGovernor(cluster, value)
            }
            key.startsWith("cpu_max_freq_") -> {
                val cluster = key.removePrefix("cpu_max_freq_").toInt()
                CpuController.setMaxFrequency(cluster, value)
            }
            key == "gpu_governor" -> GpuController.setGovernor(value)
            key == "io_scheduler" -> StorageController.setScheduler(value)
            key == "read_ahead"   -> StorageController.setReadAhead(value.toIntOrNull() ?: 128)
        }
    }
}
