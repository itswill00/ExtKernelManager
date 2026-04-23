package com.hans.ext.kernelmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "setting_persistence")
data class SettingPersistence(
    @PrimaryKey val path: String,
    val value: String,
    val name: String
)
