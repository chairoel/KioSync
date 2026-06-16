package com.mascill.kiosync.di

import android.content.Context
import com.mascill.kiosync.core.data.datastore.KioskPreferencesDataSource
import com.mascill.kiosync.core.data.repository.KioskRepository
import com.mascill.kiosync.core.data.repository.KioskRepositoryImpl
import com.mascill.kiosync.core.system.ElapsedRealtimeClock
import com.mascill.kiosync.core.system.SystemElapsedRealtimeClock
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskStartPlanner
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskViewModelFactory

object Injection {

    @Volatile
    private var preferencesDataSource: KioskPreferencesDataSource? = null

    @Volatile
    private var repository: KioskRepository? = null

    @Volatile
    private var kioskViewModelFactory: KioskViewModelFactory? = null

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

    fun provideKioskRepository(context: Context): KioskRepository {
        return repository ?: synchronized(this) {
            val appContext = context.applicationContext
            repository ?: KioskRepositoryImpl(
                context = appContext,
                preferencesDataSource = provideKioskPreferencesDataSource(appContext)
            )
                .also { repository = it }
        }
    }

    fun provideAppPackageName(context: Context): String {
        return context.applicationContext.packageName
    }

    fun provideElapsedRealtimeClock(): ElapsedRealtimeClock {
        return SystemElapsedRealtimeClock
    }

    fun provideKioskStartPlanner(): KioskStartPlanner {
        return KioskStartPlanner(
            elapsedRealtimeClock = provideElapsedRealtimeClock()
        )
    }

    private fun provideKioskPreferencesDataSource(context: Context): KioskPreferencesDataSource {
        return preferencesDataSource ?: synchronized(this) {
            preferencesDataSource ?: KioskPreferencesDataSource(context.applicationContext)
                .also { preferencesDataSource = it }
        }
    }
}
