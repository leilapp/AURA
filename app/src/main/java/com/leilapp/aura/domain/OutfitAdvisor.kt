package com.leilapp.aura.domain

/** Datos reales devueltos por el proveedor meteorológico. */
data class WeatherSnapshot(
    val place: String,
    val temperature: Double,
    val apparentTemperature: Double?,
    val weatherCode: Int,
    val windSpeed: Double?,
    val days: List<DayForecast>,
    val fetchedAt: Long,
)

data class DayForecast(
    val epochDay: Long,
    val min: Double,
    val max: Double,
    val precipitationProbability: Int?,
    val weatherCode: Int,
    val uvIndexMax: Double?,
)

data class OutfitAdvice(val headline: String, val items: List<String>, val warnings: List<String>)

/** Descripción en español de los códigos WMO que usa Open-Meteo. */
fun describeWeatherCode(code: Int): String = when (code) {
    0 -> "Despejado"
    1 -> "Mayormente despejado"
    2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Niebla"
    51, 53, 55 -> "Llovizna"
    56, 57 -> "Llovizna helada"
    61, 63 -> "Lluvia"
    65 -> "Lluvia intensa"
    66, 67 -> "Lluvia helada"
    71, 73, 75, 77 -> "Nieve"
    80, 81 -> "Chubascos"
    82 -> "Chubascos fuertes"
    85, 86 -> "Chubascos de nieve"
    95 -> "Tormenta"
    96, 99 -> "Tormenta con granizo"
    else -> "Condición desconocida"
}

fun isRainCode(code: Int) = code in 51..67 || code in 80..82 || code in 95..99
fun isSnowCode(code: Int) = code in 71..77 || code in 85..86

/**
 * Reglas simples (sin IA) para sugerir vestimenta a partir del pronóstico de un día.
 * Pensado para ampliarse después con prendas registradas por la usuaria.
 */
fun adviseOutfit(day: DayForecast, currentFeelsLike: Double? = null): OutfitAdvice {
    val items = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val reference = currentFeelsLike ?: ((day.min + day.max) / 2)

    val headline = when {
        day.max < 10 || reference < 8 -> "Día frío: abrígate bien"
        day.max < 18 -> "Día fresco: lleva una capa extra"
        day.max < 27 -> "Temperatura agradable"
        else -> "Día caluroso: ropa ligera"
    }

    when {
        day.max < 10 || reference < 8 -> items += listOf("Abrigo o chamarra gruesa", "Suéter", "Bufanda y calcetines gruesos")
        day.max < 18 -> items += listOf("Chamarra ligera o suéter", "Pantalón largo")
        day.max < 27 -> items += listOf("Ropa cómoda de entretiempo", "Una capa ligera por si refresca")
        else -> items += listOf("Ropa ligera y transpirable", "Colores claros", "Lleva agua")
    }

    if (day.max - day.min >= 12) {
        items += "Vístete en capas: habrá mucha diferencia entre la mañana y la tarde"
    }

    val rainProbability = day.precipitationProbability ?: 0
    if (isRainCode(day.weatherCode) || rainProbability >= 40) {
        items += "Paraguas o impermeable"
        items += "Calzado que no se moje"
    }
    if (rainProbability >= 70) warnings += "Probabilidad de lluvia alta ($rainProbability %)"
    if (day.weatherCode in 95..99) warnings += "Posibles tormentas eléctricas"
    if (isSnowCode(day.weatherCode)) {
        items += "Botas y guantes"
        warnings += "Posible nieve o aguanieve"
    }
    if ((day.uvIndexMax ?: 0.0) >= 6) {
        items += "Protector solar, lentes y gorra"
        if ((day.uvIndexMax ?: 0.0) >= 8) warnings += "Índice UV muy alto"
    }
    if (day.max >= 33) warnings += "Calor intenso: evita el sol directo al mediodía"
    if (day.min <= 2) warnings += "Temperaturas cercanas a 0 °C"

    return OutfitAdvice(headline, items.distinct(), warnings)
}
