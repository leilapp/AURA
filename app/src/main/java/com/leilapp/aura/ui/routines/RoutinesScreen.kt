package com.leilapp.aura.ui.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.leilapp.aura.data.AuraRepository
import com.leilapp.aura.data.RoutineEntity
import com.leilapp.aura.domain.Weekdays
import com.leilapp.aura.domain.minutesLabel
import com.leilapp.aura.domain.shortSpanish
import com.leilapp.aura.ui.auraViewModel
import com.leilapp.aura.ui.components.AuraCard
import com.leilapp.aura.ui.components.AuraScaffold
import com.leilapp.aura.ui.components.AuraTextField
import com.leilapp.aura.ui.components.CenteredContent
import com.leilapp.aura.ui.components.ConfirmDeleteDialog
import com.leilapp.aura.ui.components.EmptyState
import com.leilapp.aura.ui.components.SectionHeader
import com.leilapp.aura.ui.components.TimeField
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

data class RoutinesState(val loaded: Boolean = false, val routines: List<RoutineEntity> = emptyList(), val doneToday: Set<Long> = emptySet())

class RoutinesViewModel(private val repository: AuraRepository) : ViewModel() {
    // Las rutinas se marcan por día: la fecha forma parte de la clave, así mañana empieza sin marcar.
    private val today = LocalDate.now()

    val state = combine(repository.routines, repository.routineCompletions(today)) { r, c ->
        RoutinesState(true, r, c.map { it.routineId }.toSet())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutinesState())

    fun toggleDone(routine: RoutineEntity, done: Boolean) = viewModelScope.launch { repository.setRoutineDone(routine.id, today, done) }
    fun setActive(routine: RoutineEntity, active: Boolean) = viewModelScope.launch { repository.saveRoutine(routine.copy(active = active)) }
    fun delete(routine: RoutineEntity, after: () -> Unit = {}) = viewModelScope.launch { repository.deleteRoutine(routine.id); after() }
    suspend fun load(id: Long) = repository.getRoutine(id)
    fun save(routine: RoutineEntity, after: () -> Unit) = viewModelScope.launch { repository.saveRoutine(routine); after() }
}

@Composable
fun RoutinesScreen(onBack: () -> Unit, onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val vm = auraViewModel { RoutinesViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val todayDow = LocalDate.now().dayOfWeek
    val forToday = state.routines.filter { it.active && Weekdays.contains(it.daysMask, todayDow) }
    val others = state.routines - forToday.toSet()

    AuraScaffold(
        title = "Rutinas",
        onBack = onBack,
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Nueva rutina") } },
    ) { padding ->
        CenteredContent(padding) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (state.loaded && state.routines.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.Filled.Refresh, "Crea tus propios hábitos",
                            "Aura no impone rutinas. Define las tuyas, los días en que tocan y, si quieres, una hora con recordatorio.",
                            actionLabel = "Nueva rutina", onAction = onAdd,
                        )
                    }
                }
                if (forToday.isNotEmpty()) {
                    val done = forToday.count { it.id in state.doneToday }
                    item {
                        AuraCard {
                            Text("Progreso de hoy: $done de ${forToday.size}", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { done.toFloat() / forToday.size }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    item { SectionHeader("Para hoy") }
                    items(forToday, key = { it.id }) { r ->
                        RoutineRow(r, done = r.id in state.doneToday, showCheck = true, onOpen = { onOpen(r.id) },
                            onCheck = { vm.toggleDone(r, it) }, onActive = { vm.setActive(r, it) })
                    }
                }
                if (others.isNotEmpty()) {
                    item { SectionHeader("Otros días o desactivadas") }
                    items(others, key = { it.id }) { r ->
                        RoutineRow(r, done = false, showCheck = false, onOpen = { onOpen(r.id) }, onCheck = {}, onActive = { vm.setActive(r, it) })
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun RoutineRow(
    routine: RoutineEntity,
    done: Boolean,
    showCheck: Boolean,
    onOpen: () -> Unit,
    onCheck: (Boolean) -> Unit,
    onActive: (Boolean) -> Unit,
) {
    AuraCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showCheck) Checkbox(checked = done, onCheckedChange = onCheck)
            Column(Modifier.weight(1f)) {
                Text(routine.title, style = MaterialTheme.typography.titleMedium)
                val time = routine.timeMinutes?.let { " · ${minutesLabel(it)}" } ?: ""
                Text(Weekdays.label(routine.daysMask) + time, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = routine.active, onCheckedChange = onActive)
        }
    }
}

@Composable
fun RoutineEditScreen(routineId: Long, onDone: () -> Unit) {
    val vm = auraViewModel { RoutinesViewModel(it.repository) }
    var original by remember { mutableStateOf<RoutineEntity?>(null) }
    var loaded by rememberSaveable { mutableStateOf(routineId == 0L) }
    var title by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var mask by rememberSaveable { mutableIntStateOf(Weekdays.ALL) }
    var time by rememberSaveable { mutableStateOf<Int?>(null) }
    var reminder by rememberSaveable { mutableStateOf(false) }
    var active by rememberSaveable { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(routineId) {
        if (routineId != 0L) {
            vm.load(routineId)?.let { r ->
                original = r
                if (!loaded) {
                    title = r.title; notes = r.notes; mask = r.daysMask; time = r.timeMinutes
                    reminder = r.reminderEnabled; active = r.active
                }
            }
            loaded = true
        }
    }

    AuraScaffold(
        title = if (routineId == 0L) "Nueva rutina" else "Editar rutina",
        onBack = onDone,
        actions = { if (original != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar rutina") } },
    ) { padding ->
        CenteredContent(padding) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AuraTextField(title, { title = it; error = null }, "Nombre de la rutina *", error = error)
                Text("Días", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DayOfWeek.entries.forEach { d ->
                        FilterChip(selected = Weekdays.contains(mask, d), onClick = { mask = Weekdays.toggle(mask, d) }, label = { Text(d.shortSpanish()) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { mask = Weekdays.ALL }) { Text("Todos") }
                    TextButton(onClick = { mask = Weekdays.WEEKDAYS }) { Text("Lun–Vie") }
                    TextButton(onClick = { mask = 0b1100000 }) { Text("Fin de semana") }
                }
                TimeField("Hora (opcional)", time, { time = it; if (it == null) reminder = false })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recordatorio a esa hora", style = MaterialTheme.typography.bodyLarge)
                        if (time == null) Text("Elige una hora para activarlo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = reminder, enabled = time != null, onCheckedChange = { reminder = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Activa", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = active, onCheckedChange = { active = it })
                }
                AuraTextField(notes, { notes = it }, "Notas", singleLine = false)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDone) { Text("Cancelar") }
                    Button(enabled = loaded, onClick = {
                        when {
                            title.isBlank() -> error = "Escribe un nombre"
                            mask == 0 -> error = "Elige al menos un día"
                            else -> vm.save(
                                (original ?: RoutineEntity(title = "", daysMask = mask)).copy(
                                    title = title.trim(), notes = notes.trim(), daysMask = mask, timeMinutes = time,
                                    reminderEnabled = reminder && time != null, active = active,
                                ),
                                onDone,
                            )
                        }
                    }) { Text("Guardar") }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDeleteDialog(title, onConfirm = { original?.let { vm.delete(it, onDone) } }, onDismiss = { confirmDelete = false })
    }
}
