package com.hans.ext.kernelmanager.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.hans.ext.kernelmanager.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileService : AccessibilityService() {
    private var lastPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            if (packageName != lastPackage) {
                lastPackage = packageName
                checkAndApplyProfile(packageName)
            }
        }
    }

    private fun checkAndApplyProfile(packageName: String) {
        // Logic to fetch profile from DB and apply it
        // This would call applySetting() logic similar to BootReceiver
    }

    override fun onInterrupt() {}
}
