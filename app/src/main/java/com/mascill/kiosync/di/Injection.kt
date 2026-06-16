package com.mascill.kiosync.di

import android.content.Context
import com.mascill.kiosync.core.data.datastore.KioskPreferencesDataSource
import com.mascill.kiosync.core.data.repository.KioskRepository
import com.mascill.kiosync.core.data.repository.KioskRepositoryImpl
import com.mascill.kiosync.core.system.ElapsedRealtimeClock
import com.mascill.kiosync.core.system.SystemElapsedRealtimeClock

object Injection {

    @Volatile
    private var preferencesDataSource: KioskPreferencesDataSource? = null

    @Volatile
    private var repository: KioskRepository? = null

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

    private fun provideKioskPreferencesDataSource(context: Context): KioskPreferencesDataSource {
        return preferencesDataSource ?: synchronized(this) {
            preferencesDataSource ?: KioskPreferencesDataSource(context.applicationContext)
                .also { preferencesDataSource = it }
        }
    }
}
