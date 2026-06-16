package com.mascill.kiosync

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import com.mascill.kiosync.core.data.repository.KioskRepository
import com.mascill.kiosync.core.model.LaunchableApp
import com.mascill.kiosync.core.system.ElapsedRealtimeClock
import com.mascill.kiosync.feature.kiosk.model.KioskSideEffect
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KioskViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_followsRepositoryFlows() = runViewModelTest {
        val repository = FakeKioskRepository(
            kioskEnabled = true,
            allowedPackages = setOf(ALLOWED_PACKAGE),
            launchableApps = listOf(allowedApp())
        )
        val viewModel = createViewModel(repository)
        collectUiState(viewModel)

        assertTrue(viewModel.uiState.value.kioskEnabled)
        assertEquals(setOf(ALLOWED_PACKAGE), viewModel.uiState.value.allowedPackages)
        assertEquals(listOf(ALLOWED_PACKAGE), viewModel.uiState.value.allowedApps.map { it.packageName })
    }

    @Test
    fun statusTap_sevenTimes_opensPinDialog() = runViewModelTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)

        repeat(7) {
            viewModel.onStatusTap()
        }
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showPinDialog)
    }

    @Test
    fun confirmPin_validPin_opensAdminPanel() = runViewModelTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.onPinChange("123456")
        viewModel.confirmPin()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showPinDialog)
        assertTrue(viewModel.uiState.value.showAdminPanel)
        assertFalse(viewModel.uiState.value.pinError)
    }

    @Test
    fun confirmPin_invalidPin_showsError() = runViewModelTest {
        val viewModel = createViewModel()
        collectUiState(viewModel)

        viewModel.onPinChange("111111")
        viewModel.confirmPin()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.pinError)
        assertFalse(viewModel.uiState.value.showAdminPanel)
    }

    @Test
    fun kioskToggle_savesStateAndEmitsStartOrStop() = runViewModelTest {
        val repository = FakeKioskRepository()
        val viewModel = createViewModel(repository)
        collectUiState(viewModel)

        val startEffect = async { viewModel.sideEffects.first() }
        viewModel.onKioskEnabledChange(true)
        advanceUntilIdle()

        assertTrue(repository.kioskEnabledFlow.value)
        assertEquals(
            KioskSideEffect.StartKiosk(setOf(APP_PACKAGE)),
            startEffect.await()
        )

        val stopEffect = async { viewModel.sideEffects.first() }
        viewModel.onKioskEnabledChange(false)
        advanceUntilIdle()

        assertFalse(repository.kioskEnabledFlow.value)
        assertEquals(KioskSideEffect.StopKiosk, stopEffect.await())
    }

    @Test
    fun hostResumed_whenKioskActive_emitsStartKiosk() = runViewModelTest {
        val viewModel = createViewModel(
            FakeKioskRepository(kioskEnabled = true)
        )

        val effect = async { viewModel.sideEffects.first() }
        viewModel.onHostResumed()
        advanceUntilIdle()

        assertEquals(
            KioskSideEffect.StartKiosk(setOf(APP_PACKAGE)),
            effect.await()
        )
    }

    @Test
    fun hostResumed_whenKioskActiveDuringBootGracePeriod_emitsDelayKioskStart() =
        runViewModelTest {
            val viewModel = createViewModel(
                repository = FakeKioskRepository(kioskEnabled = true),
                elapsedRealtimeMs = 10_000L
            )
            collectUiState(viewModel)

            val effect = async { viewModel.sideEffects.first() }
            viewModel.onHostResumed()
            advanceUntilIdle()

            assertEquals(KioskSideEffect.DelayKioskStart(50_000L), effect.await())
            assertTrue(viewModel.uiState.value.waitingForSystemInit)
        }

    @Test
    fun sideEffect_emittedBeforeCollection_isDeliveredToCollector() = runViewModelTest {
        val viewModel = createViewModel(
            repository = FakeKioskRepository(kioskEnabled = true),
            elapsedRealtimeMs = 10_000L
        )

        viewModel.onHostResumed()
        advanceUntilIdle()

        assertEquals(
            KioskSideEffect.DelayKioskStart(50_000L),
            viewModel.sideEffects.first()
        )
    }

    @Test
    fun delayedKioskStartReady_whenKioskActive_emitsStartKioskAndClearsWaiting() =
        runViewModelTest {
            val viewModel = createViewModel(
                repository = FakeKioskRepository(kioskEnabled = true),
                elapsedRealtimeMs = 10_000L
            )
            collectUiState(viewModel)

            val delayEffect = async { viewModel.sideEffects.first() }
            viewModel.onHostResumed()
            advanceUntilIdle()
            assertEquals(KioskSideEffect.DelayKioskStart(50_000L), delayEffect.await())

            val startEffect = async { viewModel.sideEffects.first() }
            viewModel.onDelayedKioskStartReady()
            advanceUntilIdle()

            assertEquals(
                KioskSideEffect.StartKiosk(setOf(APP_PACKAGE)),
                startEffect.await()
            )
            assertFalse(viewModel.uiState.value.waitingForSystemInit)
        }

    @Test
    fun hostResumed_whenKioskInactive_emitsSetKioskInactive() = runViewModelTest {
        val viewModel = createViewModel(
            FakeKioskRepository(kioskEnabled = false)
        )

        val effect = async { viewModel.sideEffects.first() }
        viewModel.onHostResumed()
        advanceUntilIdle()

        assertEquals(KioskSideEffect.SetKioskInactive, effect.await())
    }

    @Test
    fun delayedKioskStartReady_whenKioskWasDisabled_emitsSetKioskInactiveAndClearsWaiting() =
        runViewModelTest {
            val repository = FakeKioskRepository(kioskEnabled = true)
            val viewModel = createViewModel(
                repository = repository,
                elapsedRealtimeMs = 10_000L
            )
            collectUiState(viewModel)

            val delayEffect = async { viewModel.sideEffects.first() }
            viewModel.onHostResumed()
            advanceUntilIdle()
            assertEquals(KioskSideEffect.DelayKioskStart(50_000L), delayEffect.await())

            repository.kioskEnabledFlow.value = false
            advanceUntilIdle()

            val effect = async { viewModel.sideEffects.first() }
            viewModel.onDelayedKioskStartReady()
            advanceUntilIdle()

            assertEquals(KioskSideEffect.SetKioskInactive, effect.await())
            assertFalse(viewModel.uiState.value.waitingForSystemInit)
        }

    @Test
    fun allowedAppChange_whenKioskActive_emitsApplyPolicy() = runViewModelTest {
        val viewModel = createViewModel(
            FakeKioskRepository(
                kioskEnabled = true,
                launchableApps = listOf(allowedApp())
            )
        )
        collectUiState(viewModel)

        val effect = async { viewModel.sideEffects.first() }
        viewModel.onAllowedAppChange(ALLOWED_PACKAGE, true)
        advanceUntilIdle()

        assertEquals(
            KioskSideEffect.ApplyPolicy(setOf(APP_PACKAGE, ALLOWED_PACKAGE)),
            effect.await()
        )
    }

    @Test
    fun allowedAppChange_whenKioskInactive_doesNotEmitApplyPolicy() = runViewModelTest {
        val viewModel = createViewModel(
            FakeKioskRepository(
                kioskEnabled = false,
                launchableApps = listOf(allowedApp())
            )
        )
        collectUiState(viewModel)

        viewModel.onAllowedAppChange(ALLOWED_PACKAGE, true)
        advanceUntilIdle()

        val effect = withTimeoutOrNull(100) {
            viewModel.sideEffects.first()
        }
        assertNull(effect)
    }

    @Test
    fun launchApp_whenAllowedAndLaunchable_emitsLaunchApp() = runViewModelTest {
        val viewModel = createViewModel(
            FakeKioskRepository(
                allowedPackages = setOf(ALLOWED_PACKAGE),
                launchableApps = listOf(allowedApp())
            )
        )
        collectUiState(viewModel)

        val effect = async { viewModel.sideEffects.first() }
        viewModel.onLaunchApp(ALLOWED_PACKAGE)
        advanceUntilIdle()

        assertEquals(KioskSideEffect.LaunchApp(ALLOWED_PACKAGE), effect.await())
    }

    @Test
    fun launchApp_whenNotAllowed_doesNotEmitLaunchApp() = runViewModelTest {
        val viewModel = createViewModel(
            FakeKioskRepository(
                allowedPackages = emptySet(),
                launchableApps = listOf(allowedApp())
            )
        )
        collectUiState(viewModel)

        viewModel.onLaunchApp(ALLOWED_PACKAGE)
        advanceUntilIdle()

        val effect = withTimeoutOrNull(100) {
            viewModel.sideEffects.first()
        }
        assertNull(effect)
    }

    @Test
    fun lockTaskPackages_includesAppPackageAndAllowedLaunchablePackagesOnly() = runViewModelTest {
        val viewModel = createViewModel(
            FakeKioskRepository(
                allowedPackages = setOf(ALLOWED_PACKAGE, STALE_PACKAGE),
                launchableApps = listOf(allowedApp())
            )
        )
        collectUiState(viewModel)

        assertEquals(
            setOf(APP_PACKAGE, ALLOWED_PACKAGE),
            viewModel.lockTaskPackages()
        )
    }

    private fun TestScope.collectUiState(viewModel: KioskViewModel) {
        backgroundScope.launch(dispatcher) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
    }

    private fun createViewModel(
        repository: FakeKioskRepository = FakeKioskRepository(),
        elapsedRealtimeMs: Long = BOOT_GRACE_PERIOD_MS
    ): KioskViewModel {
        return KioskViewModel(
            repository = repository,
            appPackageName = APP_PACKAGE,
            elapsedRealtimeClock = FakeElapsedRealtimeClock(elapsedRealtimeMs)
        )
    }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) {
        runTest(dispatcher) {
            block()
        }
    }

    private fun allowedApp(): LaunchableApp {
        return LaunchableApp(
            label = "Allowed App",
            packageName = ALLOWED_PACKAGE,
            icon = TestDrawable()
        )
    }

    private class FakeKioskRepository(
        kioskEnabled: Boolean = false,
        allowedPackages: Set<String> = emptySet(),
        private val launchableApps: List<LaunchableApp> = emptyList()
    ) : KioskRepository {

        val kioskEnabledFlow = MutableStateFlow(kioskEnabled)
        val allowedPackagesFlow = MutableStateFlow(allowedPackages)

        override val kioskEnabled: Flow<Boolean> = kioskEnabledFlow
        override val allowedPackages: Flow<Set<String>> = allowedPackagesFlow

        override suspend fun setKioskEnabled(enabled: Boolean) {
            kioskEnabledFlow.value = enabled
        }

        override suspend fun setAllowedPackages(packageNames: Set<String>) {
            allowedPackagesFlow.value = packageNames
        }

        override fun getLaunchableApps(): List<LaunchableApp> {
            return launchableApps
        }
    }

    private class FakeElapsedRealtimeClock(
        private val elapsedRealtimeMs: Long
    ) : ElapsedRealtimeClock {
        override fun elapsedRealtimeMs(): Long = elapsedRealtimeMs
    }

    private class TestDrawable : Drawable() {
        override fun draw(canvas: Canvas) = Unit
        override fun setAlpha(alpha: Int) = Unit
        override fun setColorFilter(colorFilter: ColorFilter?) = Unit
        @Deprecated("Deprecated in Android framework")
        override fun getOpacity(): Int = PixelFormat.TRANSPARENT
    }

    private companion object {
        const val APP_PACKAGE = "com.mascill.kiosync"
        const val ALLOWED_PACKAGE = "com.example.allowed"
        const val STALE_PACKAGE = "com.example.stale"
        const val BOOT_GRACE_PERIOD_MS = 60_000L
    }
}
