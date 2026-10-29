package com.leilapp.aura.ui.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leilapp.aura.data.TaskEntity
import com.leilapp.aura.ui.auraViewModel
import com.leilapp.aura.ui.components.AuraScaffold
import com.leilapp.aura.ui.components.CenteredContent
import com.leilapp.aura.ui.components.ConfirmDeleteDialog
import com.leilapp.aura.ui.components.EmptyState
import com.leilapp.aura.ui.today.TaskRow
import java.time.LocalDate

@Composable
fun TasksScreen(onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val vm = auraViewModel { TasksViewModel(it.repository) }
    val state by vm.state.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<TaskEntity?>(null) }
    val today = LocalDate.now()

    AuraScaffold(
        title = "Tareas",
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Nueva tarea") }
        },
    ) { padding ->
        CenteredContent(padding) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TaskFilter.entries.forEach { f ->
                        FilterChip(selected = state.filter == f, onClick = { vm.setFilter(f) }, label = { Text(f.label) })
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = state.categoryId == null, onClick = { vm.setCategory(null) }, label = { Text("Todas las categorías") })
                    state.categories.forEach { c ->
                        FilterChip(selected = state.categoryId == c.id, onClick = { vm.setCategory(c.id) }, label = { Text(c.name) })
                    }
                }
                if (!state.loading && state.tasks.isEmpty()) {
                    EmptyState(
                        icon = Icons.Filled.CheckCircle,
                        title = "No hay tareas aquí",
                        message = "Prueba otro filtro o crea una tarea nueva.",
                        actionLabel = "Nueva tarea",
                        onAction = onAdd,
                    )
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        Row {
                            Column(Modifier.weight(1f)) {
                                TaskRow(task, today, onClick = { onOpen(task.id) }, onDone = { vm.toggle(task) })
                            }
                            IconButton(onClick = { toDelete = task }) { Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${task.title}") }
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
    toDelete?.let { task ->
        ConfirmDeleteDialog(task.title, onConfirm = { vm.delete(task) }, onDismiss = { toDelete = null })
    }
}
