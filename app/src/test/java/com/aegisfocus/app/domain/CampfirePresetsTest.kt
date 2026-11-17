package com.aegisfocus.app.domain

import com.aegisfocus.app.domain.campfire.CampfireMode
import com.aegisfocus.app.domain.campfire.CampfirePresets
import com.aegisfocus.app.domain.campfire.CampfireSound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampfirePresetsTest {

    @Test
    fun everyPresetOnlyUsesSoundsOfItsMode() {
        CampfirePresets.all.forEach { preset ->
            assertTrue(
                "${preset.name} uses a sound outside ${preset.mode}",
                preset.levels.keys.all { it in preset.mode.sounds }
            )
        }
    }

    @Test
    fun levelsAreValidVolumes() {
        CampfirePresets.all.forEach { preset ->
            assertTrue(preset.levels.values.all { it in 0.01f..1f })
        }
    }

    @Test
    fun eachModeHasAFreePreset() {
        CampfireMode.values().forEach { mode ->
            assertTrue("$mode has no free preset", CampfirePresets.forMode(mode).any { it.isFree })
        }
    }

    @Test
    fun threeCoreSoundsAreFree() {
        assertEquals(3, CampfireSound.values().count { it.isFree })
    }

    @Test
    fun soundKeysAreUniqueAndResolvable() {
        val keys = CampfireSound.values().map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        CampfireSound.values().forEach { assertEquals(it, CampfireSound.fromKey(it.key)) }
    }
}
