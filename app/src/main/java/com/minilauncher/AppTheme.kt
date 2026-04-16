package com.minilauncher

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.runBlocking

enum class ThemeMode(
    val storageValue: String,
    val shortLabel: String,
) {
    DARK("dark", "Dark"),
    LIGHT("light", "Light");

    companion object {
        fun fromStorage(value: String?): ThemeMode {
            return entries.firstOrNull { it.storageValue == value } ?: DARK
        }
    }
}

data class LauncherPalette(
    val background: androidx.compose.ui.graphics.Color,
    val surface: androidx.compose.ui.graphics.Color,
    val surfaceAlt: androidx.compose.ui.graphics.Color,
    val textPrimary: androidx.compose.ui.graphics.Color,
    val textSecondary: androidx.compose.ui.graphics.Color,
    val textMuted: androidx.compose.ui.graphics.Color,
    val iconMuted: androidx.compose.ui.graphics.Color,
    val inputBackground: androidx.compose.ui.graphics.Color,
    val inputBorderFocused: androidx.compose.ui.graphics.Color,
    val inputBorderUnfocused: androidx.compose.ui.graphics.Color,
    val batteryCharging: androidx.compose.ui.graphics.Color,
)

private val DarkPalette = LauncherPalette(
    background = androidx.compose.ui.graphics.Color(0xFF000000),
    surface = androidx.compose.ui.graphics.Color(0xFF151515),
    surfaceAlt = androidx.compose.ui.graphics.Color(0xFF101010),
    textPrimary = androidx.compose.ui.graphics.Color.White,
    textSecondary = androidx.compose.ui.graphics.Color(0xFFD0D0D0),
    textMuted = androidx.compose.ui.graphics.Color(0xFFB0B0B0),
    iconMuted = androidx.compose.ui.graphics.Color(0xFFBDBDBD),
    inputBackground = androidx.compose.ui.graphics.Color(0xFF101010),
    inputBorderFocused = androidx.compose.ui.graphics.Color(0xFFBDBDBD),
    inputBorderUnfocused = androidx.compose.ui.graphics.Color(0xFF4A4A4A),
    batteryCharging = androidx.compose.ui.graphics.Color(0xFF4ADE80),
)

private val LightPalette = LauncherPalette(
    background = androidx.compose.ui.graphics.Color(0xFFF3F4F6),
    surface = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    surfaceAlt = androidx.compose.ui.graphics.Color(0xFFF9FAFB),
    textPrimary = androidx.compose.ui.graphics.Color(0xFF111827),
    textSecondary = androidx.compose.ui.graphics.Color(0xFF374151),
    textMuted = androidx.compose.ui.graphics.Color(0xFF4B5563),
    iconMuted = androidx.compose.ui.graphics.Color(0xFF374151),
    inputBackground = androidx.compose.ui.graphics.Color(0xFFFFFFFF),
    inputBorderFocused = androidx.compose.ui.graphics.Color(0xFF374151),
    inputBorderUnfocused = androidx.compose.ui.graphics.Color(0xFFD1D5DB),
    batteryCharging = androidx.compose.ui.graphics.Color(0xFF16A34A),
)

private val LocalLauncherPalette = staticCompositionLocalOf { DarkPalette }

class ThemeStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun loadThemeMode(): ThemeMode {
        val value = dataStore.readString(LauncherPreferenceKeys.themeMode, ThemeMode.DARK.storageValue)
        return ThemeMode.fromStorage(value)
    }

    fun loadThemeModeBlocking(): ThemeMode = runBlocking {
        loadThemeMode()
    }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        dataStore.writeString(LauncherPreferenceKeys.themeMode, themeMode.storageValue)
    }
}

@Composable
fun MinimalLauncherTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val palette = when (themeMode) {
        ThemeMode.DARK -> DarkPalette
        ThemeMode.LIGHT -> LightPalette
    }

    CompositionLocalProvider(LocalLauncherPalette provides palette) {
        MaterialTheme(content = content)
    }
}

@Composable
fun launcherPalette(): LauncherPalette = LocalLauncherPalette.current

fun ThemeMode.fromStorageLabel(context: Context): String {
    return when (this) {
        ThemeMode.DARK -> context.getString(R.string.theme_dark)
        ThemeMode.LIGHT -> context.getString(R.string.theme_light)
    }
}
