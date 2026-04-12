package com.minilauncher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private const val LEGACY_SHARED_PREFS = "launcher_prefs"
private const val DATASTORE_NAME = "launcher_preferences"

val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_SHARED_PREFS))
    },
)

object LauncherPreferenceKeys {
    val languageTag = stringPreferencesKey("language_tag")
    val favoritePackagesOrder = stringPreferencesKey("favorite_packages_order")
    val hiddenPackages = stringPreferencesKey("hidden_packages")
    val homeReorderHintDismissed = booleanPreferencesKey("home_reorder_hint_dismissed")
    val themeMode = stringPreferencesKey("theme_mode")
    val usagePromptEnabled = booleanPreferencesKey("usage_prompt_enabled")
}

fun DataStore<Preferences>.safeData(): Flow<Preferences> {
    return data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
}

suspend fun DataStore<Preferences>.readString(key: Preferences.Key<String>, defaultValue: String): String {
    return safeData().map { preferences ->
        preferences[key] ?: defaultValue
    }.first()
}

suspend fun DataStore<Preferences>.writeString(key: Preferences.Key<String>, value: String) {
    edit { preferences ->
        preferences[key] = value
    }
}

suspend fun DataStore<Preferences>.readBoolean(key: Preferences.Key<Boolean>, defaultValue: Boolean): Boolean {
    return safeData().map { preferences ->
        preferences[key] ?: defaultValue
    }.first()
}

suspend fun DataStore<Preferences>.writeBoolean(key: Preferences.Key<Boolean>, value: Boolean) {
    edit { preferences ->
        preferences[key] = value
    }
}

class UsagePromptStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun load(): Boolean {
        return dataStore.readBoolean(LauncherPreferenceKeys.usagePromptEnabled, false)
    }

    fun loadBlocking(): Boolean = runBlocking { load() }

    suspend fun save(enabled: Boolean) {
        dataStore.writeBoolean(LauncherPreferenceKeys.usagePromptEnabled, enabled)
    }
}

class HiddenAppsStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun loadHiddenPackages(): List<String> {
        return dataStore.safeData()
            .map { preferences -> preferences[LauncherPreferenceKeys.hiddenPackages] }
            .first()
            .orEmpty()
            .split(HIDDEN_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    suspend fun hide(packageName: String): List<String> {
        val current = loadHiddenPackages().toMutableList()
        if (!current.contains(packageName)) {
            current.add(packageName)
        }
        return save(current)
    }

    suspend fun restore(packageName: String): List<String> {
        val current = loadHiddenPackages().toMutableList()
        current.remove(packageName)
        return save(current)
    }

    suspend fun restoreAll(): List<String> = save(emptyList())

    private suspend fun save(values: List<String>): List<String> {
        val cleaned = values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        dataStore.writeString(
            LauncherPreferenceKeys.hiddenPackages,
            cleaned.joinToString(HIDDEN_SEPARATOR),
        )
        return cleaned
    }

    private companion object {
        const val HIDDEN_SEPARATOR = "|"
    }
}

class HomeHintsStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun isHomeReorderHintVisible(): Boolean {
        val dismissed = dataStore.readBoolean(LauncherPreferenceKeys.homeReorderHintDismissed, false)
        return !dismissed
    }

    suspend fun dismissHomeReorderHint() {
        dataStore.writeBoolean(LauncherPreferenceKeys.homeReorderHintDismissed, true)
    }
}
