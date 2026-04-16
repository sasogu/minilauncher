package es.sasogu.minilauncher

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.runBlocking
import java.util.Locale

enum class AppLanguage(
    val localeTag: String,
    val shortLabel: String,
) {
    SPANISH("es", "ES"),
    VALENCIAN("ca", "VAL"),
    ENGLISH("en", "EN");

    fun next(): AppLanguage {
        val all = entries
        return all[(ordinal + 1) % all.size]
    }

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            return entries.firstOrNull { it.localeTag == tag } ?: SPANISH
        }
    }
}

class LanguageStore(
    private val dataStore: DataStore<Preferences>,
) {
    suspend fun loadLanguage(): AppLanguage {
        val tag = dataStore.readString(LauncherPreferenceKeys.languageTag, AppLanguage.SPANISH.localeTag)
        return AppLanguage.fromTag(tag)
    }

    fun loadLanguageBlocking(): AppLanguage = runBlocking {
        loadLanguage()
    }

    suspend fun saveLanguage(language: AppLanguage) {
        dataStore.writeString(LauncherPreferenceKeys.languageTag, language.localeTag)
    }
}

fun Context.withAppLanguage(language: AppLanguage): Context {
    val locale = Locale.forLanguageTag(language.localeTag)
    Locale.setDefault(locale)

    val config = resources.configuration
    config.setLocale(locale)
    return createConfigurationContext(config)
}
