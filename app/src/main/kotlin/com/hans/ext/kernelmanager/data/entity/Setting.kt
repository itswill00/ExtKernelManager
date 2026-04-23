package com.hans.ext.kernelmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class Setting(
    @PrimaryKey val key: String,
    val value: String,
    val applyOnBoot: Boolean = false
)


