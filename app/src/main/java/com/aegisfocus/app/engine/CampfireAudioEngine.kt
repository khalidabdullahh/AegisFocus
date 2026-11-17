package com.aegisfocus.app.engine

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.content.ContextCompat
import com.aegisfocus.app.domain.campfire.CampfireMode
import com.aegisfocus.app.domain.campfire.CampfireSound
import com.aegisfocus.app.service.CampfireAudioService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mixes several looping ambient sounds (one MediaPlayer per sound, each with its own volume).
 * Lives in AppContainer so playback survives screen changes; CampfireAudioService keeps the
 * process alive and shows the media notification while it plays.
 *
 * All calls must be made on the main thread.
 */
class CampfireAudioEngine(context: Context) {

    data class State(
        val levels: Map<CampfireSound, Float> = emptyMap(),
        val isPlaying: Boolean = false,
        val timerEndEpochMs: Long = 0L,
        val mode: CampfireMode = CampfireMode.SLEEP
    )

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val players = mutableMapOf<CampfireSound, MediaPlayer>()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var master = 1f          // used for the sleep-timer fade-out
    private var duck = 1f            // lowered when another app asks us to duck
    private var resumeOnGain = false

    private val attrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnGain = false
                pauseInternal()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_state.value.isPlaying) {
                    resumeOnGain = true
                    pauseInternal()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                duck = 0.3f
                applyVolumes()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                duck = 1f
                applyVolumes()
                if (resumeOnGain) {
                    resumeOnGain = false
                    play()
                }
            }
        }
    }

    private val focusRequest: AudioFocusRequest by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(focusListener, Handler(Looper.getMainLooper()))
            .build()
    }

    // ───────────────────────────── mix control ─────────────────────────────

    /** Replace the whole mix (sounds not in [levels] go silent). */
    fun setMix(levels: Map<CampfireSound, Float>, mode: CampfireMode = _state.value.mode) {
        val clean = levels.mapValues { it.value.coerceIn(0f, 1f) }.filterValues { it > MIN_LEVEL }
        _state.update { it.copy(levels = clean, mode = mode) }
        syncPlayers()
    }

    fun setLevel(sound: CampfireSound, level: Float) {
        val v = level.coerceIn(0f, 1f)
        _state.update { s ->
            val next = s.levels.toMutableMap()
            if (v > MIN_LEVEL) next[sound] = v else next.remove(sound)
            s.copy(levels = next)
        }
        syncPlayers()
    }

    fun setMode(mode: CampfireMode) {
        val kept = _state.value.levels.filterKeys { it in mode.sounds }
        _state.update { it.copy(mode = mode) }
        setMix(kept, mode)
    }

    // ───────────────────────────── playback ─────────────────────────────

    fun play() {
        if (_state.value.levels.isEmpty()) return
        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        master = 1f
        _state.update { it.copy(isPlaying = true) }
        syncPlayers()
        startService()
    }

    /** User pressed pause/stop: also cancels the sleep timer. */
    fun pause() {
        cancelTimer()
        pauseInternal()
    }

    fun toggle() {
        if (_state.value.isPlaying) pause() else play()
    }

    private fun pauseInternal() {
        players.values.forEach { runCatching { if (it.isPlaying) it.pause() } }
        _state.update { it.copy(isPlaying = false) }
        if (!resumeOnGain) audioManager.abandonAudioFocusRequest(focusRequest)
    }

    // ───────────────────────────── timer ─────────────────────────────

    /** Plays for [minutes], then fades out over the last seconds and stops. */
    fun startTimer(minutes: Int) {
        cancelTimer()
        val totalMs = minutes * 60_000L
        val end = System.currentTimeMillis() + totalMs
        _state.update { it.copy(timerEndEpochMs = end) }
        timerJob = scope.launch {
            val fadeMs = minOf(FADE_OUT_MS, totalMs / 2)
            delay(totalMs - fadeMs)
            val steps = 60
            for (i in 1..steps) {
                master = 1f - i / steps.toFloat()
                applyVolumes()
                delay(fadeMs / steps)
            }
            pauseInternal()
            master = 1f
            _state.update { it.copy(timerEndEpochMs = 0L) }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        if (master != 1f) {
            master = 1f
            applyVolumes()
        }
        if (_state.value.timerEndEpochMs != 0L) {
            _state.update { it.copy(timerEndEpochMs = 0L) }
        }
    }

    // ───────────────────────────── internals ─────────────────────────────

    /** Make the running players match the current levels. */
    private fun syncPlayers() {
        val s = _state.value
        if (!s.isPlaying) {
            // Not playing: free anything left over, players are created lazily on play()
            if (players.isNotEmpty()) releaseAll()
            return
        }
        // stop sounds that were removed
        players.keys.filter { it !in s.levels }.forEach { sound ->
            players.remove(sound)?.let { runCatching { it.release() } }
        }
        if (s.levels.isEmpty()) {
            pause()
            return
        }
        s.levels.keys.forEach { sound ->
            val p = players[sound] ?: createPlayer(sound)?.also { players[sound] = it }
            if (p != null && !p.isPlaying) runCatching { p.start() }
        }
        applyVolumes()
    }

    private fun createPlayer(sound: CampfireSound): MediaPlayer? {
        val p = MediaPlayer.create(
            appContext, sound.rawRes, attrs, audioManager.generateAudioSessionId()
        ) ?: return null
        p.isLooping = true
        p.setWakeMode(appContext, PowerManager.PARTIAL_WAKE_LOCK)
        return p
    }

    private fun applyVolumes() {
        val levels = _state.value.levels
        players.forEach { (sound, p) ->
            val l = levels[sound] ?: 0f
            // squared curve feels more natural than linear for ambient sound
            val v = (l * l * master * duck).coerceIn(0f, 1f)
            runCatching { p.setVolume(v, v) }
        }
    }

    private fun releaseAll() {
        players.values.forEach { runCatching { it.release() } }
        players.clear()
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, CampfireAudioService::class.java)
                    .setAction(CampfireAudioService.ACTION_START)
            )
        } catch (_: Exception) {
            // Playback still works while the app is open if the system refuses the service.
        }
    }

    companion object {
        private const val MIN_LEVEL = 0.01f
        private const val FADE_OUT_MS = 15_000L
    }
}
