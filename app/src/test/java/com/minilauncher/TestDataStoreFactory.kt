package es.sasogu.minilauncher

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
fun createTestPreferencesDataStore(name: String = "test"): DataStore<Preferences> {
    val directory = Files.createTempDirectory("minilauncher-$name").toFile()
    directory.deleteOnExit()
    return PreferenceDataStoreFactory.create(
        scope = TestScope(UnconfinedTestDispatcher()),
        produceFile = { File(directory, "$name.preferences_pb") },
    )
}
