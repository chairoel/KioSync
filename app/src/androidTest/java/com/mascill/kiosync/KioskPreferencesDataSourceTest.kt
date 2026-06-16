package com.mascill.kiosync

import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.mascill.kiosync.core.data.datastore.KioskPreferencesDataSource
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KioskPreferencesDataSourceTest {

    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        appContext.deleteSharedPreferences(KioskPreferencesDataSource.PREF_NAME)
        File(appContext.filesDir, "datastore/${KioskPreferencesDataSource.PREF_NAME}.preferences_pb")
            .delete()
    }

    @Test
    fun useAppContext() {
        assertEquals("com.mascill.kiosync", appContext.packageName)
    }

    @Test
    fun sharedPreferencesMigration_preservesKioskSettings() = runBlocking {
        appContext.getSharedPreferences(KioskPreferencesDataSource.PREF_NAME, 0)
            .edit {
                putBoolean("kiosk_enabled", true)
                putStringSet("allowed_app_packages", setOf("com.example.allowed"))
            }

        val dataSource = KioskPreferencesDataSource(appContext)

        assertTrue(dataSource.kioskEnabled.first())
        assertEquals(setOf("com.example.allowed"), dataSource.allowedPackages.first())
    }
}
