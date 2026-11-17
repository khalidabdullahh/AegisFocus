package com.aegisfocus.app.domain.campfire

import androidx.annotation.RawRes
import com.aegisfocus.app.R

/** One looping ambient sound. [isFree] sounds are available without Pro. */
enum class CampfireSound(
    val key: String,
    val label: String,
    @RawRes val rawRes: Int,
    val isFree: Boolean
) {
    CAMPFIRE("campfire", "Campfire", R.raw.campfire_crackle, true),
    RAIN("rain", "Rain on tent", R.raw.rain_on_tent, true),
    BROWN("brown", "Brown noise", R.raw.brown_noise, true),
    CRICKETS("crickets", "Night crickets", R.raw.night_crickets, false),
    OWL("owl", "Distant owl", R.raw.distant_owl, false),
    WIND("wind", "Soft wind", R.raw.soft_wind, false),
    THUNDER("thunder", "Far thunder", R.raw.far_thunder, false),
    STREAM("stream", "Forest stream", R.raw.forest_stream, false),
    PINK("pink", "Pink noise", R.raw.pink_noise, false);

    companion object {
        fun fromKey(key: String): CampfireSound? = values().firstOrNull { it.key == key }
    }
}

enum class CampfireMode(
    val label: String,
    val sounds: List<CampfireSound>,
    val timerMinutes: List<Int>
) {
    SLEEP(
        label = "Deep Sleep",
        sounds = listOf(
            CampfireSound.CAMPFIRE, CampfireSound.CRICKETS, CampfireSound.OWL,
            CampfireSound.WIND, CampfireSound.RAIN, CampfireSound.THUNDER, CampfireSound.BROWN
        ),
        timerMinutes = listOf(15, 30, 60, 90)
    ),
    STUDY(
        label = "Study",
        sounds = listOf(
            CampfireSound.CAMPFIRE, CampfireSound.RAIN, CampfireSound.STREAM,
            CampfireSound.PINK, CampfireSound.BROWN, CampfireSound.WIND
        ),
        timerMinutes = listOf(25, 50, 90, 120)
    ),
    FOCUS(
        label = "Deep Focus",
        sounds = listOf(
            CampfireSound.BROWN, CampfireSound.PINK, CampfireSound.RAIN,
            CampfireSound.CAMPFIRE, CampfireSound.WIND
        ),
        timerMinutes = listOf(25, 50, 90, 120)
    );

    companion object {
        fun fromName(name: String?): CampfireMode =
            values().firstOrNull { it.name == name } ?: SLEEP
    }
}

data class CampfirePreset(
    val name: String,
    val tagline: String,
    val mode: CampfireMode,
    val levels: Map<CampfireSound, Float>
) {
    /** A preset is free when every sound in it is free. */
    val isFree: Boolean get() = levels.keys.all { it.isFree }
}

/** A mix the user saved themselves. */
data class SavedMix(
    val id: Long,
    val name: String,
    val mode: CampfireMode,
    val levels: Map<CampfireSound, Float>
)

private typealias S = CampfireSound

object CampfirePresets {

    val all: List<CampfirePreset> = listOf(
        // ----- Deep Sleep -----
        CampfirePreset(
            "Warm Fire", "Soft crackle and a low hum", CampfireMode.SLEEP,
            mapOf(S.CAMPFIRE to 0.55f, S.BROWN to 0.25f)
        ),
        CampfirePreset(
            "Deep Sleep Night", "Fire, crickets, a faraway owl", CampfireMode.SLEEP,
            mapOf(S.CAMPFIRE to 0.45f, S.CRICKETS to 0.5f, S.OWL to 0.25f, S.BROWN to 0.3f)
        ),
        CampfirePreset(
            "Cabin Storm", "Rain, thunder and a warm fire", CampfireMode.SLEEP,
            mapOf(S.RAIN to 0.6f, S.THUNDER to 0.45f, S.CAMPFIRE to 0.4f, S.WIND to 0.2f)
        ),
        CampfirePreset(
            "Forest Night", "Stream, crickets and wind", CampfireMode.SLEEP,
            mapOf(S.CRICKETS to 0.55f, S.OWL to 0.3f, S.STREAM to 0.3f, S.WIND to 0.2f)
        ),
        // ----- Study -----
        CampfirePreset(
            "Exam Focus", "Quiet fire, steady low noise", CampfireMode.STUDY,
            mapOf(S.CAMPFIRE to 0.35f, S.BROWN to 0.25f)
        ),
        CampfirePreset(
            "Rainy Desk", "Rain with a little fire", CampfireMode.STUDY,
            mapOf(S.RAIN to 0.5f, S.CAMPFIRE to 0.3f, S.BROWN to 0.15f)
        ),
        CampfirePreset(
            "Forest Library", "Stream and a distant fire", CampfireMode.STUDY,
            mapOf(S.STREAM to 0.35f, S.CAMPFIRE to 0.3f, S.WIND to 0.12f)
        ),
        // ----- Deep Focus -----
        CampfirePreset(
            "Night Coding", "Low hum and soft rain", CampfireMode.FOCUS,
            mapOf(S.BROWN to 0.35f, S.RAIN to 0.25f, S.CAMPFIRE to 0.2f)
        ),
        CampfirePreset(
            "Deep Work", "Pink noise to mask distractions", CampfireMode.FOCUS,
            mapOf(S.PINK to 0.3f, S.CAMPFIRE to 0.25f, S.WIND to 0.1f)
        ),
        CampfirePreset(
            "Quiet Desk", "Barely-there noise, zero distraction", CampfireMode.FOCUS,
            mapOf(S.PINK to 0.22f, S.BROWN to 0.2f, S.RAIN to 0.1f)
        )
    )

    fun forMode(mode: CampfireMode): List<CampfirePreset> = all.filter { it.mode == mode }

    val default: CampfirePreset get() = all.first()
}
