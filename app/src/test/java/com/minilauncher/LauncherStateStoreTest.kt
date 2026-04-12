package com.minilauncher

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherStateStoreTest {

    @Test
    fun onQueryChange_filters_visible_apps_ignoring_hidden_packages() {
        val stateStore = createStateStore(
            loadedApps = listOf(
                LaunchableApp(label = "Mail", packageName = "org.example.mail"),
                LaunchableApp(label = "Maps", packageName = "org.example.maps"),
            ),
        )

        val state = LauncherUiState(
            allApps = listOf(
                LaunchableApp(label = "Mail", packageName = "org.example.mail"),
                LaunchableApp(label = "Maps", packageName = "org.example.maps"),
            ),
            hiddenPackages = listOf("org.example.maps"),
        )

        val result = stateStore.onQueryChange(state, "map")

        assertTrue(result.filteredApps.isEmpty())
    }

    @Test
    fun hideApp_updates_hidden_lists_and_last_hidden_app() = runTest {
        val stateStore = createStateStore(
            loadedApps = listOf(
                LaunchableApp(label = "Mail", packageName = "org.example.mail"),
                LaunchableApp(label = "Maps", packageName = "org.example.maps"),
            ),
        )

        val initial = LauncherUiState(
            allApps = listOf(
                LaunchableApp(label = "Mail", packageName = "org.example.mail"),
                LaunchableApp(label = "Maps", packageName = "org.example.maps"),
            ),
            filteredApps = listOf(
                LaunchableApp(label = "Mail", packageName = "org.example.mail"),
                LaunchableApp(label = "Maps", packageName = "org.example.maps"),
            ),
        )

        val hidden = stateStore.hideApp(initial, LaunchableApp(label = "Maps", packageName = "org.example.maps"))

        assertEquals(listOf("org.example.maps"), hidden.hiddenPackages)
        assertEquals(listOf("Maps"), hidden.hiddenApps.map { it.label })
        assertEquals(listOf("Mail"), hidden.filteredApps.map { it.label })
        assertEquals("org.example.maps", hidden.lastHiddenApp?.packageName)
    }

    @Test
    fun clearLastHiddenApp_removes_last_hidden_reference() = runTest {
        val stateStore = createStateStore(loadedApps = emptyList())
        val state = LauncherUiState(lastHiddenApp = LaunchableApp("Maps", "org.example.maps"))

        val cleared = stateStore.clearLastHiddenApp(state)

        assertNull(cleared.lastHiddenApp)
    }

    private fun createStateStore(loadedApps: List<LaunchableApp>): LauncherStateStore {
        val appsDataSource = object : LaunchableAppsDataSource {
            override fun loadLaunchableApps(): List<LaunchableApp> = loadedApps
        }
        return LauncherStateStore(
            appsRepository = appsDataSource,
            favoritesStore = FavoritesStore(createTestPreferencesDataStore("state-favorites")),
            hiddenAppsStore = HiddenAppsStore(createTestPreferencesDataStore("state-hidden")),
        )
    }
}
