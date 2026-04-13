package com.minilauncher

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LauncherUiAction {
    data class QueryChanged(val query: String) : LauncherUiAction
    data class HomeQueryChanged(val query: String) : LauncherUiAction
    data class ToggleFavorite(val app: LaunchableApp) : LauncherUiAction
    data class PromoteFavorite(val app: LaunchableApp) : LauncherUiAction
    data class HideApp(val app: LaunchableApp) : LauncherUiAction
    data class RestoreHiddenApp(val app: LaunchableApp) : LauncherUiAction
    data object RestoreAllHiddenApps : LauncherUiAction
    data object HiddenAppNoticeConsumed : LauncherUiAction
    data class LanguageChanged(val language: AppLanguage) : LauncherUiAction
    data class ThemeChanged(val themeMode: ThemeMode) : LauncherUiAction
    data class UsagePromptToggled(val enabled: Boolean) : LauncherUiAction
    data object DismissHomeReorderHint : LauncherUiAction
    data object OpenWebSearch : LauncherUiAction
    data object DismissWebSearch : LauncherUiAction
    data class PendingLaunchChanged(val app: LaunchableApp?) : LauncherUiAction
    data class TimeoutNoticeChanged(val notice: TimeoutNotice?) : LauncherUiAction
    data class TransientMessageChanged(val message: String?) : LauncherUiAction
    data object RefreshApps : LauncherUiAction
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val appsRepository = AppsRepository(application.packageManager)
    private val favoritesStore = FavoritesStore(application.launcherDataStore)
    private val hiddenAppsStore = HiddenAppsStore(application.launcherDataStore)
    private val launcherStateStore = LauncherStateStore(appsRepository, favoritesStore, hiddenAppsStore)
    private val languageStore = LanguageStore(application.launcherDataStore)
    private val themeStore = ThemeStore(application.launcherDataStore)
    private val usagePromptStore = UsagePromptStore(application.launcherDataStore)
    private val homeHintsStore = HomeHintsStore(application.launcherDataStore)
    private var hasCompletedInitialAppsLoad = false

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val language = languageStore.loadLanguage()
            val themeMode = themeStore.loadThemeMode()
            val usagePromptEnabled = usagePromptStore.load()
            val showHomeReorderHint = homeHintsStore.isHomeReorderHintVisible()
            _uiState.value = _uiState.value.copy(
                selectedLanguage = language,
                selectedThemeMode = themeMode,
                usagePromptEnabled = usagePromptEnabled,
                showHomeReorderHint = showHomeReorderHint,
            )
        }
        dispatch(LauncherUiAction.RefreshApps)
    }

    fun dispatch(action: LauncherUiAction) {
        when (action) {
            is LauncherUiAction.QueryChanged -> {
                _uiState.value = launcherStateStore.onQueryChange(_uiState.value, action.query)
            }

            is LauncherUiAction.HomeQueryChanged -> {
                _uiState.value = launcherStateStore.onHomeQueryChange(_uiState.value, action.query)
            }

            is LauncherUiAction.ToggleFavorite -> {
                viewModelScope.launch {
                    _uiState.value = launcherStateStore.toggleFavorite(_uiState.value, action.app)
                }
            }

            is LauncherUiAction.PromoteFavorite -> {
                viewModelScope.launch {
                    _uiState.value = launcherStateStore.promoteFavorite(_uiState.value, action.app)
                    if (_uiState.value.showHomeReorderHint) {
                        homeHintsStore.dismissHomeReorderHint()
                        _uiState.value = _uiState.value.copy(showHomeReorderHint = false)
                    }
                }
            }

            is LauncherUiAction.HideApp -> {
                viewModelScope.launch {
                    _uiState.value = launcherStateStore.hideApp(_uiState.value, action.app)
                }
            }

            is LauncherUiAction.RestoreHiddenApp -> {
                viewModelScope.launch {
                    _uiState.value = launcherStateStore.restoreHiddenApp(_uiState.value, action.app)
                }
            }

            LauncherUiAction.RestoreAllHiddenApps -> {
                viewModelScope.launch {
                    _uiState.value = launcherStateStore.restoreAllHiddenApps(_uiState.value)
                }
            }

            LauncherUiAction.HiddenAppNoticeConsumed -> {
                _uiState.value = launcherStateStore.clearLastHiddenApp(_uiState.value)
            }

            is LauncherUiAction.LanguageChanged -> {
                val currentLanguage = _uiState.value.selectedLanguage
                if (currentLanguage == action.language) return
                viewModelScope.launch {
                    languageStore.saveLanguage(action.language)
                    _uiState.value = _uiState.value.copy(selectedLanguage = action.language)
                }
            }

            is LauncherUiAction.ThemeChanged -> {
                val currentTheme = _uiState.value.selectedThemeMode
                if (currentTheme == action.themeMode) return
                viewModelScope.launch {
                    themeStore.saveThemeMode(action.themeMode)
                    _uiState.value = _uiState.value.copy(selectedThemeMode = action.themeMode)
                }
            }

            is LauncherUiAction.UsagePromptToggled -> {
                viewModelScope.launch {
                    usagePromptStore.save(action.enabled)
                    _uiState.value = _uiState.value.copy(usagePromptEnabled = action.enabled)
                }
            }

            LauncherUiAction.DismissHomeReorderHint -> {
                if (!_uiState.value.showHomeReorderHint) return
                viewModelScope.launch {
                    homeHintsStore.dismissHomeReorderHint()
                    _uiState.value = _uiState.value.copy(showHomeReorderHint = false)
                }
            }

            LauncherUiAction.OpenWebSearch -> {
                _uiState.value = _uiState.value.copy(showWebSearch = true)
            }

            LauncherUiAction.DismissWebSearch -> {
                _uiState.value = _uiState.value.copy(showWebSearch = false)
            }

            is LauncherUiAction.PendingLaunchChanged -> {
                _uiState.value = _uiState.value.copy(pendingLaunchApp = action.app)
            }

            is LauncherUiAction.TimeoutNoticeChanged -> {
                _uiState.value = _uiState.value.copy(timeoutNotice = action.notice)
            }

            is LauncherUiAction.TransientMessageChanged -> {
                _uiState.value = _uiState.value.copy(transientMessage = action.message)
            }

            LauncherUiAction.RefreshApps -> {
                viewModelScope.launch(Dispatchers.IO) {
                    val startMs = SystemClock.elapsedRealtime()
                    launcherStateStore.loadApps(_uiState)
                    val visibleApps = _uiState.value.filteredApps.ifEmpty { _uiState.value.allApps }
                    (appsRepository as? LaunchableAppsIconPreloader)?.prewarmIcons(
                        apps = visibleApps,
                        maxIcons = VISIBLE_ICON_PREWARM_LIMIT,
                    )

                    val elapsedMs = SystemClock.elapsedRealtime() - startMs
                    val stateNow = _uiState.value
                    val isInitial = !hasCompletedInitialAppsLoad
                    val refreshedState = if (isInitial) {
                        stateNow.copy(
                            firstLoadDurationMs = elapsedMs,
                            appsRefreshCount = stateNow.appsRefreshCount + 1,
                        )
                    } else {
                        stateNow.copy(
                            lastReloadDurationMs = elapsedMs,
                            appsRefreshCount = stateNow.appsRefreshCount + 1,
                        )
                    }
                    _uiState.value = refreshedState
                    hasCompletedInitialAppsLoad = true

                    val phase = if (isInitial) "initial" else "reload"
                    Log.d(LOG_TAG, "Apps load ($phase) took ${elapsedMs}ms, refreshCount=${refreshedState.appsRefreshCount}")
                }
            }
        }
    }

    private companion object {
        const val LOG_TAG = "LauncherPerformance"
        const val VISIBLE_ICON_PREWARM_LIMIT = 24
    }
}
