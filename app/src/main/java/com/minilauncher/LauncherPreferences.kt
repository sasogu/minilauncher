package com.minilauncher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
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
