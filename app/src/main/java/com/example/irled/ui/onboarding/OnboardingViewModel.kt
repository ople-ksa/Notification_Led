package com.example.irled.ui.onboarding

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.example.irled.HardwareManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {
    private val hardwareManager = HardwareManager(application)

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        _uiState.update {
            it.copy(
                hasIrEmitter = hardwareManager.hasIrEmitter(),
                isNotificationAccessGranted = hardwareManager.isNotificationAccessGranted(),
                isPhonePermissionGranted = ContextCompat.checkSelfPermission(
                    getApplication(),
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
    }
}

data class OnboardingUiState(
    val hasIrEmitter: Boolean = false,
    val isNotificationAccessGranted: Boolean = false,
    val isPhonePermissionGranted: Boolean = false
)
