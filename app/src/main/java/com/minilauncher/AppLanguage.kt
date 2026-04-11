package com.minilauncher

import android.content.Context
import android.content.SharedPreferences
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
    private val preferences: SharedPreferences,
) {
    fun loadLanguage(): AppLanguage {
        val tag = preferences.getString(LANGUAGE_KEY, AppLanguage.SPANISH.localeTag)
        return AppLanguage.fromTag(tag)
    }

    fun saveLanguage(language: AppLanguage) {
        preferences.edit().putString(LANGUAGE_KEY, language.localeTag).apply()
    }

    private companion object {
        const val LANGUAGE_KEY = "language_tag"
    }
}

fun Context.withAppLanguage(language: AppLanguage): Context {
    val locale = Locale.forLanguageTag(language.localeTag)
    Locale.setDefault(locale)

    val config = resources.configuration
    config.setLocale(locale)
    return createConfigurationContext(config)
}
