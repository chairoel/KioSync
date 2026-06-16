package com.mascill.kiosync.feature.kiosk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mascill.kiosync.core.data.repository.KioskRepository

@Suppress("UNCHECKED_CAST")
class KioskViewModelFactory(
    private val kioskRepository: KioskRepository,
    private val appPackageName: String,
    private val kioskStartPlanner: KioskStartPlanner
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(KioskViewModel::class.java) ->
                KioskViewModel(
                    repository = kioskRepository,
                    appPackageName = appPackageName,
                    kioskStartPlanner = kioskStartPlanner
                ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
