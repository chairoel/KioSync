package com.mascill.kiosync.feature.kiosk.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mascill.kiosync.core.data.repository.KioskRepository
import com.mascill.kiosync.core.system.ElapsedRealtimeClock
import com.mascill.kiosync.di.Injection

@Suppress("UNCHECKED_CAST")
class KioskViewModelFactory(
    private val kioskRepository: KioskRepository,
    private val appPackageName: String,
    private val elapsedRealtimeClock: ElapsedRealtimeClock
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(KioskViewModel::class.java) ->
                KioskViewModel(
                    repository = kioskRepository,
                    appPackageName = appPackageName,
                    elapsedRealtimeClock = elapsedRealtimeClock
                ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    companion object {
        @Volatile
        private var instance: KioskViewModelFactory? = null

        fun getInstance(context: Context): KioskViewModelFactory =
            instance ?: synchronized(this) {
                val appContext = context.applicationContext
                instance ?: KioskViewModelFactory(
                    kioskRepository = Injection.provideKioskRepository(appContext),
                    appPackageName = Injection.provideAppPackageName(appContext),
                    elapsedRealtimeClock = Injection.provideElapsedRealtimeClock()
                )
                    .also { instance = it }
            }
    }
}
