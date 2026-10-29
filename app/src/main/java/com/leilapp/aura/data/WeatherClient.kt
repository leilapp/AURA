package com.leilapp.aura.data

import com.leilapp.aura.domain.DayForecast
import com.leilapp.aura.domain.WeatherSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate

/**
 * Cliente mínimo para Open-Meteo (https://open-meteo.com/en/docs), que no requiere clave API.
 * Solo se envían coordenadas o el nombre de la ciudad; ningún dato personal.
 * Se usa HttpURLConnection + org.json (incluidos en Android) para no añadir dependencias.
 */
class WeatherClient {

    suspend fun searchCity(query: String): List<WeatherPlace> = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search?count=5&language=es&format=json&name=" +
            URLEncoder.encode(query.trim(), "UTF-8")
        val json = JSONObject(get(url))
        val results = json.optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).map { i ->
            val r = results.getJSONObject(i)
            val parts = listOfNotNull(
                r.optString("name").takeIf { it.isNotBlank() },
                r.optString("admin1").takeIf { it.isNotBlank() },
                r.optString("country").takeIf { it.isNotBlank() },
            )
            WeatherPlace(parts.joinToString(", "), r.getDouble("latitude"), r.getDouble("longitude"))
        }
    }

    /** Devuelve el JSON crudo para poder guardarlo como caché. */
    suspend fun fetchForecastJson(place: WeatherPlace): String = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${place.latitude}&longitude=${place.longitude}" +
            "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,uv_index_max" +
            "&timezone=auto&forecast_days=5"
        val body = get(url)
        JSONObject(body).put("aura_place", place.name).put("aura_fetched_at", System.currentTimeMillis()).toString()
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("El servicio del clima respondió con el código $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        fun parse(json: String): WeatherSnapshot {
            val root = JSONObject(json)
            val current = root.getJSONObject("current")
            val daily = root.getJSONObject("daily")
            val dates = daily.getJSONArray("time")
            val days = (0 until dates.length()).map { i ->
                DayForecast(
                    epochDay = LocalDate.parse(dates.getString(i)).toEpochDay(),
                    min = daily.getJSONArray("temperature_2m_min").getDouble(i),
                    max = daily.getJSONArray("temperature_2m_max").getDouble(i),
                    precipitationProbability = daily.optJSONArray("precipitation_probability_max")
                        ?.takeUnless { it.isNull(i) }?.getInt(i),
                    weatherCode = daily.getJSONArray("weather_code").getInt(i),
                    uvIndexMax = daily.optJSONArray("uv_index_max")?.takeUnless { it.isNull(i) }?.getDouble(i),
                )
            }
            return WeatherSnapshot(
                place = root.optString("aura_place"),
                temperature = current.getDouble("temperature_2m"),
                apparentTemperature = if (current.isNull("apparent_temperature")) null else current.getDouble("apparent_temperature"),
                weatherCode = current.getInt("weather_code"),
                windSpeed = if (current.isNull("wind_speed_10m")) null else current.getDouble("wind_speed_10m"),
                days = days,
                fetchedAt = root.optLong("aura_fetched_at"),
            )
        }
    }
}
