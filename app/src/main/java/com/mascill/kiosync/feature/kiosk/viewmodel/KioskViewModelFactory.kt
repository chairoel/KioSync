package com.mascill.kiosync.feature.kiosk.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mascill.kiosync.core.data.repository.KioskRepository
import com.mascill.kiosync.di.Injection

@Suppress("UNCHECKED_CAST")
class KioskViewModelFactory(
    private val kioskRepository: KioskRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(KioskViewModel::class.java) ->
                KioskViewModel(kioskRepository) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        @Volatile
        private var instance: KioskViewModelFactory? = null

        fun getInstance(context: Context): KioskViewModelFactory =
            instance ?: synchronized(this) {
                instance ?: KioskViewModelFactory(Injection.provideKioskRepository(context))
                    .also { instance = it }
            }
    }
}
