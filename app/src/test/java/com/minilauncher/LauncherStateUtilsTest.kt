package com.minilauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherStateUtilsTest {

    @Test
    fun normalize_removes_accents_and_lowercases() {
        assertEquals("camara", normalize("Cámara"))
        assertEquals("aplicacion util", normalize("Aplicación Útil"))
    }

    @Test
    fun filterApps_matches_by_label_ignoring_accents() {
        val apps = listOf(
            LaunchableApp(label = "Cámara", packageName = "com.android.camera"),
            LaunchableApp(label = "Firefox", packageName = "org.mozilla.firefox"),
        )

        val result = filterApps(apps, "camara")

        assertEquals(listOf("Cámara"), result.map { it.label })
    }

    @Test
    fun filterApps_matches_by_package_name() {
        val apps = listOf(
            LaunchableApp(label = "Mail", packageName = "org.example.mail"),
            LaunchableApp(label = "Maps", packageName = "org.example.maps"),
        )

        val result = filterApps(apps, "maps")

        assertEquals(listOf("Maps"), result.map { it.label })
    }

    @Test
    fun filterApps_matches_by_tag() {
        val apps = listOf(
            LaunchableApp(label = "Firefox", packageName = "org.mozilla.firefox", tags = listOf("internet", "trabajo")),
            LaunchableApp(label = "Signal", packageName = "org.signal", tags = listOf("mensajes")),
        )

        val result = filterApps(apps, "trabajo")

        assertEquals(listOf("Firefox"), result.map { it.label })
    }

    @Test
    fun filterApps_returns_all_when_query_blank() {
        val apps = listOf(
            LaunchableApp(label = "Mail", packageName = "org.example.mail"),
            LaunchableApp(label = "Maps", packageName = "org.example.maps"),
        )

        val result = filterApps(apps, "   ")

        assertEquals(apps, result)
        assertTrue(result.size == 2)
    }
}
