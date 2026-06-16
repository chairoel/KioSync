package com.mascill.kiosync.di

import android.content.Context
import com.mascill.kiosync.data.repository.KioSyncRepository
import com.mascill.kiosync.data.repository.KioSyncRepositoryImpl

object KioSyncDependencies {

    @Volatile
    private var repository: KioSyncRepository? = null

    fun repository(context: Context): KioSyncRepository {
        return repository ?: synchronized(this) {
            repository ?: KioSyncRepositoryImpl(context.applicationContext)
                .also { repository = it }
        }
    }
}
