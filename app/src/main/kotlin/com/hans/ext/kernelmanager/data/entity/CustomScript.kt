package com.hans.ext.kernelmanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_scripts")
data class CustomScript(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val content: String,
    val runOnBoot: Boolean = false
)
