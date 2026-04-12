package com.minilauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import androidx.collection.LruCache
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

data class LauncherUiState(
    val query: String = "",
    val homeQuery: String = "",
    val allApps: List<LaunchableApp> = emptyList(),
    val filteredApps: List<LaunchableApp> = emptyList(),
    val favoritePackages: List<String> = emptyList(),
    val pendingLaunchApp: LaunchableApp? = null,
    val timeoutNotice: TimeoutNotice? = null,
    val selectedLanguage: AppLanguage = AppLanguage.SPANISH,
    val selectedThemeMode: ThemeMode = ThemeMode.DARK,
)

data class TimeoutNotice(
    val appLabel: String,
    val minutes: Int,
)

data class LaunchableApp(
    val label: String,
    val packageName: String,
)

class LauncherStateStore(
    private val appsRepository: AppsRepository,
    private val favoritesStore: FavoritesStore,
) {
    fun onQueryChange(state: LauncherUiState, query: String): LauncherUiState {
        return state.copy(
            query = query,
            filteredApps = filterApps(state.allApps, query),
        )
    }

    fun onHomeQueryChange(state: LauncherUiState, query: String): LauncherUiState {
        return state.copy(homeQuery = query)
    }

    suspend fun toggleFavorite(state: LauncherUiState, app: LaunchableApp): LauncherUiState {
        return state.copy(favoritePackages = favoritesStore.toggle(app.packageName))
    }

    suspend fun promoteFavorite(state: LauncherUiState, app: LaunchableApp): LauncherUiState {
        return state.copy(favoritePackages = favoritesStore.promote(app.packageName))
    }

    suspend fun loadApps(
        stateFlow: MutableStateFlow<LauncherUiState>,
    ) {
        val apps = appsRepository.loadLaunchableApps()
        val favorites = favoritesStore.loadFavorites()

        apps.chunked(32)
            .runningFold(emptyList<LaunchableApp>()) { acc, chunk -> acc + chunk }
            .drop(1)
            .forEach { partialApps ->
                val stateNow = stateFlow.value
                val activeQuery = stateNow.query
                stateFlow.value = stateNow.copy(
                    allApps = partialApps,
                    filteredApps = filterApps(partialApps, activeQuery),
                    favoritePackages = favorites,
                )
            }

        val stateNow = stateFlow.value
        stateFlow.value = stateNow.copy(
            allApps = apps,
            filteredApps = filterApps(apps, stateNow.query),
            favoritePackages = favorites,
        )
    }
}

class AppsRepository(
    private val packageManager: PackageManager,
) {
    fun loadLaunchableApps(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        val apps: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)

        return apps
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                if (label.isBlank()) return@mapNotNull null

                LaunchableApp(
                    label = label,
                    packageName = packageName,
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { normalize(it.label) }
    }
}

object AppIconCache {
    private val cache = LruCache<String, Bitmap>(200)

    fun get(packageName: String): Bitmap? = cache.get(packageName)

    fun put(packageName: String, bitmap: Bitmap) {
        cache.put(packageName, bitmap)
    }
}

class FavoritesStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun loadFavorites(): List<String> {
        val ordered = dataStore.safeData()
            .map { preferences -> preferences[LauncherPreferenceKeys.favoritePackagesOrder] }
            .first()
            ?.split(FAVORITES_SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
        if (!ordered.isNullOrEmpty()) return ordered

        return emptyList()
    }

    suspend fun toggle(packageName: String): List<String> {
        val current = loadFavorites().toMutableList()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(0, packageName)
        }
        return save(current)
    }

    suspend fun promote(packageName: String): List<String> {
        val current = loadFavorites().toMutableList()
        val index = current.indexOf(packageName)
        if (index <= 0) return current
        current.removeAt(index)
        current.add(0, packageName)
        return save(current)
    }

    private suspend fun save(values: List<String>): List<String> {
        val cleaned = values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        dataStore.writeString(
            LauncherPreferenceKeys.favoritePackagesOrder,
            cleaned.joinToString(FAVORITES_SEPARATOR),
        )
        return cleaned
    }

    private companion object {
        const val FAVORITES_SEPARATOR = "|"
    }
}

fun filterApps(
    apps: List<LaunchableApp>,
    query: String,
): List<LaunchableApp> {
    if (query.isBlank()) return apps
    val normalizedQuery = normalize(query)
    return apps.filter { app ->
        normalize(app.label).contains(normalizedQuery) ||
            app.packageName.lowercase(Locale.ROOT).contains(normalizedQuery)
    }
}

fun normalize(value: String): String {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase(Locale.ROOT)
}
