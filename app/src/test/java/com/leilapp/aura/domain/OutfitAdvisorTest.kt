package com.leilapp.aura.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OutfitAdvisorTest {
    private fun day(min: Double, max: Double, rain: Int? = 0, code: Int = 0, uv: Double? = 3.0) =
        DayForecast(0, min, max, rain, code, uv)

    @Test
    fun coldDaySuggestsWarmClothes() {
        val advice = adviseOutfit(day(2.0, 8.0))
        assertTrue(advice.headline.contains("frío"))
        assertTrue(advice.items.any { it.contains("Abrigo") })
    }

    @Test
    fun hotDaySuggestsLightClothes() {
        val advice = adviseOutfit(day(22.0, 34.0, uv = 9.0))
        assertTrue(advice.items.any { it.contains("ligera") })
        assertTrue(advice.items.any { it.contains("Protector solar") })
        assertTrue(advice.warnings.any { it.contains("Calor") })
    }

    @Test
    fun highRainProbabilityAddsUmbrellaAndWarning() {
        val advice = adviseOutfit(day(14.0, 20.0, rain = 80, code = 61))
        assertTrue(advice.items.any { it.contains("Paraguas") })
        assertTrue(advice.warnings.any { it.contains("80") })
    }

    @Test
    fun dryMildDayHasNoWarnings() {
        val advice = adviseOutfit(day(15.0, 23.0, rain = 10))
        assertTrue(advice.warnings.isEmpty())
        assertTrue(advice.items.none { it.contains("Paraguas") })
    }

    @Test
    fun weatherCodesAreDescribedInSpanish() {
        assertEquals("Despejado", describeWeatherCode(0))
        assertEquals("Tormenta", describeWeatherCode(95))
    }
}
