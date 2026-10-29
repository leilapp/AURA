package com.leilapp.aura.data

import com.leilapp.aura.domain.WeatherSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Une el cliente de Open-Meteo con la caché en DataStore. Nunca inventa datos: si falla, lo informa. */
class WeatherRepository(private val settings: SettingsRepository, private val client: WeatherClient) {

    val snapshot: Flow<WeatherSnapshot?> = settings.weatherCache.map { json ->
        json?.let { runCatching { WeatherClient.parse(it) }.getOrNull() }
    }

    suspend fun searchCity(query: String): List<WeatherPlace> = client.searchCity(query)

    /** Consulta el pronóstico. Devuelve null si todo salió bien, o un mensaje de error comprensible. */
    suspend fun refresh(force: Boolean = false): String? {
        val place = settings.settings.first().place ?: return "Elige una ciudad para consultar el clima."
        if (!force) {
            val current = snapshot.first()
            if (current != null && System.currentTimeMillis() - current.fetchedAt < STALE_AFTER_MS) return null
        }
        return try {
            settings.setWeatherCache(client.fetchForecastJson(place))
            null
        } catch (e: java.net.UnknownHostException) {
            "Sin conexión a internet. El resto de Aura sigue funcionando."
        } catch (e: java.net.SocketTimeoutException) {
            "El servicio del clima tardó demasiado en responder."
        } catch (e: Exception) {
            "No se pudo obtener el clima (${e.message ?: "error desconocido"})."
        }
    }

    companion object {
        const val STALE_AFTER_MS = 60 * 60 * 1000L
    }
}
