package com.example.irled

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Destination : NavKey {
    @Serializable
    data object Onboarding : Destination

    @Serializable
    data object Main : Destination

    @Serializable
    data object Settings : Destination
}
