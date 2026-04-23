package com.hans.ext.kernelmanager.util

import com.topjohnwu.superuser.Shell

object SmartShell {

    /** Baca satu baris dari file sysfs (single-value nodes). */
    fun read(path: String): String =
        Shell.cmd("cat $path").exec().out.firstOrNull()?.trim() ?: ""

    /** Baca semua baris dari file (multiline: /proc/meminfo, dll). */
    fun readLines(path: String): List<String> =
        Shell.cmd("cat $path").exec().out.map { it.trim() }.filter { it.isNotEmpty() }

    /** Jalankan shell command, ambil baris pertama output. */
    fun sh(cmd: String): String =
        Shell.cmd(cmd).exec().out.firstOrNull()?.trim() ?: ""

    /** Jalankan shell command, ambil SEMUA baris output. */
    fun shLines(cmd: String): List<String> =
        Shell.cmd(cmd).exec().out.map { it.trim() }.filter { it.isNotEmpty() }

    /** Convenience: baca integer langsung dari node sysfs. */
    fun readInt(path: String): Int? = read(path).toIntOrNull()

    /** Convenience: baca long langsung dari node sysfs. */
    fun readLong(path: String): Long? = read(path).toLongOrNull()

    /** Tulis nilai ke node sysfs dengan tiga strategi fallback. */
    fun write(path: String, value: String): Boolean {
        // Strategi 1: tulis langsung
        Shell.cmd("echo '$value' > $path").exec()
        if (verify(path, value)) return true

        // Strategi 2: perbaiki permission lalu tulis ulang
        Shell.cmd("chmod 666 $path", "chown root:root $path", "echo '$value' > $path").exec()
        if (verify(path, value)) return true

        // Strategi 3: tee sebagai fallback aman (tanpa menyentuh SELinux)
        Shell.cmd("echo '$value' | tee $path > /dev/null 2>&1").exec()
        return verify(path, value)
    }

    private fun verify(path: String, expected: String): Boolean = read(path) == expected

    /** Cek apakah node sysfs/procfs ada. Menggunakan sh() agar shell mengeksekusi kondisi. */
    fun nodeExists(path: String): Boolean {
        if (path.isEmpty()) return false
        return sh("[ -e $path ] && echo 1 || echo 0") == "1"
    }

    fun isCommandAvailable(cmd: String): Boolean =
        Shell.cmd("which $cmd").exec().isSuccess
}
