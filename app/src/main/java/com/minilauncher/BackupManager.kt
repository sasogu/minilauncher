package com.minilauncher

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

object BackupManager {
    private const val BACKUP_VERSION = 1

    suspend fun exportToUri(context: Context, uri: android.net.Uri) = withContext(Dispatchers.IO) {
        val dataStore = context.launcherDataStore
        val backupJson = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("languageTag", dataStore.readString(LauncherPreferenceKeys.languageTag, AppLanguage.SPANISH.localeTag))
            put("favoritePackagesOrder", dataStore.readString(LauncherPreferenceKeys.favoritePackagesOrder, ""))
            put("hiddenPackages", dataStore.readString(LauncherPreferenceKeys.hiddenPackages, ""))
            put("appTags", dataStore.readString(LauncherPreferenceKeys.appTags, ""))
            put("themeMode", dataStore.readString(LauncherPreferenceKeys.themeMode, ThemeMode.DARK.storageValue))
            put("usagePromptEnabled", dataStore.readBoolean(LauncherPreferenceKeys.usagePromptEnabled, false))
            put("moonIlluminationPercentageVisible", dataStore.readBoolean(LauncherPreferenceKeys.moonIlluminationPercentageVisible, true))
            put("homeWeekdayVisible", dataStore.readBoolean(LauncherPreferenceKeys.homeWeekdayVisible, true))
            put("homeDateVisible", dataStore.readBoolean(LauncherPreferenceKeys.homeDateVisible, true))
            put("homeUse24HourTime", dataStore.readBoolean(LauncherPreferenceKeys.homeUse24HourTime, true))
            put("homeReorderHintDismissed", dataStore.readBoolean(LauncherPreferenceKeys.homeReorderHintDismissed, false))
        }

        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("Could not open output stream")
        output.bufferedWriter().use { writer ->
            writer.write(backupJson.toString(2))
        }
    }

    suspend fun importFromUri(context: Context, uri: android.net.Uri) = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open input stream")
        val jsonText = input.bufferedReader().use { it.readText() }
        val backupJson = JSONObject(jsonText)

        val version = backupJson.optInt("version", -1)
        require(version in 1..BACKUP_VERSION) { "Unsupported backup version" }

        context.launcherDataStore.edit { preferences ->
            setOrRemove(preferences, LauncherPreferenceKeys.languageTag, backupJson.optString("languageTag", AppLanguage.SPANISH.localeTag))
            setOrRemove(preferences, LauncherPreferenceKeys.favoritePackagesOrder, backupJson.optString("favoritePackagesOrder", ""))
            setOrRemove(preferences, LauncherPreferenceKeys.hiddenPackages, backupJson.optString("hiddenPackages", ""))
            setOrRemove(preferences, LauncherPreferenceKeys.appTags, backupJson.optString("appTags", ""))
            setOrRemove(preferences, LauncherPreferenceKeys.themeMode, backupJson.optString("themeMode", ThemeMode.DARK.storageValue))
            preferences[LauncherPreferenceKeys.usagePromptEnabled] = backupJson.optBoolean("usagePromptEnabled", false)
            preferences[LauncherPreferenceKeys.moonIlluminationPercentageVisible] = backupJson.optBoolean("moonIlluminationPercentageVisible", true)
            preferences[LauncherPreferenceKeys.homeWeekdayVisible] = backupJson.optBoolean("homeWeekdayVisible", true)
            preferences[LauncherPreferenceKeys.homeDateVisible] = backupJson.optBoolean("homeDateVisible", true)
            preferences[LauncherPreferenceKeys.homeUse24HourTime] = backupJson.optBoolean("homeUse24HourTime", true)
            preferences[LauncherPreferenceKeys.homeReorderHintDismissed] = backupJson.optBoolean("homeReorderHintDismissed", false)
        }
    }

    private fun setOrRemove(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        key: androidx.datastore.preferences.core.Preferences.Key<String>,
        value: String,
    ) {
        if (value.isBlank()) {
            preferences.remove(key)
        } else {
            preferences[key] = value
        }
    }
}
