package com.mascill.kiosync.di

import android.content.Context
import com.mascill.kiosync.core.data.datasource.LaunchableAppDataSource
import com.mascill.kiosync.core.data.datastore.KioskPreferencesDataSource
import com.mascill.kiosync.core.data.repository.KioskRepository
import com.mascill.kiosync.core.data.repository.KioskRepositoryImpl
import com.mascill.kiosync.core.system.ElapsedRealtimeClock
import com.mascill.kiosync.core.system.SystemElapsedRealtimeClock
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskStartPlanner
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskViewModelFactory

/**
 * Lightweight manual dependency graph for the app.
 *
 * The providers cache singleton-like dependencies that are safe to reuse across activity
 * recreation while still keeping construction easy to follow in a small project.
 */
object Injection {

    @Volatile
    private var preferencesDataSource: KioskPreferencesDataSource? = null

    @Volatile
    private var launchableAppDataSource: LaunchableAppDataSource? = null

    @Volatile
    private var repository: KioskRepository? = null

    @Volatile
    private var kioskViewModelFactory: KioskViewModelFactory? = null

    /** Provides the factory used by MainActivity's viewModels delegate. */
    fun provideKioskViewModelFactory(context: Context): KioskViewModelFactory {
        return kioskViewModelFactory ?: synchronized(this) {
            val appContext = context.applicationContext
            kioskViewModelFactory ?: KioskViewModelFactory(
                kioskRepository = provideKioskRepository(appContext),
                appPackageName = provideAppPackageName(appContext),
                kioskStartPlanner = provideKioskStartPlanner()
            )
                .also { kioskViewModelFactory = it }
        }
    }

    /** Provides the repository that combines settings and installed app discovery. */
    fun provideKioskRepository(context: Context): KioskRepository {
        return repository ?: synchronized(this) {
            val appContext = context.applicationContext
            repository ?: KioskRepositoryImpl(
                preferencesDataSource = provideKioskPreferencesDataSource(appContext),
                launchableAppDataSource = provideLaunchableAppDataSource(appContext)
            )
                .also { repository = it }
        }
    }

    /** Returns this app package name for Lock Task allowlisting. */
    fun provideAppPackageName(context: Context): String {
        return context.applicationContext.packageName
    }

    /** Provides the elapsed realtime source used by kiosk startup planning. */
    fun provideElapsedRealtimeClock(): ElapsedRealtimeClock {
        return SystemElapsedRealtimeClock
    }

    /** Provides startup planning with production clock dependencies. */
    fun provideKioskStartPlanner(): KioskStartPlanner {
        return KioskStartPlanner(
            elapsedRealtimeClock = provideElapsedRealtimeClock()
        )
    }

    /** Provides persisted settings access. */
    private fun provideKioskPreferencesDataSource(context: Context): KioskPreferencesDataSource {
        return preferencesDataSource ?: synchronized(this) {
            preferencesDataSource ?: KioskPreferencesDataSource(context.applicationContext)
                .also { preferencesDataSource = it }
        }
    }

    /** Provides installed launcher app discovery. */
    private fun provideLaunchableAppDataSource(context: Context): LaunchableAppDataSource {
        return launchableAppDataSource ?: synchronized(this) {
            launchableAppDataSource ?: LaunchableAppDataSource(context.applicationContext)
                .also { launchableAppDataSource = it }
        }
    }
}
