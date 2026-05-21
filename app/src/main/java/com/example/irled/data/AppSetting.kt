package com.example.irled.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AppCategory {
    CELLULAR, EMAIL, SOCIAL, GENERAL
}

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val packageName: String,
    val isEnabled: Boolean = true,
    val onDurationMs: Long = 100,
    val offDurationMs: Long = 1000,
    val repeatCount: Int = 3,
    val repeatIndefinitelyIfLocked: Boolean = false,
    val isMessageApp: Boolean = false,
    val isGradient: Boolean = false,
    val isHighBrightness: Boolean = false,
    val category: AppCategory = AppCategory.GENERAL
)
