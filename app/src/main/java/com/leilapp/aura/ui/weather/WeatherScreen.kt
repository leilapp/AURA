package com.leilapp.aura.ui.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.leilapp.aura.data.SettingsRepository
import com.leilapp.aura.data.WeatherPlace
import com.leilapp.aura.data.WeatherRepository
import com.leilapp.aura.domain.DayForecast
import com.leilapp.aura.domain.adviseOutfit
import com.leilapp.aura.domain.describeWeatherCode
import com.leilapp.aura.domain.relativeLabel
import com.leilapp.aura.ui.auraViewModel
import com.leilapp.aura.ui.components.AuraCard
import com.leilapp.aura.ui.components.AuraScaffold
import com.leilapp.aura.ui.components.AuraTextField
import com.leilapp.aura.ui.components.CenteredContent
import com.leilapp.aura.ui.components.SectionHeader
import com.leilapp.aura.ui.components.WarningBanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date
import kotlin.coroutines.resume
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine

data class WeatherUi(
    val loading: Boolean = false,
    val error: String? = null,
    val searching: Boolean = false,
    val results: List<WeatherPlace>? = null,
)

class WeatherViewModel(private val repo: WeatherRepository, private val settings: SettingsRepository) : ViewModel() {
    val snapshot = repo.snapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val prefs = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    private val _ui = MutableStateFlow(WeatherUi())
    val ui = _ui.asStateFlow()

    init { refresh(force = false) }

    fun refresh(force: Boolean = true) = viewModelScope.launch {
        _ui.value = _ui.value.copy(loading = true, error = null)
        val error = repo.refresh(force)
        _ui.value = _ui.value.copy(loading = false, error = error)
    }

    fun search(query: String) = viewModelScope.launch {
        if (query.isBlank()) return@launch
        _ui.value = _ui.value.copy(searching = true, error = null, results = null)
        val results = runCatching { repo.searchCity(query) }
        _ui.value = _ui.value.copy(
            searching = false,
            results = results.getOrNull(),
            error = if (results.isFailure) "No se pudo buscar la ciudad. Revisa tu conexión." else null,
        )
    }

    fun choose(place: WeatherPlace) = viewModelScope.launch {
        settings.setPlace(place)
        _ui.value = _ui.value.copy(results = null)
        refresh(force = true)
    }

    fun locationFailed(message: String) { _ui.value = _ui.value.copy(error = message) }
}

/** Consulta una única ubicación aproximada (no se rastrea de forma continua). */
@SuppressLint("MissingPermission")
private suspend fun currentCoarseLocation(context: Context): Location? {
    val manager = context.getSystemService(LocationManager::class.java) ?: return null
    val provider = when {
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        else -> return manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
    }
    manager.getLastKnownLocation(provider)?.let { return it }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    return suspendCancellableCoroutine { cont ->
        val signal = android.os.CancellationSignal()
        cont.invokeOnCancellation { signal.cancel() }
        manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { cont.resume(it) }
    }
}

@Composable
fun WeatherScreen(onBack: () -> Unit) {
    val vm = auraViewModel { WeatherViewModel(it.weather, it.settings) }
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var changingCity by remember { mutableStateOf(false) }

    fun useLocation() = scope.launch {
        val location = currentCoarseLocation(context)
        if (location == null) vm.locationFailed("No se pudo obtener tu ubicación. Escribe tu ciudad manualmente.")
        else vm.choose(WeatherPlace("Mi ubicación", (location.latitude * 100).roundToInt() / 100.0, (location.longitude * 100).roundToInt() / 100.0))
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useLocation() else vm.locationFailed("Sin permiso de ubicación. Puedes escribir tu ciudad.")
    }

    AuraScaffold(
        title = "Clima y vestimenta",
        onBack = onBack,
        actions = {
            if (prefs?.place != null) IconButton(onClick = { vm.refresh() }, enabled = !ui.loading) { Icon(Icons.Filled.Refresh, contentDescription = "Actualizar") }
        },
    ) { padding ->
        CenteredContent(padding) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val place = prefs?.place
                if (place == null || changingCity) {
                    item {
                        AuraCard {
                            Text("¿Dónde quieres consultar el clima?", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { AuraTextField(query, { query = it }, "Ciudad") }
                                IconButton(onClick = { vm.search(query) }) { Icon(Icons.Filled.Search, contentDescription = "Buscar ciudad") }
                            }
                            OutlinedButton(onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) useLocation()
                                else permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                            }) {
                                Icon(Icons.Filled.LocationOn, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Usar mi ubicación aproximada")
                            }
                            Text(
                                "La ubicación se consulta una sola vez y solo se envían coordenadas aproximadas al servicio Open-Meteo.",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (ui.searching) CircularProgressIndicator(Modifier.padding(8.dp))
                            ui.results?.let { results ->
                                if (results.isEmpty()) Text("No se encontraron ciudades con ese nombre.")
                                results.forEach { r ->
                                    TextButton(onClick = { vm.choose(r); changingCity = false; query = "" }) { Text(r.name) }
                                }
                            }
                            if (place != null) TextButton(onClick = { changingCity = false }) { Text("Cancelar") }
                        }
                    }
                } else {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(place.name, modifier = Modifier.weight(1f).padding(start = 6.dp), style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = { changingCity = true }) { Text("Cambiar") }
                        }
                    }
                }

                if (ui.loading) item { Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.padding(end = 12.dp)); Text("Consultando el clima…") } }
                ui.error?.let { msg ->
                    item {
                        Column {
                            WarningBanner(msg)
                            if (place != null) TextButton(onClick = { vm.refresh() }) { Text("Reintentar") }
                        }
                    }
                }

                val s = snapshot
                if (s != null && place != null) {
                    item {
                        AuraCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text("${s.temperature.roundToInt()} °C", style = MaterialTheme.typography.displaySmall)
                            Text(describeWeatherCode(s.weatherCode), style = MaterialTheme.typography.titleMedium)
                            s.apparentTemperature?.let { Text("Sensación térmica ${it.roundToInt()} °C") }
                            s.windSpeed?.let { Text("Viento ${it.roundToInt()} km/h") }
                            Text(
                                "Actualizado: " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(s.fetchedAt)) + " · Datos: Open-Meteo",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    val today = LocalDate.now()
                    val forecastToday = s.days.firstOrNull { it.epochDay == today.toEpochDay() }
                    if (forecastToday != null) {
                        item { SectionHeader("¿Qué me pongo hoy?") }
                        item { OutfitCard(forecastToday, s.apparentTemperature) }
                    }
                    item { SectionHeader("Próximos días") }
                    s.days.filter { it.epochDay > today.toEpochDay() }.forEach { day ->
                        item { ForecastRow(day, today) }
                    }
                } else if (place != null && !ui.loading && ui.error == null) {
                    item { Text("Clima no disponible todavía.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun OutfitCard(day: DayForecast, feelsLike: Double?) {
    val advice = adviseOutfit(day, feelsLike)
    AuraCard {
        Text(advice.headline, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        advice.items.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
        advice.warnings.forEach { Spacer(Modifier.height(6.dp)); WarningBanner(it) }
    }
}

@Composable
private fun ForecastRow(day: DayForecast, today: LocalDate) {
    var expanded by remember { mutableStateOf(false) }
    AuraCard(onClick = { expanded = !expanded }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(LocalDate.ofEpochDay(day.epochDay).relativeLabel(today), style = MaterialTheme.typography.titleMedium)
                Text(describeWeatherCode(day.weatherCode), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${day.min.roundToInt()}° / ${day.max.roundToInt()}°", style = MaterialTheme.typography.titleMedium)
                day.precipitationProbability?.let { Text("Lluvia $it %", style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (expanded) {
            val advice = adviseOutfit(day)
            Spacer(Modifier.height(8.dp))
            Text(advice.headline, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            advice.items.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            advice.warnings.forEach { Text("⚠ $it", style = MaterialTheme.typography.bodyMedium) }
        } else {
            Text("Toca para ver qué ponerte", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
