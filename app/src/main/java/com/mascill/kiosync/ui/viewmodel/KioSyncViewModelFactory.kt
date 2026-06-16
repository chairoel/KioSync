package com.mascill.kiosync.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mascill.kiosync.data.repository.KioSyncRepository
import com.mascill.kiosync.di.Injection

@Suppress("UNCHECKED_CAST")
class KioSyncViewModelFactory(private val kioSyncRepository: KioSyncRepository) :
    ViewModelProvider.NewInstanceFactory() {
    companion object {
        @Volatile
        private var instance: KioSyncViewModelFactory? = null

        fun getInstance(context: Context): KioSyncViewModelFactory =
            instance ?: synchronized(this) {
                instance ?: KioSyncViewModelFactory(Injection.provideKioSyncRepository(context))
                    .also { instance = it }
            }
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MainViewModel::class.java) ->
                MainViewModel(kioSyncRepository) as T

            else -> throw IllegalArgumentException("unknown viewmodel class: ${modelClass.name}")
        }
    }
}
