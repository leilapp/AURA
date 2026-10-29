package com.leilapp.aura.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.leilapp.aura.data.AuraRepository
import com.leilapp.aura.data.Category
import com.leilapp.aura.data.SettingsRepository
import com.leilapp.aura.reminders.Notifications
import com.leilapp.aura.reminders.ReminderScheduler
import com.leilapp.aura.ui.auraViewModel
import com.leilapp.aura.ui.components.AuraCard
import com.leilapp.aura.ui.components.AuraScaffold
import com.leilapp.aura.ui.components.AuraTextField
import com.leilapp.aura.ui.components.CenteredContent
import com.leilapp.aura.ui.components.ConfirmDeleteDialog
import com.leilapp.aura.ui.components.SectionHeader
import com.leilapp.aura.ui.theme.AuraAccent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val repository: AuraRepository,
    val scheduler: ReminderScheduler,
) : ViewModel() {
    val prefs = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val categories = repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setAccent(accent: AuraAccent) = viewModelScope.launch { settings.setAccent(accent.name) }
    fun setName(name: String) = viewModelScope.launch { settings.setUserName(name) }
    fun addCategory(name: String) = viewModelScope.launch { repository.addCategory(name) }
    fun deleteCategory(c: Category) = viewModelScope.launch { repository.deleteCategory(c) }
    fun loadDemo() = viewModelScope.launch { repository.loadDemoData() }
    fun deleteDemo() = viewModelScope.launch { repository.deleteDemoData() }
    fun rescheduleAll() = viewModelScope.launch { repository.rescheduleAll() }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val vm = auraViewModel { SettingsViewModel(it.settings, it.repository, it.scheduler) }
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var name by remember { mutableStateOf<String?>(null) }
    var newCategory by remember { mutableStateOf<String?>(null) }
    var deleteCategory by remember { mutableStateOf<Category?>(null) }
    var confirmDemoDelete by remember { mutableStateOf(false) }
    var notificationsOn by remember { mutableStateOf(Notifications.hasPermission(context)) }
    var exactOn by remember { mutableStateOf(vm.scheduler.canScheduleExact()) }

    // Al volver de los ajustes del sistema, refresca el estado de los permisos.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            notificationsOn = Notifications.hasPermission(context)
            val exactNow = vm.scheduler.canScheduleExact()
            if (exactNow != exactOn) vm.rescheduleAll()
            exactOn = exactNow
        }
    }
    LaunchedEffect(prefs) { if (name == null && prefs != null) name = prefs?.userName }

    AuraScaffold(title = "Ajustes", onBack = onBack) { padding ->
        CenteredContent(padding) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Personal")
                AuraCard {
                    AuraTextField(name ?: "", { name = it }, "¿Cómo quieres que te salude? (opcional)")
                    TextButton(onClick = { vm.setName(name ?: "") }) { Text("Guardar nombre") }
                }

                SectionHeader("Color de acento")
                AuraCard {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        AuraAccent.entries.forEach { accent ->
                            val selected = prefs?.accent == accent.name
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .background(accent.primary, CircleShape)
                                        .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                        .clickable { vm.setAccent(accent) }
                                        .semantics { contentDescription = "Acento ${accent.label}" + if (selected) ", seleccionado" else "" },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                }
                                Text(accent.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                SectionHeader("Notificaciones")
                AuraCard {
                    Text(if (notificationsOn) "Notificaciones permitidas ✓" else "Notificaciones desactivadas", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Se usan solo para los recordatorios que tú configures.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }) { Text("Abrir ajustes de notificaciones") }
                    Spacer(Modifier.height(8.dp))
                    Text(if (exactOn) "Recordatorios a la hora exacta: activados ✓" else "Recordatorios a la hora exacta: desactivados", style = MaterialTheme.typography.titleSmall)
                    Text(
                        if (exactOn) "Android avisará a la hora programada."
                        else "Sin este permiso, Android puede retrasar los avisos unos minutos para ahorrar batería.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!exactOn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                            )
                        }) { Text("Permitir alarmas exactas") }
                    }
                    Text(
                        "Si tu teléfono tiene un ahorro de batería agresivo, puede que también debas permitir que Aura funcione en segundo plano.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SectionHeader("Categorías de tareas", actionLabel = "Agregar", onAction = { newCategory = "" })
                AuraCard {
                    categories.forEach { c ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.name, modifier = Modifier.weight(1f))
                            if (!c.builtIn) {
                                IconButton(onClick = { deleteCategory = c }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${c.name}") }
                            } else {
                                Text("Inicial", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                SectionHeader("Datos de ejemplo")
                AuraCard {
                    Text(
                        "Carga algunos registros marcados como «Ejemplo:» para explorar Aura. Puedes borrarlos todos después sin afectar tus datos.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        OutlinedButton(onClick = { vm.loadDemo() }) { Text("Cargar ejemplos") }
                        OutlinedButton(onClick = { confirmDemoDelete = true }) { Text("Borrar ejemplos") }
                    }
                }

                SectionHeader("Privacidad")
                AuraCard {
                    Text(
                        "Tus datos se guardan solo en este teléfono. Aura no tiene cuentas, no usa analítica y no envía información personal. " +
                            "Solo el clima se consulta en internet (Open-Meteo), enviando la ciudad o coordenadas aproximadas.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    newCategory?.let { value ->
        AlertDialog(
            onDismissRequest = { newCategory = null },
            title = { Text("Nueva categoría") },
            text = { AuraTextField(value, { newCategory = it }, "Nombre") },
            confirmButton = { TextButton(enabled = value.isNotBlank(), onClick = { vm.addCategory(value); newCategory = null }) { Text("Crear") } },
            dismissButton = { TextButton(onClick = { newCategory = null }) { Text("Cancelar") } },
        )
    }
    deleteCategory?.let { c ->
        ConfirmDeleteDialog("la categoría ${c.name} (las tareas se conservan sin categoría)", onConfirm = { vm.deleteCategory(c) }, onDismiss = { deleteCategory = null })
    }
    if (confirmDemoDelete) {
        ConfirmDeleteDialog("todos los datos de ejemplo", onConfirm = { vm.deleteDemo() }, onDismiss = { confirmDemoDelete = false })
    }
}
