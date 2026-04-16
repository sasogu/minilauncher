package com.minilauncher

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class LunarPhaseTextTest {

    @Test
    fun lunarPhaseText_shows_new_moon_icon_when_displayed_percentage_is_zero() {
        val date = Date(KNOWN_NEW_MOON_MS - (SYNODIC_MS * 0.005).toLong())

        assertEquals("\uD83C\uDF11 0%", lunarPhaseText(date, showPercentage = true))
    }

    @Test
    fun lunarPhaseText_shows_full_moon_icon_when_displayed_percentage_is_hundred() {
        val date = Date(KNOWN_NEW_MOON_MS + (SYNODIC_MS * 0.495).toLong())

        assertEquals("\uD83C\uDF15 100%", lunarPhaseText(date, showPercentage = true))
    }

    companion object {
        private const val KNOWN_NEW_MOON_MS = 947_182_440_000L
        private const val SYNODIC_MS = 2_551_442_976.0
    }
}
