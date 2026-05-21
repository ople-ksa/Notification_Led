package com.example.irled.ui.main

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.irled.data.AppDatabase
import com.example.irled.data.AppSetting
import com.example.irled.data.AppCategory
import kotlinx.coroutines.Dispatchers
import com.example.irled.preferences.SettingsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).appSettingDao()
    private val packageManager = application.packageManager
    private val settingsManager = SettingsManager(application)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps = _installedApps.asStateFlow()

    private val _enabledSettings = dao.getAllSettings().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val callAlertsEnabled = settingsManager.callAlertsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val chargingAlertsEnabled = settingsManager.chargingAlertsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val batteryFullAlertsEnabled = settingsManager.batteryFullAlertsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )

    val lowBatteryAlertsEnabled = settingsManager.lowBatteryAlertsEnabled.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val lowBatteryThreshold = settingsManager.lowBatteryThreshold.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 20
    )

    val uiState = combine(_installedApps, _enabledSettings) { installed, enabled ->
        val enabledMap = enabled.associateBy { it.packageName }
        installed.map { app ->
            AppItemState(
                packageName = app.packageName,
                appName = app.label,
                icon = app.icon,
                isEnabled = enabledMap[app.packageName]?.isEnabled ?: false,
                setting = enabledMap[app.packageName],
                isSystemApp = app.isSystemApp
            )
        }.sortedBy { it.appName }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .map { app ->
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || 
                                  (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    AppInfo(
                        packageName = app.packageName,
                        label = app.loadLabel(packageManager).toString(),
                        icon = app.loadIcon(packageManager),
                        isSystemApp = isSystem
                    )
                }
            _installedApps.value = apps
        }
    }

    fun toggleApp(packageName: String, isEnabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getSetting(packageName)
            if (existing == null && isEnabled) {
                dao.insertOrUpdate(AppSetting(packageName = packageName, isEnabled = true))
            } else if (existing != null) {
                dao.insertOrUpdate(existing.copy(isEnabled = isEnabled))
            }
        }
    }

    fun updatePattern(packageName: String, onMs: Long, offMs: Long, repeat: Int, repeatIndefinitely: Boolean, isMessageApp: Boolean, isGradient: Boolean, isHighBrightness: Boolean, category: AppCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getSetting(packageName) ?: AppSetting(packageName = packageName)
            dao.insertOrUpdate(
                existing.copy(
                    onDurationMs = onMs,
                    offDurationMs = offMs,
                    repeatCount = repeat,
                    repeatIndefinitelyIfLocked = repeatIndefinitely,
                    isMessageApp = isMessageApp,
                    isGradient = isGradient,
                    isHighBrightness = isHighBrightness,
                    category = category
                )
            )
        }
    }

    val priorityList = combine(_enabledSettings, _installedApps) { enabled, installed ->
        val installedMap = installed.associateBy { it.packageName }
        val list = mutableListOf<PriorityItem>()
        
        list.add(PriorityItem("Battery Alerts", "Critical < 20% | Full (100%) | Charger In", true))
        list.add(PriorityItem("Call Alerts", "Ongoing > Missed", true))
        
        val cellular = enabled.filter { it.category == AppCategory.CELLULAR && it.isEnabled }
            .map { PriorityItem(installedMap[it.packageName]?.label ?: it.packageName, "Cellular Message", false) }
        val email = enabled.filter { it.category == AppCategory.EMAIL && it.isEnabled }
            .map { PriorityItem(installedMap[it.packageName]?.label ?: it.packageName, "Email", false) }
        val social = enabled.filter { it.category == AppCategory.SOCIAL && it.isEnabled }
            .map { PriorityItem(installedMap[it.packageName]?.label ?: it.packageName, "Social", false) }
        val general = enabled.filter { it.category == AppCategory.GENERAL && it.isEnabled }
            .map { PriorityItem(installedMap[it.packageName]?.label ?: it.packageName, "General Notification", false) }
            
        list.addAll(cellular)
        list.addAll(email)
        list.addAll(social)
        list.addAll(general)
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleCallAlerts(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setCallAlertsEnabled(enabled) }
    }

    fun toggleChargingAlerts(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setChargingAlertsEnabled(enabled) }
    }

    fun toggleBatteryFullAlerts(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setBatteryFullAlertsEnabled(enabled) }
    }

    fun toggleLowBatteryAlerts(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setLowBatteryAlertsEnabled(enabled) }
    }

    fun setLowBatteryThreshold(threshold: Int) {
        viewModelScope.launch { settingsManager.setLowBatteryThreshold(threshold) }
    }
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable,
    val isSystemApp: Boolean
)

data class AppItemState(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    val isEnabled: Boolean,
    val setting: AppSetting?,
    val isSystemApp: Boolean
)

data class PriorityItem(
    val name: String,
    val detail: String,
    val isSystem: Boolean
)
