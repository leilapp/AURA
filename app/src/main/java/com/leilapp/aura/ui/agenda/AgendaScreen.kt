package com.leilapp.aura.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leilapp.aura.domain.longLabel
import com.leilapp.aura.domain.shortSpanish
import com.leilapp.aura.ui.auraViewModel
import com.leilapp.aura.ui.components.AuraScaffold
import com.leilapp.aura.ui.components.CenteredContent
import com.leilapp.aura.ui.components.EmptyState
import com.leilapp.aura.ui.components.SectionHeader
import com.leilapp.aura.ui.components.WarningBanner
import com.leilapp.aura.ui.theme.AuraColors
import com.leilapp.aura.ui.today.EventRow
import com.leilapp.aura.ui.today.TaskRow
import java.time.LocalDate

@Composable
fun AgendaScreen(onAdd: (epochDay: Long) -> Unit, onOpen: (Long) -> Unit, onOpenTask: (Long) -> Unit) {
    val vm = auraViewModel { EventsViewModel(it.repository) }
    val events by vm.events.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    var selected by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var showUpcoming by rememberSaveable { mutableStateOf(false) }
    val all = events.orEmpty()

    AuraScaffold(
        title = "Agenda",
        floatingActionButton = {
            FloatingActionButton(onClick = { onAdd(selected) }) { Icon(Icons.Filled.Add, contentDescription = "Nuevo evento") }
        },
    ) { padding ->
        CenteredContent(padding) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !showUpcoming, onClick = { showUpcoming = false }, label = { Text("Por día") })
                    FilterChip(selected = showUpcoming, onClick = { showUpcoming = true }, label = { Text("Próximos") })
                }
                if (!showUpcoming) {
                    DayStrip(today, selected, all.mapNotNull { if (it.completed) null else it.epochDay }.toSet(),
                        all.filter { it.forChild && !it.completed }.mapNotNull { it.epochDay }.toSet()) { selected = it }
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (showUpcoming) {
                        val upcoming = all.filter { !it.completed && it.epochDay != null && it.epochDay >= today.toEpochDay() }
                        if (upcoming.isEmpty()) {
                            item { EmptyState(Icons.Filled.DateRange, "Sin próximos compromisos", "Los eventos que agregues aparecerán aquí en orden.") }
                        }
                        upcoming.groupBy { it.epochDay!! }.forEach { (day, dayEvents) ->
                            item(key = "h$day") { SectionHeader(LocalDate.ofEpochDay(day).longLabel()) }
                            conflictsForDay(all, day).forEach { (a, b) ->
                                item { WarningBanner("Conflicto: «${a.title}» y «${b.title}»") }
                            }
                            items(dayEvents, key = { it.id }) { e -> EventRow(e, today, onClick = { onOpen(e.id) }, onDone = { vm.toggle(e) }) }
                        }
                    } else {
                        val date = LocalDate.ofEpochDay(selected)
                        item { SectionHeader(date.longLabel()) }
                        conflictsForDay(all, selected).forEach { (a, b) ->
                            item { WarningBanner("Conflicto de horario: «${a.title}» y «${b.title}» se superponen. Revisa si necesitas ajustar alguno.") }
                        }
                        val dayEvents = all.filter { it.epochDay == selected }
                            .sortedWith(compareBy({ it.completed }, { it.startMinutes ?: -1 }))
                        val dayTasks = tasks.filter { it.dueEpochDay == selected }
                        if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                            item {
                                EmptyState(
                                    Icons.Filled.DateRange, "Día libre",
                                    "No hay eventos para este día.",
                                    actionLabel = "Agregar evento", onAction = { onAdd(selected) },
                                )
                            }
                        }
                        items(dayEvents, key = { it.id }) { e -> EventRow(e, today, onClick = { onOpen(e.id) }, onDone = { vm.toggle(e) }) }
                        if (dayTasks.isNotEmpty()) {
                            item { SectionHeader("Tareas de este día") }
                            items(dayTasks, key = { "t${it.id}" }) { t -> TaskRow(t, today, onClick = { onOpenTask(t.id) }, onDone = { vm.toggleTask(t) }) }
                        }
                        if (selected != today.toEpochDay()) {
                            item { TextButton(onClick = { selected = today.toEpochDay() }) { Text("Volver a hoy") } }
                        }
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DayStrip(today: LocalDate, selected: Long, withEvents: Set<Long>, withChild: Set<Long>, onSelect: (Long) -> Unit) {
    // 7 días hacia atrás y 60 hacia adelante; para otras fechas, usar el botón + y elegir la fecha.
    val start = today.minusDays(7)
    val days = (0..67).map { start.plusDays(it.toLong()) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 5)
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(days, key = { it.toEpochDay() }) { day ->
            val epoch = day.toEpochDay()
            val isSelected = epoch == selected
            val bg = when {
                isSelected -> MaterialTheme.colorScheme.primary
                epoch == today.toEpochDay() -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            Column(
                modifier = Modifier
                    .width(52.dp)
                    .background(bg, MaterialTheme.shapes.medium)
                    .clickable { onSelect(epoch) }
                    .padding(vertical = 10.dp)
                    .semantics {
                        this.selected = isSelected
                        contentDescription = day.longLabel()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(day.dayOfWeek.shortSpanish(), style = MaterialTheme.typography.labelMedium, color = fg)
                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, color = fg)
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (epoch in withChild) Dot(AuraColors.Child)
                    if (epoch in withEvents) Dot(if (isSelected) fg else MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(6.dp).background(color, CircleShape))
}
