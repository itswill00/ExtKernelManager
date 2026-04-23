package com.hans.ext.kernelmanager.util

import com.topjohnwu.superuser.Shell
import java.io.File

object ScriptManager {
    fun runScript(script: String): List<String> {
        return Shell.cmd(script).exec().out
    }

    fun saveScript(context: android.content.Context, name: String, content: String) {
        val file = File(context.filesDir, "scripts/$name.sh")
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    fun listScripts(context: android.content.Context): List<String> {
        val dir = File(context.filesDir, "scripts")
        return dir.list()?.toList() ?: emptyList()
    }
}
