package com.leilapp.aura.ui.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leilapp.aura.data.EventEntity
import com.leilapp.aura.data.EventKind
import com.leilapp.aura.data.Priority
import com.leilapp.aura.domain.minutesLabel
import com.leilapp.aura.domain.slotOf
import com.leilapp.aura.ui.auraViewModel
import com.leilapp.aura.ui.components.AuraScaffold
import com.leilapp.aura.ui.components.AuraTextField
import com.leilapp.aura.ui.components.CenteredContent
import com.leilapp.aura.ui.components.ConfirmDeleteDialog
import com.leilapp.aura.ui.components.DateField
import com.leilapp.aura.ui.components.OptionDropdown
import com.leilapp.aura.ui.components.PrioritySelector
import com.leilapp.aura.ui.components.ReminderDropdown
import com.leilapp.aura.ui.components.TimeField
import com.leilapp.aura.ui.components.WarningBanner

/**
 * Formulario compartido por la Agenda y "Mi hijo".
 * presetChild / presetKind / presetDay solo se usan al crear.
 */
@Composable
fun EventEditScreen(eventId: Long, presetChild: Boolean, presetKind: String?, presetDay: Long?, onDone: () -> Unit) {
    val vm = auraViewModel { EventsViewModel(it.repository) }
    val allEvents by vm.events.collectAsStateWithLifecycle()
    val isNew = eventId == 0L

    var original by remember { mutableStateOf<EventEntity?>(null) }
    var loaded by rememberSaveable { mutableStateOf(isNew) }
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var forChild by rememberSaveable { mutableStateOf(presetChild) }
    var kind by rememberSaveable { mutableStateOf(presetKind ?: if (presetChild) EventKind.ACTIVITY else EventKind.EVENT) }
    var day by rememberSaveable { mutableStateOf(presetDay ?: if (isNew && kind != EventKind.MATERIAL && kind != EventKind.TO_BUY) java.time.LocalDate.now().toEpochDay() else null) }
    var start by rememberSaveable { mutableStateOf<Int?>(null) }
    var end by rememberSaveable { mutableStateOf<Int?>(null) }
    var location by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(if (presetChild) Priority.HIGH else Priority.NORMAL) }
    var reminder by rememberSaveable { mutableStateOf<Int?>(if (presetKind == EventKind.REMINDER) 0 else null) }
    var completed by rememberSaveable { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var timeError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        if (!isNew) {
            vm.load(eventId)?.let { e ->
                original = e
                if (!loaded) {
                    title = e.title; description = e.description; forChild = e.forChild; kind = e.kind
                    day = e.epochDay; start = e.startMinutes; end = e.endMinutes; location = e.location
                    notes = e.notes; priority = e.priority; reminder = e.reminderMinutesBefore; completed = e.completed
                }
            }
            loaded = true
        }
    }

    val kinds = (if (forChild) EventKind.child else EventKind.general).let { if (kind in it) it else it + kind }
    val candidate = slotOf(eventId, title, day, start, end)
    val clashes = conflictsWith(candidate, allEvents.orEmpty())

    AuraScaffold(
        title = when {
            !isNew -> "Editar"
            forChild -> "Nuevo · Mi hijo"
            kind == EventKind.REMINDER -> "Nuevo recordatorio"
            else -> "Nuevo evento"
        },
        onBack = onDone,
        actions = {
            if (original != null) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") }
        },
    ) { padding ->
        CenteredContent(padding) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AuraTextField(title, { title = it; titleError = null }, "Título *", error = titleError)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Relacionado con mi hijo", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = forChild, onCheckedChange = { forChild = it })
                }
                OptionDropdown("Categoría", kinds.map { it to it }, kind) { kind = it }
                DateField("Fecha", day, { day = it })
                TimeField("Hora de inicio", start, { start = it; timeError = null })
                TimeField("Hora de fin (opcional)", end, { end = it; timeError = null })
                timeError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                clashes.forEach { other ->
                    val time = other.startMinutes?.let { " (${minutesLabel(it)})" } ?: ""
                    WarningBanner("Se superpone con «${other.title}»$time. Puedes guardarlo igualmente; Aura no cambiará ningún horario.")
                }
                AuraTextField(location, { location = it }, "Ubicación")
                Text("Prioridad", style = MaterialTheme.typography.labelLarge)
                PrioritySelector(priority) { priority = it }
                ReminderDropdown(reminder, { reminder = it }, hasTime = start != null)
                if (reminder != null && day == null) {
                    Text("Para recibir el recordatorio, agrega una fecha.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                AuraTextField(description, { description = it }, "Descripción", singleLine = false)
                AuraTextField(notes, { notes = it }, "Notas adicionales", singleLine = false)
                if (!isNew) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = completed, onCheckedChange = { completed = it })
                        Text("Completado")
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDone) { Text("Cancelar") }
                    Button(enabled = loaded, onClick = {
                        val s = start
                        val e = end
                        when {
                            title.isBlank() -> titleError = "Escribe un título"
                            s != null && e != null && e <= s -> timeError = "La hora de fin debe ser posterior a la de inicio"
                            s == null && e != null -> timeError = "Indica también la hora de inicio"
                            else -> vm.save(
                                (original ?: EventEntity(title = "")).copy(
                                    title = title.trim(), description = description.trim(), kind = kind,
                                    epochDay = day, startMinutes = if (day == null) null else s,
                                    endMinutes = if (day == null) null else e, location = location.trim(),
                                    notes = notes.trim(), forChild = forChild, priority = priority,
                                    completed = completed, reminderMinutesBefore = reminder,
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
