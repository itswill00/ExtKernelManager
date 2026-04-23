package com.hans.ext.kernelmanager.util

import com.topjohnwu.superuser.Shell

object BuildPropEditor {
    private const val PATH = "/system/build.prop"

    fun getAllProps(): Map<String, String> {
        val lines = Shell.cmd("cat $PATH").exec().out
        val props = mutableMapOf<String, String>()
        lines.forEach { line ->
            if (line.contains("=") && !line.startsWith("#")) {
                val parts = line.split("=")
                if (parts.size == 2) {
                    props[parts[0]] = parts[1]
                }
            }
        }
        return props
    }

    fun setProp(key: String, value: String): Boolean {
        // This requires mounting system as RW
        Shell.cmd("mount -o rw,remount /system").exec()
        val result = Shell.cmd("sed -i 's/^$key=.*/$key=$value/' $PATH").exec()
        Shell.cmd("mount -o ro,remount /system").exec()
        return result.isSuccess
    }
}
