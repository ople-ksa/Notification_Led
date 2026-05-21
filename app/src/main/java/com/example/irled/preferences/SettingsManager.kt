package com.example.irled.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        val CALL_ALERTS_ENABLED = booleanPreferencesKey("call_alerts_enabled")
        val CHARGING_ALERTS_ENABLED = booleanPreferencesKey("charging_alerts_enabled")
        val BATTERY_FULL_ALERTS_ENABLED = booleanPreferencesKey("battery_full_alerts_enabled")
        val LOW_BATTERY_ALERTS_ENABLED = booleanPreferencesKey("low_battery_alerts_enabled")
        val LOW_BATTERY_THRESHOLD = androidx.datastore.preferences.core.intPreferencesKey("low_battery_threshold")
    }

    val callAlertsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[CALL_ALERTS_ENABLED] ?: true }

    val chargingAlertsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[CHARGING_ALERTS_ENABLED] ?: true }

    val batteryFullAlertsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[BATTERY_FULL_ALERTS_ENABLED] ?: true }

    val lowBatteryAlertsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[LOW_BATTERY_ALERTS_ENABLED] ?: true }

    val lowBatteryThreshold: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[LOW_BATTERY_THRESHOLD] ?: 20 }

    suspend fun setCallAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CALL_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setChargingAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CHARGING_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setBatteryFullAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BATTERY_FULL_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setLowBatteryAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOW_BATTERY_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setLowBatteryThreshold(threshold: Int) {
        context.dataStore.edit { preferences ->
            preferences[LOW_BATTERY_THRESHOLD] = threshold
        }
    }
}
