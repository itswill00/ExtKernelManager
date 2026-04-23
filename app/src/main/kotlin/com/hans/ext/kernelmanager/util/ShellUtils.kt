package com.hans.ext.kernelmanager.util

import com.topjohnwu.superuser.Shell

object ShellUtils {
    fun read(path: String): String {
        return ShellManager.exec("cat $path").firstOrNull()?.trim() ?: ""
    }

    fun exists(path: String): Boolean {
        return ShellManager.exec("[ -e $path ] && echo 1").isNotEmpty()
    }

    fun write(path: String, value: String): Boolean {
        return ShellManager.execWrite(path, value)
    }

    fun readProp(key: String): String {
        return ShellManager.exec("getprop $key").firstOrNull()?.trim() ?: ""
    }

    fun getFiles(directory: String): List<String> {
        val result = Shell.cmd("ls $directory").exec()
        return if (result.isSuccess) result.out else emptyList()
    }
}
