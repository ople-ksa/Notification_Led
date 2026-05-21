package com.example.irled.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings")
    fun getAllSettings(): Flow<List<AppSetting>>

    @Query("SELECT * FROM app_settings")
    suspend fun getAllSettingsOnce(): List<AppSetting>

    @Query("SELECT * FROM app_settings WHERE packageName = :packageName")
    suspend fun getSetting(packageName: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(setting: AppSetting)

    @Delete
    suspend fun delete(setting: AppSetting)

    @Query("DELETE FROM app_settings WHERE packageName = :packageName")
    suspend fun deleteByPackage(packageName: String)
}
