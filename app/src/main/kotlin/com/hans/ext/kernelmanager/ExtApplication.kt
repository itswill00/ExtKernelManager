package com.hans.ext.kernelmanager

import android.app.Application
import com.topjohnwu.superuser.Shell

class ExtApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize libsu
        // Configure shell to always use root
        Shell.setDefaultBuilder(Shell.Builder.create()
            .setFlags(Shell.FLAG_REDIRECT_STDERR)
            .setTimeout(10))
    }
}
