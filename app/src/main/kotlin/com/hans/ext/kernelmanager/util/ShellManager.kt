package com.hans.ext.kernelmanager.util

import com.topjohnwu.superuser.Shell

object ShellManager {
    fun isRootAvailable(): Boolean {
        return Shell.getShell().isRoot
    }

    fun exec(command: String): List<String> {
        val result = Shell.cmd(command).exec()
        return if (result.isSuccess) result.out else emptyList()
    }

    fun execWrite(path: String, value: String): Boolean {
        // Dynamic permission handling: try writing directly, if fail try chmod
        val result = Shell.cmd("echo '$value' > $path").exec()
        if (!result.isSuccess) {
            // Attempt recovery if it's a permission issue
            Shell.cmd("chmod 644 $path", "echo '$value' > $path").exec()
        }
        return Shell.cmd("cat $path").exec().out.firstOrNull()?.trim() == value
    }

    fun commandExists(cmd: String): Boolean {
        return Shell.cmd("which $cmd").exec().isSuccess
    }
}
