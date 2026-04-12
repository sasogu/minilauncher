package com.minilauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import androidx.collection.LruCache
import androidx.core.graphics.drawable.toBitmap
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
    val hiddenPackages: List<String> = emptyList(),
    val hiddenApps: List<LaunchableApp> = emptyList(),
    val lastHiddenApp: LaunchableApp? = null,
    val pendingLaunchApp: LaunchableApp? = null,
    val timeoutNotice: TimeoutNotice? = null,
    val selectedLanguage: AppLanguage = AppLanguage.SPANISH,
    val selectedThemeMode: ThemeMode = ThemeMode.DARK,
    val usagePromptEnabled: Boolean = false,
    val showHomeReorderHint: Boolean = true,
    val showWebSearch: Boolean = false,
    val firstLoadDurationMs: Long? = null,
    val lastReloadDurationMs: Long? = null,
    val appsRefreshCount: Int = 0,
)

data class TimeoutNotice(
    val appLabel: String,
    val minutes: Int,
    val packageName: String? = null,
)

data class LaunchableApp(
    val label: String,
    val packageName: String,
)

interface LaunchableAppsDataSource {
    fun loadLaunchableApps(): List<LaunchableApp>
}

interface LaunchableAppsIconPreloader {
    suspend fun prewarmIcons(apps: List<LaunchableApp>, maxIcons: Int)
}

class LauncherStateStore(
    private val appsRepository: LaunchableAppsDataSource,
    private val favoritesStore: FavoritesStore,
    private val hiddenAppsStore: HiddenAppsStore,
) {
    fun onQueryChange(state: LauncherUiState, query: String): LauncherUiState {
        val visibleApps = visibleApps(state.allApps, state.hiddenPackages)
        return state.copy(
            query = query,
            filteredApps = filterApps(visibleApps, query),
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

    suspend fun hideApp(state: LauncherUiState, app: LaunchableApp): LauncherUiState {
        val hiddenPackages = hiddenAppsStore.hide(app.packageName)
        return state.afterHiddenPackagesUpdated(hiddenPackages).copy(lastHiddenApp = app)
    }

    suspend fun restoreHiddenApp(state: LauncherUiState, app: LaunchableApp): LauncherUiState {
        val hiddenPackages = hiddenAppsStore.restore(app.packageName)
        return state.afterHiddenPackagesUpdated(hiddenPackages)
    }

    suspend fun restoreAllHiddenApps(state: LauncherUiState): LauncherUiState {
        val hiddenPackages = hiddenAppsStore.restoreAll()
        return state.afterHiddenPackagesUpdated(hiddenPackages)
    }

    fun clearLastHiddenApp(state: LauncherUiState): LauncherUiState {
        if (state.lastHiddenApp == null) return state
        return state.copy(lastHiddenApp = null)
    }

    suspend fun loadApps(
        stateFlow: MutableStateFlow<LauncherUiState>,
    ) {
        val apps = appsRepository.loadLaunchableApps()
        val favorites = favoritesStore.loadFavorites()
        val hiddenPackages = hiddenAppsStore.loadHiddenPackages()

        apps.chunked(32)
            .runningFold(emptyList<LaunchableApp>()) { acc, chunk -> acc + chunk }
            .drop(1)
            .forEach { partialApps ->
                val stateNow = stateFlow.value
                val activeQuery = stateNow.query
                val visiblePartialApps = visibleApps(partialApps, hiddenPackages)
                stateFlow.value = stateNow.copy(
                    allApps = partialApps,
                    filteredApps = filterApps(visiblePartialApps, activeQuery),
                    favoritePackages = favorites,
                    hiddenPackages = hiddenPackages,
                    hiddenApps = hiddenApps(partialApps, hiddenPackages),
                )
            }

        val stateNow = stateFlow.value
        val visibleApps = visibleApps(apps, hiddenPackages)
        stateFlow.value = stateNow.copy(
            allApps = apps,
            filteredApps = filterApps(visibleApps, stateNow.query),
            favoritePackages = favorites,
            hiddenPackages = hiddenPackages,
            hiddenApps = hiddenApps(apps, hiddenPackages),
        )
    }

    private fun visibleApps(
        apps: List<LaunchableApp>,
        hiddenPackages: List<String>,
    ): List<LaunchableApp> {
        if (hiddenPackages.isEmpty()) return apps
        val hiddenSet = hiddenPackages.toSet()
        return apps.filterNot { app -> hiddenSet.contains(app.packageName) }
    }

    private fun hiddenApps(
        apps: List<LaunchableApp>,
        hiddenPackages: List<String>,
    ): List<LaunchableApp> {
        if (hiddenPackages.isEmpty()) return emptyList()
        val appsByPackage = apps.associateBy { it.packageName }
        return hiddenPackages.mapNotNull { packageName -> appsByPackage[packageName] }
    }

    private fun LauncherUiState.afterHiddenPackagesUpdated(
        hiddenPackages: List<String>,
    ): LauncherUiState {
        val visibleApps = visibleApps(allApps, hiddenPackages)
        return copy(
            hiddenPackages = hiddenPackages,
            hiddenApps = hiddenApps(allApps, hiddenPackages),
            filteredApps = filterApps(visibleApps, query),
            lastHiddenApp = null,
        )
    }
}

class AppsRepository(
    private val packageManager: PackageManager,
) : LaunchableAppsDataSource, LaunchableAppsIconPreloader {
    override fun loadLaunchableApps(): List<LaunchableApp> {
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

    override suspend fun prewarmIcons(apps: List<LaunchableApp>, maxIcons: Int) {
        val toWarm = apps.asSequence()
            .map { it.packageName }
            .distinct()
            .take(maxIcons)
            .toList()

        withContext(Dispatchers.IO) {
            toWarm.forEach { packageName ->
                if (AppIconCache.get(packageName) != null) return@forEach
                val bitmap = runCatching {
                    packageManager.getApplicationIcon(packageName).toBitmap()
                }.getOrNull() ?: return@forEach
                AppIconCache.put(packageName, bitmap)
            }
        }
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
