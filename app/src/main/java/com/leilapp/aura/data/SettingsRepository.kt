package com.leilapp.aura.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "aura_settings")

data class WeatherPlace(val name: String, val latitude: Double, val longitude: Double)

data class AuraSettings(
    val accent: String = "VIOLETA",
    val userName: String = "",
    val place: WeatherPlace? = null,
    val notificationsIntroSeen: Boolean = false,
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val accent = stringPreferencesKey("accent")
        val userName = stringPreferencesKey("user_name")
        val placeName = stringPreferencesKey("place_name")
        val placeLat = doublePreferencesKey("place_lat")
        val placeLon = doublePreferencesKey("place_lon")
        val notificationsIntro = booleanPreferencesKey("notifications_intro_seen")
        val weatherCache = stringPreferencesKey("weather_cache")
    }

    val settings: Flow<AuraSettings> = context.dataStore.data.map { it.toSettings() }

    /** Última respuesta real del proveedor de clima, para mostrarla sin conexión indicando su hora. */
    val weatherCache: Flow<String?> = context.dataStore.data.map { it[Keys.weatherCache] }

    private fun Preferences.toSettings(): AuraSettings {
        val name = this[Keys.placeName]
        val lat = this[Keys.placeLat]
        val lon = this[Keys.placeLon]
        return AuraSettings(
            accent = this[Keys.accent] ?: "VIOLETA",
            userName = this[Keys.userName] ?: "",
            place = if (name != null && lat != null && lon != null) WeatherPlace(name, lat, lon) else null,
            notificationsIntroSeen = this[Keys.notificationsIntro] ?: false,
        )
    }

    suspend fun setAccent(accent: String) = context.dataStore.edit { it[Keys.accent] = accent }

    suspend fun setUserName(name: String) = context.dataStore.edit { it[Keys.userName] = name.trim() }

    suspend fun setPlace(place: WeatherPlace) = context.dataStore.edit {
        it[Keys.placeName] = place.name
        it[Keys.placeLat] = place.latitude
        it[Keys.placeLon] = place.longitude
        it.remove(Keys.weatherCache)
    }

    suspend fun setNotificationsIntroSeen() = context.dataStore.edit { it[Keys.notificationsIntro] = true }

    suspend fun setWeatherCache(json: String) = context.dataStore.edit { it[Keys.weatherCache] = json }
}
