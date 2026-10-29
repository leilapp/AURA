package com.leilapp.aura.ui.child

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
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leilapp.aura.data.EventKind
import com.leilapp.aura.ui.agenda.EventsViewModel
import com.leilapp.aura.ui.agenda.conflictsForDay
import com.leilapp.aura.ui.auraViewModel
import com.leilapp.aura.ui.components.AuraScaffold
import com.leilapp.aura.ui.components.CenteredContent
import com.leilapp.aura.ui.components.EmptyState
import com.leilapp.aura.ui.components.SectionHeader
import com.leilapp.aura.ui.components.WarningBanner
import com.leilapp.aura.ui.today.EventRow
import java.time.LocalDate

@Composable
fun ChildScreen(onAdd: (kind: String?) -> Unit, onOpen: (Long) -> Unit) {
    val vm = auraViewModel { EventsViewModel(it.repository) }
    val events by vm.events.collectAsStateWithLifecycle()
    var kindFilter by rememberSaveable { mutableStateOf<String?>(null) }
    var showDone by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val todayDay = today.toEpochDay()

    val all = events.orEmpty()
    val childItems = all.filter { it.forChild && (kindFilter == null || it.kind == kindFilter) && it.completed == showDone }
    val dated = childItems.filter { it.epochDay != null }.sortedWith(compareBy({ it.epochDay }, { it.startMinutes ?: -1 }))
    val past = dated.filter { it.epochDay!! < todayDay }
    val upcoming = dated.filter { it.epochDay!! >= todayDay }
    val undated = childItems.filter { it.epochDay == null }.sortedByDescending { it.priority }
    val conflictDays = all.filter { it.forChild && !it.completed && it.epochDay != null && it.epochDay >= todayDay }
        .mapNotNull { it.epochDay }.distinct()

    AuraScaffold(
        title = "Mi hijo",
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAdd(kindFilter) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Agregar") },
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = Color(0xFF2B1608),
            )
        },
    ) { padding ->
        CenteredContent(padding) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(selected = kindFilter == null, onClick = { kindFilter = null }, label = { Text("Todo") })
                    EventKind.child.forEach { k ->
                        FilterChip(selected = kindFilter == k, onClick = { kindFilter = k }, label = { Text(k) })
                    }
                }
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showDone, onClick = { showDone = false }, label = { Text("Pendientes") })
                    FilterChip(selected = showDone, onClick = { showDone = true }, label = { Text("Completados") })
                }
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!showDone) {
                        conflictDays.forEach { day ->
                            conflictsForDay(all, day).forEach { (a, b) ->
                                item { WarningBanner("Conflicto el ${LocalDate.ofEpochDay(day).dayOfMonth}: «${a.title}» y «${b.title}» se superponen.") }
                            }
                        }
                    }
                    if (events != null && childItems.isEmpty()) {
                        item {
                            EmptyState(
                                Icons.Filled.Face,
                                if (showDone) "Nada completado aún" else "Todo listo por ahora",
                                "Registra actividades, citas, eventos escolares, materiales por preparar, compras o fechas importantes.",
                                actionLabel = if (showDone) null else "Agregar",
                                onAction = if (showDone) null else ({ onAdd(kindFilter) }),
                            )
                        }
                    }
                    if (upcoming.isNotEmpty()) {
                        item { SectionHeader("Próximos") }
                        items(upcoming, key = { it.id }) { e -> EventRow(e, today, onClick = { onOpen(e.id) }, onDone = { vm.toggle(e) }) }
                    }
                    if (undated.isNotEmpty()) {
                        item { SectionHeader("Sin fecha (materiales, compras, notas)") }
                        items(undated, key = { it.id }) { e -> EventRow(e, today, onClick = { onOpen(e.id) }, onDone = { vm.toggle(e) }) }
                    }
                    if (past.isNotEmpty()) {
                        item { SectionHeader(if (showDone) "Anteriores" else "Fechas pasadas sin completar") }
                        items(past.reversed(), key = { it.id }) { e -> EventRow(e, today, onClick = { onOpen(e.id) }, onDone = { vm.toggle(e) }) }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
