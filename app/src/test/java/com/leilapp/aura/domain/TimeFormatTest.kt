package com.leilapp.aura.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TimeFormatTest {
    @Test
    fun greetingDependsOnHour() {
        assertEquals("Buenos días", greetingFor(7))
        assertEquals("Buenas tardes", greetingFor(15))
        assertEquals("Buenas noches", greetingFor(22))
        assertEquals("Buenas noches", greetingFor(2))
    }

    @Test
    fun relativeLabels() {
        val today = LocalDate.of(2026, 10, 29)
        assertEquals("Hoy", today.relativeLabel(today))
        assertEquals("Mañana", today.plusDays(1).relativeLabel(today))
        assertEquals("Ayer", today.minusDays(1).relativeLabel(today))
    }

    @Test
    fun amountsAreParsedAndFormatted() {
        assertEquals(125050L, parseAmountToCents("1,250.50"))
        assertEquals(125050L, parseAmountToCents("1250,50"))
        assertEquals(99L, parseAmountToCents("0.99"))
        assertNull(parseAmountToCents("abc"))
        assertNull(parseAmountToCents("-5"))
        assertEquals("$1,250.50", formatAmount(125050))
        assertEquals("$0.05", formatAmount(5))
    }

    @Test
    fun minutesAreFormatted() {
        assertEquals("08:05", minutesLabel(8 * 60 + 5))
    }
}
