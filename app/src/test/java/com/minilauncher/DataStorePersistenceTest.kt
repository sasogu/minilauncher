package es.sasogu.minilauncher

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.coroutines.test.runTest

class DataStorePersistenceTest {

    @Test
    fun favoritesStore_toggle_adds_and_removes_favorites() = runTest {
        val dataStore = createTestPreferencesDataStore("favorites-toggle")
        val store = FavoritesStore(dataStore)

        assertEquals(listOf("org.example.mail"), store.toggle("org.example.mail"))
        assertEquals(emptyList<String>(), store.toggle("org.example.mail"))
    }

    @Test
    fun favoritesStore_promote_moves_existing_favorite_to_front() = runTest {
        val dataStore = createTestPreferencesDataStore("favorites-promote")
        val store = FavoritesStore(dataStore)

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
    fun favoritesStore_loadFavorites_preserves_saved_order() = runTest {
        val dataStore = createTestPreferencesDataStore("favorites-order")
        val store = FavoritesStore(dataStore)

        store.toggle("org.example.one")
        store.toggle("org.example.two")

        val reloaded = FavoritesStore(dataStore).loadFavorites()

        assertEquals(listOf("org.example.two", "org.example.one"), reloaded)
    }

    @Test
    fun languageStore_persists_selected_language() = runTest {
        val dataStore = createTestPreferencesDataStore("language-save")
        val store = LanguageStore(dataStore)

        store.saveLanguage(AppLanguage.VALENCIAN)

        assertEquals(AppLanguage.VALENCIAN, store.loadLanguage())
    }

    @Test
    fun languageStore_defaults_to_spanish_for_unknown_value() = runTest {
        val dataStore = createTestPreferencesDataStore("language-fallback")
        dataStore.writeString(LauncherPreferenceKeys.languageTag, "xx")

        val store = LanguageStore(dataStore)

        assertEquals(AppLanguage.SPANISH, store.loadLanguage())
    }

    @Test
    fun hiddenAppsStore_hide_and_restore_single_package() = runTest {
        val dataStore = createTestPreferencesDataStore("hidden-single")
        val store = HiddenAppsStore(dataStore)

        assertEquals(listOf("org.example.mail"), store.hide("org.example.mail"))
        assertEquals(emptyList<String>(), store.restore("org.example.mail"))
    }

    @Test
    fun hiddenAppsStore_loadHiddenPackages_preserves_saved_order() = runTest {
        val dataStore = createTestPreferencesDataStore("hidden-order")
        val store = HiddenAppsStore(dataStore)

        store.hide("org.example.one")
        store.hide("org.example.two")

        val reloaded = HiddenAppsStore(dataStore).loadHiddenPackages()

        assertEquals(listOf("org.example.one", "org.example.two"), reloaded)
    }

    @Test
    fun hiddenAppsStore_restoreAll_clears_all_packages() = runTest {
        val dataStore = createTestPreferencesDataStore("hidden-restore-all")
        val store = HiddenAppsStore(dataStore)

        store.hide("org.example.one")
        store.hide("org.example.two")

        assertEquals(emptyList<String>(), store.restoreAll())
    }
}
