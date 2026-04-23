package com.hans.ext.kernelmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey val packageName: String,
    val cpuGovernor: String?,
    val gpuGovernor: String?,
    val ioScheduler: String?,
    val minFreq: String?,
    val maxFreq: String?
)
