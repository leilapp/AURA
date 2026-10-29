package com.leilapp.aura.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leilapp.aura.data.Priority
import com.leilapp.aura.data.TaskEntity
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
import java.time.LocalDate

@Composable
fun TaskEditScreen(taskId: Long, presetCategoryName: String?, onDone: () -> Unit) {
    val vm = auraViewModel { TasksViewModel(it.repository) }
    val categories by vm.categories.collectAsStateWithLifecycle()

    var original by remember { mutableStateOf<TaskEntity?>(null) }
    var loaded by rememberSaveable { mutableStateOf(taskId == 0L) }
    var title by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var due by rememberSaveable { mutableStateOf<Long?>(if (taskId == 0L) LocalDate.now().toEpochDay() else null) }
    var time by rememberSaveable { mutableStateOf<Int?>(null) }
    var priority by rememberSaveable { mutableStateOf(Priority.NORMAL) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var reminder by rememberSaveable { mutableStateOf<Int?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var newCategory by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(taskId) {
        if (taskId != 0L) {
            vm.load(taskId)?.let { t ->
                original = t
                if (!loaded) {
                    title = t.title; notes = t.notes; due = t.dueEpochDay; time = t.dueMinutes
                    priority = t.priority; categoryId = t.categoryId; reminder = t.reminderMinutesBefore
                }
            }
            loaded = true
        }
    }
    LaunchedEffect(categories, presetCategoryName) {
        if (taskId == 0L && categoryId == null && presetCategoryName != null) {
            categoryId = categories.firstOrNull { it.name == presetCategoryName }?.id
        }
    }

    AuraScaffold(
        title = if (taskId == 0L) "Nueva tarea" else "Editar tarea",
        onBack = onDone,
        actions = {
            if (original != null) {
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar tarea") }
            }
        },
    ) { padding ->
        CenteredContent(padding) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                AuraTextField(title, { title = it; titleError = null }, "Título *", error = titleError)
                DateField("Fecha", due, { due = it })
                TimeField("Hora (opcional)", time, { time = it })
                Text("Prioridad", style = MaterialTheme.typography.labelLarge)
                PrioritySelector(priority) { priority = it }
                OptionDropdown(
                    "Categoría",
                    listOf<Pair<Long?, String>>(null to "Sin categoría") + categories.map { it.id to it.name },
                    categoryId,
                ) { categoryId = it }
                TextButton(onClick = { newCategory = "" }) { Text("+ Crear categoría") }
                ReminderDropdown(reminder, { reminder = it }, hasTime = time != null)
                if (reminder != null && due == null) {
                    Text("Para recibir el recordatorio, la tarea necesita una fecha.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                AuraTextField(notes, { notes = it }, "Notas", singleLine = false)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDone) { Text("Cancelar") }
                    Button(enabled = loaded, onClick = {
                        if (title.isBlank()) {
                            titleError = "Escribe un título"
                        } else {
                            val base = original ?: TaskEntity(title = "")
                            vm.save(
                                base.copy(
                                    title = title.trim(), notes = notes.trim(), dueEpochDay = due,
                                    dueMinutes = if (due == null) null else time, priority = priority,
                                    categoryId = categoryId, reminderMinutesBefore = reminder,
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
    newCategory?.let { value ->
        AlertDialog(
            onDismissRequest = { newCategory = null },
            title = { Text("Nueva categoría") },
            text = { AuraTextField(value, { newCategory = it }, "Nombre") },
            confirmButton = {
                TextButton(enabled = value.isNotBlank(), onClick = { vm.addCategory(value); newCategory = null }) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = { newCategory = null }) { Text("Cancelar") } },
        )
    }
}
