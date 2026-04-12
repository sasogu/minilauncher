package com.minilauncher

import android.content.Context
import androidx.datastore.dataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LegacyMigrationInstrumentedTest {

    @Before
    fun cleanPersistentState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.dataStoreFile("launcher_preferences").delete()
    }

    @Test
    fun migrates_language_from_legacy_shared_preferences() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("language_tag", "en")
            .commit()

        val language = runBlocking {
            LanguageStore(context.launcherDataStore).loadLanguage()
        }

        assertEquals(AppLanguage.ENGLISH, language)
    }
}
