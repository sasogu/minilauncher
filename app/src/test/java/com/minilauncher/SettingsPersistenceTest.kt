package com.minilauncher

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsPersistenceTest {

    @Test
    fun favoritesStore_toggle_adds_and_removes_favorites() {
        val preferences = FakeSharedPreferences()
        val store = FavoritesStore(preferences)

        assertEquals(listOf("org.example.mail"), store.toggle("org.example.mail"))
        assertEquals(emptyList<String>(), store.toggle("org.example.mail"))
    }

    @Test
    fun favoritesStore_promote_moves_existing_favorite_to_front() {
        val preferences = FakeSharedPreferences()
        val store = FavoritesStore(preferences)

        store.toggle("org.example.one")
        store.toggle("org.example.two")
        store.toggle("org.example.three")

        val reordered = store.promote("org.example.one")

        assertEquals(
            listOf("org.example.one", "org.example.three", "org.example.two"),
            reordered,
        )
    }

    @Test
    fun favoritesStore_loadFavorites_preserves_saved_order() {
        val preferences = FakeSharedPreferences()
        val store = FavoritesStore(preferences)

        store.toggle("org.example.one")
        store.toggle("org.example.two")

        val reloaded = FavoritesStore(preferences).loadFavorites()

        assertEquals(listOf("org.example.two", "org.example.one"), reloaded)
    }

    @Test
    fun languageStore_persists_selected_language() {
        val preferences = FakeSharedPreferences()
        val store = LanguageStore(preferences)

        store.saveLanguage(AppLanguage.VALENCIAN)

        assertEquals(AppLanguage.VALENCIAN, store.loadLanguage())
    }

    @Test
    fun languageStore_defaults_to_spanish_for_unknown_value() {
        val preferences = FakeSharedPreferences()
        preferences.edit().putString("language_tag", "xx").apply()

        val store = LanguageStore(preferences)

        assertEquals(AppLanguage.SPANISH, store.loadLanguage())
    }
}