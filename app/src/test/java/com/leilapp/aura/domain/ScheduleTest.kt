package com.leilapp.aura.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleTest {
    private val day = LocalDate.of(2026, 10, 29).toEpochDay() // jueves

    @Test
    fun overlappingEventsAreReported() {
        val a = slotOf(1, "Natación", day, 17 * 60, 18 * 60)!!
        val b = slotOf(2, "Llamada", day, 17 * 60 + 30, null)!!
        val c = slotOf(3, "Cena", day, 19 * 60, 20 * 60)!!
        val conflicts = findConflicts(listOf(c, b, a))
        assertEquals(1, conflicts.size)
        assertEquals(setOf(1L, 2L), setOf(conflicts[0].first.id, conflicts[0].second.id))
    }

    @Test
    fun backToBackEventsDoNotConflict() {
        val a = slotOf(1, "A", day, 9 * 60, 10 * 60)!!
        val b = slotOf(2, "B", day, 10 * 60, 11 * 60)!!
        assertTrue(findConflicts(listOf(a, b)).isEmpty())
    }

    @Test
    fun eventsWithoutTimeNeverConflict() {
        assertNull(slotOf(1, "Sin hora", day, null, null))
    }

    @Test
    fun differentDaysDoNotConflict() {
        val a = slotOf(1, "A", day, 9 * 60, 10 * 60)!!
        val b = slotOf(2, "B", day + 1, 9 * 60, 10 * 60)!!
        assertTrue(findConflicts(listOf(a, b)).isEmpty())
    }

    @Test
    fun reminderIsComputedBeforeStart() {
        val now = LocalDateTime.of(2026, 10, 29, 8, 0)
        assertEquals(LocalDateTime.of(2026, 10, 29, 16, 30), reminderAt(day, 17 * 60, 30, now))
        // Sin hora: se usa las 9:00.
        assertEquals(LocalDateTime.of(2026, 10, 28, 9, 0), reminderAt(day, null, 1440, now.minusDays(2)))
    }

    @Test
    fun pastRemindersAreSkipped() {
        val now = LocalDateTime.of(2026, 10, 29, 18, 0)
        assertNull(reminderAt(day, 17 * 60, 0, now))
        assertNull(reminderAt(day, 17 * 60, null, now.minusDays(1)))
    }

    @Test
    fun routineNextOccurrenceRespectsWeekdays() {
        val thursdayMorning = LocalDateTime.of(2026, 10, 29, 7, 0)
        val mondayOnly = Weekdays.bit(DayOfWeek.MONDAY)
        assertEquals(LocalDateTime.of(2026, 11, 2, 8, 0), Weekdays.nextOccurrence(mondayOnly, 8 * 60, thursdayMorning))
        // Hoy toca y aún no es la hora.
        assertEquals(LocalDateTime.of(2026, 10, 29, 8, 0), Weekdays.nextOccurrence(Weekdays.ALL, 8 * 60, thursdayMorning))
        // Ya pasó la hora de hoy: siguiente semana mismo día.
        val thursdayOnly = Weekdays.bit(DayOfWeek.THURSDAY)
        assertEquals(LocalDateTime.of(2026, 11, 5, 8, 0), Weekdays.nextOccurrence(thursdayOnly, 8 * 60, thursdayMorning.plusHours(2)))
        assertNull(Weekdays.nextOccurrence(0, 8 * 60, thursdayMorning))
    }

    @Test
    fun weekdayMaskLabels() {
        assertEquals("Todos los días", Weekdays.label(Weekdays.ALL))
        assertEquals("Lunes a viernes", Weekdays.label(Weekdays.WEEKDAYS))
        assertTrue(Weekdays.contains(Weekdays.WEEKDAYS, DayOfWeek.FRIDAY))
        assertTrue(!Weekdays.contains(Weekdays.WEEKDAYS, DayOfWeek.SUNDAY))
    }
}
