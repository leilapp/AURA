package com.leilapp.aura.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Hora usada para avisar de algo que tiene fecha pero no hora (pagos, tareas sin hora…). */
const val DEFAULT_REMINDER_MINUTES = 9 * 60

/** Duración supuesta de un evento sin hora de fin, para detectar conflictos. */
const val DEFAULT_EVENT_DURATION = 60

data class TimeSlot(val id: Long, val title: String, val epochDay: Long, val start: Int, val end: Int)

/** Momento del recordatorio, o null si ya pasó o no corresponde. */
fun reminderAt(
    epochDay: Long,
    minutes: Int?,
    minutesBefore: Int?,
    now: LocalDateTime,
): LocalDateTime? {
    if (minutesBefore == null) return null
    val base = LocalDate.ofEpochDay(epochDay).atTime(LocalTime.MIN).plusMinutes((minutes ?: DEFAULT_REMINDER_MINUTES).toLong())
    val at = base.minusMinutes(minutesBefore.toLong())
    return if (at.isAfter(now)) at else null
}

/**
 * Pares de actividades que se superponen el mismo día. Solo informa: nunca mueve ni borra nada.
 * Un evento sin hora no puede generar conflicto.
 */
fun findConflicts(slots: List<TimeSlot>): List<Pair<TimeSlot, TimeSlot>> {
    val result = mutableListOf<Pair<TimeSlot, TimeSlot>>()
    slots.groupBy { it.epochDay }.values.forEach { day ->
        val sorted = day.sortedBy { it.start }
        for (i in sorted.indices) {
            for (j in i + 1 until sorted.size) {
                val a = sorted[i]
                val b = sorted[j]
                if (b.start >= a.end) break
                result += a to b
            }
        }
    }
    return result
}

fun slotOf(id: Long, title: String, epochDay: Long?, start: Int?, end: Int?): TimeSlot? {
    if (epochDay == null || start == null) return null
    val realEnd = if (end != null && end > start) end else start + DEFAULT_EVENT_DURATION
    return TimeSlot(id, title, epochDay, start, realEnd)
}

object Weekdays {
    const val ALL = 0b1111111
    const val WEEKDAYS = 0b0011111

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun contains(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0

    fun toggle(mask: Int, day: DayOfWeek): Int = mask xor bit(day)

    fun label(mask: Int): String = when (mask) {
        ALL -> "Todos los días"
        WEEKDAYS -> "Lunes a viernes"
        0b1100000 -> "Fines de semana"
        0 -> "Ningún día"
        else -> DayOfWeek.entries.filter { contains(mask, it) }.joinToString(" · ") { it.shortSpanish() }
    }

    /** Próxima fecha y hora (a partir de "now", sin incluirlo) en que toca la rutina. */
    fun nextOccurrence(mask: Int, minutes: Int, now: LocalDateTime): LocalDateTime? {
        if (mask == 0) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (!contains(mask, date.dayOfWeek)) continue
            val at = date.atStartOfDay().plusMinutes(minutes.toLong())
            if (at.isAfter(now)) return at
        }
        return null
    }
}
