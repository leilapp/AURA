package com.leilapp.aura.ui.today

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leilapp.aura.data.EventEntity
import com.leilapp.aura.data.EventKind
import com.leilapp.aura.data.TaskEntity
import com.leilapp.aura.domain.describeWeatherCode
import com.leilapp.aura.domain.formatAmount
import com.leilapp.aura.domain.greetingFor
import com.leilapp.aura.domain.longLabel
import com.leilapp.aura.domain.minutesLabel
import com.leilapp.aura.domain.relativeLabel
import com.leilapp.aura.reminders.Notifications
import com.leilapp.aura.ui.auraViewModel
import com.leilapp.aura.ui.components.AccentStripe
import com.leilapp.aura.ui.components.AuraCard
import com.leilapp.aura.ui.components.CenteredContent
import com.leilapp.aura.ui.components.ChildPill
import com.leilapp.aura.ui.components.EmptyState
import com.leilapp.aura.ui.components.PriorityPill
import com.leilapp.aura.ui.components.SectionHeader
import com.leilapp.aura.ui.components.WarningBanner
import com.leilapp.aura.ui.theme.AuraColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

data class TodayActions(
    val addTask: () -> Unit,
    val addEvent: () -> Unit,
    val addChild: () -> Unit,
    val addReminder: () -> Unit,
    val openTask: (Long) -> Unit,
    val openEvent: (Long) -> Unit,
    val openChild: () -> Unit,
    val openTasks: () -> Unit,
    val openRoutines: () -> Unit,
    val openPayments: () -> Unit,
    val openWeather: () -> Unit,
)

@Composable
fun TodayScreen(actions: TodayActions) {
    val vm = auraViewModel { TodayViewModel(it.repository, it.settings, it.weather) }
    val state by vm.state.collectAsStateWithLifecycle()

    if (state.loading) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
        }
        return
    }

    val today = state.now.toLocalDate()
    CenteredContent(PaddingValues(0.dp)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    val name = state.userName.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""
                    Text(greetingFor(state.now.hour) + name, style = MaterialTheme.typography.displaySmall)
                    Text(today.longLabel(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(summaryLine(state), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item { QuickActions(actions) }

            if (state.showNotificationsIntro) {
                item { NotificationsIntroCard(onDone = vm::dismissNotificationsIntro) }
            }

            state.conflicts.forEach { (a, b) ->
                item { WarningBanner("Conflicto de horario: «${a.title}» y «${b.title}» se superponen.") }
            }

            item { NextCard(state) }

            // Mi hijo va primero entre las secciones de contenido.
            item { SectionHeader("Mi hijo · próximos 7 días", actionLabel = "Ver todo", onAction = actions.openChild) }
            if (state.childUpcoming.isEmpty()) {
                item {
                    AuraCard {
                        Text("Sin actividades próximas de tu hijo.", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = actions.addChild) { Text("Agregar actividad") }
                    }
                }
            } else {
                state.childUpcoming.take(4).forEach { event ->
                    item(key = "child-${event.id}") { EventRow(event, today, onClick = { actions.openEvent(event.id) }, onDone = { vm.completeEvent(event) }) }
                }
                if (state.childUpcoming.size > 4) {
                    item { TextButton(onClick = actions.openChild) { Text("Y ${state.childUpcoming.size - 4} más…") } }
                }
            }

            item { SectionHeader("Agenda de hoy") }
            val otherEvents = state.events.filterNot { it.forChild }
            if (otherEvents.isEmpty()) {
                item { AuraCard { Text("No tienes eventos para hoy.", style = MaterialTheme.typography.bodyMedium) } }
            } else {
                otherEvents.forEach { event ->
                    item(key = "event-${event.id}") { EventRow(event, today, onClick = { actions.openEvent(event.id) }, onDone = { vm.completeEvent(event) }) }
                }
            }

            item { SectionHeader("Tareas pendientes", actionLabel = "Ver todas", onAction = actions.openTasks) }
            if (state.tasks.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.CheckCircle,
                        title = "Nada pendiente para hoy",
                        message = "Cuando agregues tareas con fecha de hoy o vencidas, aparecerán aquí.",
                        actionLabel = "Nueva tarea",
                        onAction = actions.addTask,
                    )
                }
            } else {
                if (state.overdueCount > 0) {
                    item { Text("${state.overdueCount} vencida(s)", color = AuraColors.Error, style = MaterialTheme.typography.labelLarge) }
                }
                state.tasks.take(8).forEach { task ->
                    item(key = "task-${task.id}") { TaskRow(task, today, onClick = { actions.openTask(task.id) }, onDone = { vm.completeTask(task) }) }
                }
            }

            item { SectionHeader("Rutinas de hoy", actionLabel = "Administrar", onAction = actions.openRoutines) }
            item { RoutinesCard(state, onToggle = vm::toggleRoutine, onManage = actions.openRoutines) }

            if (state.paymentsDue.isNotEmpty()) {
                item { SectionHeader("Pagos próximos", actionLabel = "Ver pagos", onAction = actions.openPayments) }
                item {
                    AuraCard(onClick = actions.openPayments) {
                        state.paymentsDue.take(4).forEach { p ->
                            val due = LocalDate.ofEpochDay(p.dueEpochDay)
                            val overdue = due.isBefore(today)
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(p.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${formatAmount(p.amountCents)} · ${if (overdue) "vencido" else due.relativeLabel(today)}",
                                    color = if (overdue) AuraColors.Error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            item { SectionHeader("Clima", actionLabel = "Pronóstico", onAction = actions.openWeather) }
            item { WeatherSummary(state, actions.openWeather) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun summaryLine(state: TodayState): String {
    val parts = mutableListOf<String>()
    val events = state.events.size
    parts += if (events == 0) "sin eventos" else "$events evento(s)"
    parts += if (state.tasks.isEmpty()) "sin tareas pendientes" else "${state.tasks.size} tarea(s)"
    val pendingRoutines = state.routines.count { !it.done }
    if (state.routines.isNotEmpty()) parts += "$pendingRoutines rutina(s) por hacer"
    return "Hoy: " + parts.joinToString(" · ")
}

@Composable
private fun QuickActions(actions: TodayActions) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        QuickChip("Tarea", Icons.Filled.Add, actions.addTask)
        QuickChip("Evento", Icons.Filled.DateRange, actions.addEvent)
        QuickChip("Mi hijo", Icons.Filled.Face, actions.addChild)
        QuickChip("Recordatorio", Icons.Filled.Notifications, actions.addReminder)
    }
}

@Composable
private fun QuickChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.padding(0.dp)) },
        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}

@Composable
private fun NotificationsIntroCard(onDone: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    if (Notifications.hasPermission(context)) {
        // Ya concedido (o Android < 13): no hace falta mostrar la tarjeta.
        androidx.compose.runtime.LaunchedEffect(Unit) { onDone() }
        return
    }
    AuraCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
        Text("Activa los recordatorios", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Aura usa las notificaciones solo para avisarte de tareas, eventos, actividades de tu hijo, rutinas y pagos que tú programes.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Row {
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onDone()
            }) { Text("Permitir notificaciones") }
            TextButton(onClick = onDone) { Text("Ahora no") }
        }
    }
}

@Composable
private fun NextCard(state: TodayState) {
    val next = state.next
    AuraCard(containerColor = if (next?.forChild == true) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text("Próximo compromiso", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        if (next == null) {
            Text("No hay compromisos con hora programados.", style = MaterialTheme.typography.bodyLarge)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(next.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (next.forChild) ChildPill()
            }
            val day = next.at.toLocalDate().relativeLabel(state.now.toLocalDate())
            val place = next.location.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
            Text("$day, ${next.at.format(DateTimeFormatter.ofPattern("HH:mm"))}$place", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun EventRow(event: EventEntity, today: LocalDate, onClick: () -> Unit, onDone: (() -> Unit)? = null) {
    AuraCard(onClick = onClick, containerColor = if (event.forChild) AuraColors.ChildContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccentStripe(if (event.forChild) AuraColors.Child else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val whenText = buildString {
                    event.epochDay?.let { append(LocalDate.ofEpochDay(it).relativeLabel(today)) }
                    event.startMinutes?.let { append(", ").append(minutesLabel(it)) }
                    event.endMinutes?.let { append("–").append(minutesLabel(it)) }
                    if (event.kind != EventKind.EVENT) append(" · ").append(event.kind)
                    if (event.location.isNotBlank()) append(" · ").append(event.location)
                }
                Text(whenText.ifBlank { event.kind }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                    if (event.forChild) ChildPill()
                    PriorityPill(event.priority)
                }
            }
            if (onDone != null) {
                Checkbox(checked = event.completed, onCheckedChange = { onDone() })
            }
        }
    }
}

@Composable
fun TaskRow(task: TaskEntity, today: LocalDate, onClick: () -> Unit, onDone: () -> Unit) {
    AuraCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.completed, onCheckedChange = { onDone() })
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val due = task.dueEpochDay?.let { LocalDate.ofEpochDay(it) }
                val overdue = due != null && due.isBefore(today) && !task.completed
                val text = buildString {
                    if (due != null) append(if (overdue) "Vencida · " + due.relativeLabel(today) else due.relativeLabel(today))
                    task.dueMinutes?.let { append(", ").append(minutesLabel(it)) }
                }
                if (text.isNotBlank()) {
                    Text(text, style = MaterialTheme.typography.bodyMedium, color = if (overdue) AuraColors.Error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PriorityPill(task.priority)
        }
    }
}

@Composable
private fun RoutinesCard(state: TodayState, onToggle: (RoutineToday) -> Unit, onManage: () -> Unit) {
    AuraCard {
        if (state.routines.isEmpty()) {
            Text("No hay rutinas para hoy.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onManage) { Text("Crear una rutina") }
        } else {
            val done = state.routines.count { it.done }
            val total = state.routines.size
            Text("$done de $total completadas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            state.routines.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.done, onCheckedChange = { onToggle(item) })
                    Text(item.routine.title, modifier = Modifier.weight(1f))
                    item.routine.timeMinutes?.let {
                        Text(minutesLabel(it), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherSummary(state: TodayState, onOpen: () -> Unit) {
    val weather = state.weather
    AuraCard(onClick = onOpen) {
        if (weather == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                Text("Clima no disponible todavía. Toca para elegir tu ciudad y consultarlo.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val today = weather.days.firstOrNull { it.epochDay == state.now.toLocalDate().toEpochDay() } ?: weather.days.firstOrNull()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("${weather.temperature.roundToInt()} °C · ${describeWeatherCode(weather.weatherCode)}", style = MaterialTheme.typography.titleMedium)
                    if (today != null) {
                        val rain = today.precipitationProbability?.let { " · lluvia $it %" } ?: ""
                        Text("Mín ${today.min.roundToInt()}° / Máx ${today.max.roundToInt()}°$rain", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(weather.place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
