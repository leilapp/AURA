package com.leilapp.aura.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val SPANISH: Locale = Locale.forLanguageTag("es-MX")

private val longDate = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SPANISH)
private val shortDate = DateTimeFormatter.ofPattern("EEE d MMM", SPANISH)

fun LocalDate.longLabel(): String = format(longDate).replaceFirstChar { it.titlecase(SPANISH) }

/** "Hoy", "Mañana", "Ayer" o una fecha corta. */
fun LocalDate.relativeLabel(today: LocalDate = LocalDate.now()): String = when (this) {
    today -> "Hoy"
    today.plusDays(1) -> "Mañana"
    today.minusDays(1) -> "Ayer"
    else -> format(shortDate).replaceFirstChar { it.titlecase(SPANISH) }
}

fun minutesLabel(minutes: Int): String = LocalTime.of(minutes / 60, minutes % 60).let {
    "%02d:%02d".format(it.hour, it.minute)
}

fun DayOfWeek.shortSpanish(): String =
    getDisplayName(TextStyle.SHORT, SPANISH).replace(".", "").replaceFirstChar { it.titlecase(SPANISH) }

/** Saludo según la hora: 5–11 mañana, 12–18 tarde, resto noche. */
fun greetingFor(hour: Int): String = when (hour) {
    in 5..11 -> "Buenos días"
    in 12..18 -> "Buenas tardes"
    else -> "Buenas noches"
}

fun formatAmount(cents: Long): String {
    val sign = if (cents < 0) "-" else ""
    val abs = kotlin.math.abs(cents)
    val whole = abs / 100
    val frac = abs % 100
    val grouped = whole.toString().reversed().chunked(3).joinToString(",").reversed()
    return sign + "\$" + grouped + "." + frac.toString().padStart(2, '0')
}

/** Convierte "1,234.50" o "1234,5" a centavos. Devuelve null si no es válido. */
fun parseAmountToCents(text: String): Long? {
    val clean = text.trim().replace(" ", "").replace("$", "")
    if (clean.isEmpty()) return null
    // Si hay coma y no punto, la coma es decimal; si hay ambos, la coma es separador de miles.
    val normalized = if (clean.contains(',') && !clean.contains('.')) clean.replace(',', '.') else clean.replace(",", "")
    val value = normalized.toBigDecimalOrNull() ?: return null
    if (value.signum() < 0) return null
    return value.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).toLong()
}

val REMINDER_OPTIONS: List<Pair<Int?, String>> = listOf(
    null to "Sin recordatorio",
    0 to "A la hora",
    10 to "10 min antes",
    30 to "30 min antes",
    60 to "1 hora antes",
    120 to "2 horas antes",
    1440 to "1 día antes",
    2880 to "2 días antes",
    10080 to "1 semana antes",
)

fun reminderLabel(value: Int?): String =
    REMINDER_OPTIONS.firstOrNull { it.first == value }?.second ?: "$value min antes"
