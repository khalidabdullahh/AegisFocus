package com.aegisfocus.app.presentation.campfire

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegisfocus.app.data.local.CampfireStore
import com.aegisfocus.app.domain.campfire.CampfireMode
import com.aegisfocus.app.domain.campfire.CampfirePreset
import com.aegisfocus.app.domain.campfire.CampfirePresets
import com.aegisfocus.app.domain.campfire.CampfireSound
import com.aegisfocus.app.domain.campfire.SavedMix
import com.aegisfocus.app.domain.subscription.EntitlementManager
import com.aegisfocus.app.engine.CampfireAudioEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CampfireUiState(
    val mode: CampfireMode = CampfireMode.SLEEP,
    val levels: Map<CampfireSound, Float> = emptyMap(),
    val isPlaying: Boolean = false,
    val timerRemainingMs: Long = 0L,
    val isPro: Boolean = false,
    val savedMixes: List<SavedMix> = emptyList()
)

class CampfireViewModel(
    private val engine: CampfireAudioEngine,
    private val store: CampfireStore,
    private val entitlementManager: EntitlementManager
) : ViewModel() {

    private val isPro = MutableStateFlow(false)
    private val savedMixes = MutableStateFlow(store.getMixes())

    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }

    val uiState: StateFlow<CampfireUiState> = combine(
        engine.state, savedMixes, isPro, ticker
    ) { s, mixes, pro, now ->
        CampfireUiState(
            mode = s.mode,
            levels = s.levels,
            isPlaying = s.isPlaying,
            timerRemainingMs = if (s.timerEndEpochMs > 0) (s.timerEndEpochMs - now).coerceAtLeast(0L) else 0L,
            isPro = pro,
            savedMixes = mixes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CampfireUiState())

    init {
        // First open after a process start: restore the last mix (silent, not playing)
        if (engine.state.value.levels.isEmpty() && !engine.state.value.isPlaying) {
            val mode = store.lastMode() ?: CampfireMode.SLEEP
            val last = store.lastLevels().filterKeys { it in mode.sounds }
            engine.setMix(if (last.isNotEmpty()) last else CampfirePresets.default.levels, mode)
        }
        viewModelScope.launch {
            entitlementManager.activePlan.collect { isPro.value = entitlementManager.canUseAllCampfireSounds() }
        }
    }

    // ----- actions -----

    fun togglePlay() = engine.toggle()

    fun selectMode(mode: CampfireMode) {
        if (mode == engine.state.value.mode) return
        engine.setMode(mode)
        persist()
    }

    fun setLevel(sound: CampfireSound, level: Float) = engine.setLevel(sound, level)

    /** Called when the user lifts the finger off a slider. */
    fun persist() {
        val s = engine.state.value
        store.saveLast(s.mode, s.levels)
    }

    fun applyPreset(preset: CampfirePreset, startPlaying: Boolean = true) {
        engine.setMix(preset.levels, preset.mode)
        persist()
        if (startPlaying && !engine.state.value.isPlaying) engine.play()
    }

    fun applySaved(mix: SavedMix) {
        engine.setMix(mix.levels, mix.mode)
        persist()
        if (!engine.state.value.isPlaying) engine.play()
    }

    fun saveCurrentMix(name: String) {
        val s = engine.state.value
        if (s.levels.isEmpty() || name.isBlank()) return
        savedMixes.value = store.addMix(name, s.mode, s.levels)
    }

    fun deleteMix(id: Long) {
        savedMixes.value = store.deleteMix(id)
    }

    fun startTimer(minutes: Int) {
        if (!engine.state.value.isPlaying) engine.play()
        if (engine.state.value.isPlaying) engine.startTimer(minutes)
    }

    fun cancelTimer() = engine.cancelTimer()
}
