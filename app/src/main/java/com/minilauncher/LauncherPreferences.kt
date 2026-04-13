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
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
    val appTags = stringPreferencesKey("app_tags")
    val homeReorderHintDismissed = booleanPreferencesKey("home_reorder_hint_dismissed")
    val themeMode = stringPreferencesKey("theme_mode")
    val usagePromptEnabled = booleanPreferencesKey("usage_prompt_enabled")
    val moonIlluminationPercentageVisible = booleanPreferencesKey("moon_illumination_percentage_visible")
    val homeWeekdayVisible = booleanPreferencesKey("home_weekday_visible")
    val homeDateVisible = booleanPreferencesKey("home_date_visible")
    val homeUse24HourTime = booleanPreferencesKey("home_use_24_hour_time")
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

class MoonIlluminationStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun load(): Boolean {
        return dataStore.readBoolean(LauncherPreferenceKeys.moonIlluminationPercentageVisible, true)
    }

    fun loadBlocking(): Boolean = runBlocking { load() }

    suspend fun save(visible: Boolean) {
        dataStore.writeBoolean(LauncherPreferenceKeys.moonIlluminationPercentageVisible, visible)
    }
}

class HomeHeaderDateStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun loadShowWeekday(): Boolean {
        return dataStore.readBoolean(LauncherPreferenceKeys.homeWeekdayVisible, true)
    }

    suspend fun loadShowDate(): Boolean {
        return dataStore.readBoolean(LauncherPreferenceKeys.homeDateVisible, true)
    }

    suspend fun loadUse24HourTime(): Boolean {
        return dataStore.readBoolean(LauncherPreferenceKeys.homeUse24HourTime, true)
    }

    suspend fun saveShowWeekday(visible: Boolean) {
        dataStore.writeBoolean(LauncherPreferenceKeys.homeWeekdayVisible, visible)
    }

    suspend fun saveShowDate(visible: Boolean) {
        dataStore.writeBoolean(LauncherPreferenceKeys.homeDateVisible, visible)
    }

    suspend fun saveUse24HourTime(enabled: Boolean) {
        dataStore.writeBoolean(LauncherPreferenceKeys.homeUse24HourTime, enabled)
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

class AppTagsStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun loadAllTags(): Map<String, List<String>> {
        val raw = dataStore.safeData()
            .map { preferences -> preferences[LauncherPreferenceKeys.appTags] }
            .first()
            .orEmpty()
        if (raw.isBlank()) return emptyMap()

        return raw.lineSequence()
            .mapNotNull { line ->
                val packageAndTags = line.split(PACKAGE_TAGS_SEPARATOR, limit = 2)
                if (packageAndTags.size != 2) return@mapNotNull null

                val packageName = decode(packageAndTags[0]).trim()
                if (packageName.isBlank()) return@mapNotNull null

                val tags = packageAndTags[1]
                    .split(TAG_SEPARATOR)
                    .mapNotNull { decode(it).trim().takeIf(String::isNotBlank) }
                    .distinctBy { normalize(it) }

                packageName to tags
            }
            .filter { (_, tags) -> tags.isNotEmpty() }
            .toMap()
    }

    suspend fun saveTags(packageName: String, tags: List<String>): Map<String, List<String>> {
        val current = loadAllTags().toMutableMap()
        val cleanedTags = cleanTags(tags)
        if (cleanedTags.isEmpty()) {
            current.remove(packageName)
        } else {
            current[packageName] = cleanedTags
        }
        dataStore.writeString(LauncherPreferenceKeys.appTags, serialize(current))
        return current.toMap()
    }

    companion object {
        fun cleanTags(tags: List<String>): List<String> {
            return tags
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinctBy { normalize(it) }
        }

        fun parseTagsInput(input: String): List<String> {
            return cleanTags(input.split(',', '\n'))
        }

        private const val PACKAGE_TAGS_SEPARATOR = "\t"
        private const val TAG_SEPARATOR = "|"

        private fun serialize(values: Map<String, List<String>>): String {
            return values.entries
                .sortedBy { it.key }
                .joinToString("\n") { (packageName, tags) ->
                    "${encode(packageName)}$PACKAGE_TAGS_SEPARATOR${tags.joinToString(TAG_SEPARATOR) { encode(it) }}"
                }
        }

        private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

        private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8)
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
