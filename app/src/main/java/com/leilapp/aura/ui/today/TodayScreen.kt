package com.leilapp.aura.ui.today

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leilapp.aura.data.EventEntity
import com.leilapp.aura.data.EventKind
import com.leilapp.aura.data.PaymentEntity
import com.leilapp.aura.data.Priority
import com.leilapp.aura.data.TaskEntity
import com.leilapp.aura.domain.SPANISH
import com.leilapp.aura.domain.describeWeatherCode
import com.leilapp.aura.domain.formatAmount
import com.leilapp.aura.domain.greetingFor
import com.leilapp.aura.domain.longLabel
import com.leilapp.aura.domain.minutesLabel
import com.leilapp.aura.domain.relativeLabel
import com.leilapp.aura.domain.shortSpanish
import com.leilapp.aura.reminders.Notifications
import com.leilapp.aura.ui.auraViewModel
import com.leilapp.aura.ui.components.AccentStripe
import com.leilapp.aura.ui.components.AuraCard
import com.leilapp.aura.ui.components.CenteredContent
import com.leilapp.aura.ui.components.ChildPill
import com.leilapp.aura.ui.components.EmptyState
import com.leilapp.aura.ui.components.Eyebrow
import com.leilapp.aura.ui.components.GroupDivider
import com.leilapp.aura.ui.components.GroupPosition
import com.leilapp.aura.ui.components.GroupedRow
import com.leilapp.aura.ui.components.IconBadge
import com.leilapp.aura.ui.components.ListRowContent
import com.leilapp.aura.ui.components.Pill
import com.leilapp.aura.ui.components.PriorityPill
import com.leilapp.aura.ui.components.ProgressRing
import com.leilapp.aura.ui.components.QuickActionTile
import com.leilapp.aura.ui.components.SectionHeader
import com.leilapp.aura.ui.components.SkeletonBlock
import com.leilapp.aura.ui.components.WarningBanner
import com.leilapp.aura.ui.theme.AuraColors
import com.leilapp.aura.ui.theme.AuraSpacing
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
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
    /** Abre Ajustes desde el avatar del encabezado (donde se configura el nombre). */
    val openSettings: () -> Unit = {},
)

private const val CHILD_PREVIEW = 4
private const val TASK_PREVIEW = 8

@Composable
fun TodayScreen(actions: TodayActions) {
    val vm = auraViewModel { TodayViewModel(it.repository, it.settings, it.weather) }
    val state by vm.state.collectAsStateWithLifecycle()

    if (state.loading) {
        TodaySkeleton()
        return
    }

    val today = state.now.toLocalDate()
    val nowMinutes = state.now.hour * 60 + state.now.minute
    CenteredContent(PaddingValues(0.dp)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = AuraSpacing.screen, end = AuraSpacing.screen, top = 8.dp, bottom = 32.dp),
        ) {
            item(key = "header") { Header(state, onAvatar = actions.openSettings) }

            item(key = "quick") { QuickActions(actions, Modifier.padding(top = AuraSpacing.xl)) }

            if (state.showNotificationsIntro) {
                item(key = "notif") {
                    NotificationsIntroCard(onDone = vm::dismissNotificationsIntro, modifier = Modifier.padding(top = AuraSpacing.md).animateItem())
                }
            }

            state.conflicts.forEachIndexed { i, (a, b) ->
                item(key = "conflict-$i") {
                    WarningBanner(
                        "Conflicto de horario: «${a.title}» y «${b.title}» se superponen.",
                        modifier = Modifier.padding(top = AuraSpacing.sm),
                    )
                }
            }

            item(key = "next") { NextCard(state, onAdd = actions.addEvent, modifier = Modifier.padding(top = AuraSpacing.lg)) }

            // Mi hijo va primero entre las secciones de contenido.
            item(key = "child-header") {
                SectionHeader(
                    "Mi hijo",
                    supporting = "Próximos 7 días",
                    actionLabel = "Ver todo",
                    onAction = actions.openChild,
                    leading = { IconBadge(Icons.Outlined.Face, tint = AuraColors.Child, size = 32.dp, iconSize = 17.dp) },
                    modifier = Modifier.padding(top = AuraSpacing.md),
                )
            }
            item(key = "child-section") {
                ChildSection(
                    events = state.childUpcoming,
                    today = today,
                    onOpen = actions.openEvent,
                    onDone = vm::completeEvent,
                    onAdd = actions.addChild,
                    onSeeAll = actions.openChild,
                    modifier = Modifier.padding(top = AuraSpacing.sm),
                )
            }

            // Agenda de hoy en formato línea de tiempo (lo de "Mi hijo" ya aparece arriba).
            val agenda = state.events.filterNot { it.forChild }
                .sortedWith(compareBy<EventEntity> { it.startMinutes ?: -1 }.thenByDescending { it.priority })
            item(key = "agenda-header") {
                SectionHeader(
                    "Agenda de hoy",
                    supporting = if (agenda.isEmpty()) null else countLabel(agenda.size, "evento", "eventos"),
                    modifier = Modifier.padding(top = AuraSpacing.md),
                )
            }
            if (agenda.isEmpty()) {
                item(key = "agenda-empty") {
                    InlineEmpty(
                        icon = Icons.Outlined.DateRange,
                        text = "No tienes eventos para hoy.",
                        actionLabel = "Nuevo evento",
                        onAction = actions.addEvent,
                        modifier = Modifier.padding(top = AuraSpacing.sm),
                    )
                }
            } else {
                agenda.forEachIndexed { i, event ->
                    item(key = "event-${event.id}") {
                        TimelineEventRow(
                            event = event,
                            nowMinutes = nowMinutes,
                            position = GroupPosition.of(i, agenda.size),
                            onClick = { actions.openEvent(event.id) },
                            onDone = { vm.completeEvent(event) },
                            modifier = Modifier.padding(top = if (i == 0) AuraSpacing.sm else 0.dp).animateItem(),
                        )
                    }
                }
            }

            item(key = "tasks-header") {
                val supporting = when {
                    state.tasks.isEmpty() -> null
                    state.overdueCount > 0 -> countLabel(state.tasks.size, "pendiente", "pendientes") + " · " +
                        countLabel(state.overdueCount, "vencida", "vencidas")
                    else -> countLabel(state.tasks.size, "pendiente", "pendientes")
                }
                SectionHeader(
                    "Tareas pendientes",
                    supporting = supporting,
                    actionLabel = "Ver todas",
                    onAction = actions.openTasks,
                    modifier = Modifier.padding(top = AuraSpacing.md),
                )
            }
            if (state.tasks.isEmpty()) {
                item(key = "tasks-empty") {
                    AuraCard(modifier = Modifier.padding(top = AuraSpacing.sm), contentPadding = PaddingValues(0.dp)) {
                        EmptyState(
                            icon = Icons.Outlined.CheckCircle,
                            title = "Todo en orden por hoy",
                            message = "No hay tareas para hoy ni vencidas. Disfruta el respiro.",
                            actionLabel = "Nueva tarea",
                            onAction = actions.addTask,
                            compact = true,
                        )
                    }
                }
            } else {
                val shown = state.tasks.take(TASK_PREVIEW)
                shown.forEachIndexed { i, task ->
                    item(key = "task-${task.id}") {
                        CompactTaskRow(
                            task = task,
                            today = today,
                            position = GroupPosition.of(i, shown.size),
                            onClick = { actions.openTask(task.id) },
                            onDone = { vm.completeTask(task) },
                            modifier = Modifier.padding(top = if (i == 0) AuraSpacing.sm else 0.dp).animateItem(),
                        )
                    }
                }
                if (state.tasks.size > TASK_PREVIEW) {
                    item(key = "tasks-more") {
                        TextButton(onClick = actions.openTasks) { Text("Ver ${state.tasks.size - TASK_PREVIEW} más") }
                    }
                }
            }

            item(key = "routines-header") {
                SectionHeader("Rutinas de hoy", actionLabel = "Administrar", onAction = actions.openRoutines, modifier = Modifier.padding(top = AuraSpacing.md))
            }
            item(key = "routines") {
                RoutinesCard(state, onToggle = vm::toggleRoutine, onManage = actions.openRoutines, modifier = Modifier.padding(top = AuraSpacing.sm))
            }

            if (state.paymentsDue.isNotEmpty()) {
                item(key = "payments-header") {
                    SectionHeader("Pagos próximos", actionLabel = "Ver pagos", onAction = actions.openPayments, modifier = Modifier.padding(top = AuraSpacing.md))
                }
                val payments = state.paymentsDue.take(4)
                payments.forEachIndexed { i, p ->
                    item(key = "payment-${p.id}") {
                        PaymentRow(
                            payment = p,
                            today = today,
                            position = GroupPosition.of(i, payments.size),
                            onClick = actions.openPayments,
                            modifier = Modifier.padding(top = if (i == 0) AuraSpacing.sm else 0.dp).animateItem(),
                        )
                    }
                }
            }

            item(key = "weather-header") {
                SectionHeader("Clima", actionLabel = "Pronóstico", onAction = actions.openWeather, modifier = Modifier.padding(top = AuraSpacing.md))
            }
            item(key = "weather") { WeatherSummary(state, actions.openWeather, Modifier.padding(top = AuraSpacing.sm)) }
        }
    }
}

private fun countLabel(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"

private fun summaryLine(state: TodayState): String {
    val parts = mutableListOf<String>()
    val events = state.events.size
    parts += if (events == 0) "Sin eventos" else countLabel(events, "evento", "eventos")
    parts += if (state.tasks.isEmpty()) "sin tareas pendientes" else countLabel(state.tasks.size, "tarea", "tareas")
    val pendingRoutines = state.routines.count { !it.done }
    if (state.routines.isNotEmpty()) parts += countLabel(pendingRoutines, "rutina por hacer", "rutinas por hacer")
    return parts.joinToString(" · ")
}

// ---------- Encabezado ----------

@Composable
private fun Header(state: TodayState, onAvatar: () -> Unit) {
    val today = state.now.toLocalDate()
    val name = state.userName.trim()
    val progress = if (state.plannedToday == 0) 0f else state.doneToday.toFloat() / state.plannedToday
    Row(Modifier.fillMaxWidth().padding(top = AuraSpacing.md), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(today.longLabel(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                greetingFor(state.now.hour) + if (name.isNotEmpty()) ", $name" else "",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                summaryLine(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(AuraSpacing.md))
        val description = if (state.plannedToday == 0) "Perfil y ajustes" else
            "Progreso de hoy: ${state.doneToday} de ${state.plannedToday}. Abrir perfil y ajustes"
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ProgressRing(
                progress = progress,
                size = 54.dp,
                stroke = 3.dp,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onAvatar)
                    .semantics { contentDescription = description },
            ) {
                Box(
                    Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (name.isNotEmpty()) {
                        Text(
                            name.first().uppercase(SPANISH),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
                    }
                }
            }
            if (state.plannedToday > 0) {
                Text(
                    "${(progress * 100).roundToInt()} %",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

// ---------- Acciones rápidas ----------

@Composable
private fun QuickActions(actions: TodayActions, modifier: Modifier = Modifier) {
    val items = listOf(
        QuickItem("Tarea", Icons.Outlined.CheckCircle, null, actions.addTask),
        QuickItem("Evento", Icons.Outlined.DateRange, null, actions.addEvent),
        QuickItem("Mi hijo", Icons.Outlined.Face, AuraColors.Child, actions.addChild),
        QuickItem("Recordatorio", Icons.Outlined.Notifications, null, actions.addReminder),
    )
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // En pantallas angostas se reparten en 2×2 para que ninguna etiqueta se corte.
        val columns = if (maxWidth >= 360.dp) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
                    row.forEach { item ->
                        QuickActionTile(
                            label = item.label,
                            icon = item.icon,
                            onClick = item.onClick,
                            tint = item.tint ?: MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private data class QuickItem(val label: String, val icon: ImageVector, val tint: Color?, val onClick: () -> Unit)

// ---------- Permiso de notificaciones ----------

@Composable
private fun NotificationsIntroCard(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    if (Notifications.hasPermission(context)) {
        // Ya concedido (o Android < 13): no hace falta mostrar la tarjeta.
        androidx.compose.runtime.LaunchedEffect(Unit) { onDone() }
        return
    }
    AuraCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(Icons.Outlined.Notifications)
            Spacer(Modifier.width(AuraSpacing.md))
            Column(Modifier.weight(1f)) {
                Text("Activa los recordatorios", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Aura usa las notificaciones solo para avisarte de tareas, eventos, actividades de tu hijo, rutinas y pagos que tú programes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDone) { Text("Ahora no", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else onDone()
            }) { Text("Permitir notificaciones") }
        }
    }
}

// ---------- Próximo compromiso ----------

/** "En 25 min", "En 2 h 10 min" o null si falta más de un día. */
private fun countdown(now: LocalDateTime, at: LocalDateTime): String? {
    val minutes = Duration.between(now, at).toMinutes()
    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "En $minutes min"
        minutes < 24 * 60 -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) "En $h h" else "En $h h $m min"
        }
        else -> null
    }
}

@Composable
private fun NextCard(state: TodayState, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val next = state.next
    if (next == null) {
        InlineEmpty(
            icon = Icons.Outlined.DateRange,
            title = "Sin compromisos próximos",
            text = "Tu agenda está despejada. Buen momento para planear algo.",
            actionLabel = "Agendar",
            onAction = onAdd,
            modifier = modifier,
        )
        return
    }
    val accent = if (next.forChild) AuraColors.Child else MaterialTheme.colorScheme.primary
    val glow = if (next.forChild) AuraColors.ChildContainer else MaterialTheme.colorScheme.primaryContainer
    val shape = MaterialTheme.shapes.large
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            // Degradado muy suave: da profundidad sin parecer un banner.
            .background(Brush.linearGradient(listOf(glow.copy(alpha = 0.55f), Color.Transparent)))
            .border(1.dp, accent.copy(alpha = 0.22f), shape)
            .padding(18.dp)
            .animateContentSize(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("Próximo compromiso", color = accent, modifier = Modifier.weight(1f))
            countdown(state.now, next.at)?.let { Pill(it, color = accent, container = accent.copy(alpha = 0.14f)) }
        }
        Text(
            next.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 10.dp),
        )
        FlowRow(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val day = next.at.toLocalDate().relativeLabel(state.now.toLocalDate())
            MetaItem(Icons.Outlined.DateRange, "$day · ${next.at.format(DateTimeFormatter.ofPattern("HH:mm"))}")
            if (next.location.isNotBlank()) MetaItem(Icons.Outlined.Place, next.location)
            if (next.forChild) ChildPill()
        }
    }
}

@Composable
private fun MetaItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Estado vacío en línea: una sola fila, sin ocupar media pantalla. */
@Composable
private fun InlineEmpty(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    border: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(container)
            .border(1.dp, border, MaterialTheme.shapes.large)
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, tint = tint)
        Spacer(Modifier.width(AuraSpacing.md))
        Column(Modifier.weight(1f)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, color = tint) }
        }
    }
}

// ---------- Mi hijo ----------

private fun kindIcon(kind: String): ImageVector = when (kind) {
    EventKind.SCHOOL -> Icons.Outlined.Edit
    EventKind.ACTIVITY -> Icons.Outlined.Star
    EventKind.APPOINTMENT -> Icons.Outlined.Person
    EventKind.MATERIAL -> Icons.Outlined.Build
    EventKind.TO_BUY -> Icons.Outlined.ShoppingCart
    EventKind.IMPORTANT_DATE -> Icons.Outlined.Favorite
    EventKind.REMINDER -> Icons.Outlined.Notifications
    else -> Icons.Outlined.DateRange
}

@Composable
private fun ChildSection(
    events: List<EventEntity>,
    today: LocalDate,
    onOpen: (Long) -> Unit,
    onDone: (EventEntity) -> Unit,
    onAdd: () -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (events.isEmpty()) {
        InlineEmpty(
            icon = Icons.Outlined.Face,
            text = "Sin actividades de tu hijo en los próximos 7 días.",
            actionLabel = "Agregar",
            onAction = onAdd,
            tint = AuraColors.Child,
            container = AuraColors.ChildSurface,
            border = AuraColors.Child.copy(alpha = 0.16f),
            modifier = modifier,
        )
        return
    }
    val shape = MaterialTheme.shapes.large
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AuraColors.ChildSurface)
            .border(1.dp, AuraColors.Child.copy(alpha = 0.16f), shape)
            .animateContentSize(),
    ) {
        // Resumen por tipo (escolar, actividad, recordatorio…) con datos reales de la semana.
        val byKind = events.groupingBy { it.kind }.eachCount().entries.sortedByDescending { it.value }
        FlowRow(
            Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            byKind.forEach { (kind, count) ->
                Row(
                    Modifier
                        .background(AuraColors.ChildContainer.copy(alpha = 0.7f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(kindIcon(kind), contentDescription = null, tint = AuraColors.Child, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("$kind · $count", style = MaterialTheme.typography.labelMedium, color = Color(0xFFFCDCC8))
                }
            }
        }
        val shown = events.take(CHILD_PREVIEW)
        shown.forEachIndexed { i, event ->
            if (i > 0) GroupDivider(startIndent = 70.dp)
            ChildActivityRow(event, today, onClick = { onOpen(event.id) }, onDone = { onDone(event) })
        }
        if (events.size > CHILD_PREVIEW) {
            GroupDivider()
            TextButton(onClick = onSeeAll, modifier = Modifier.fillMaxWidth()) {
                Text("Ver ${events.size - CHILD_PREVIEW} actividades más", color = AuraColors.Child)
            }
        }
    }
}

@Composable
private fun ChildActivityRow(event: EventEntity, today: LocalDate, onClick: () -> Unit, onDone: () -> Unit) {
    val date = event.epochDay?.let { LocalDate.ofEpochDay(it) }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Bloque de fecha: lectura rápida del día.
        Column(
            Modifier
                .size(width = 44.dp, height = 48.dp)
                .background(AuraColors.ChildContainer, MaterialTheme.shapes.small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (date != null) {
                Text(
                    if (date == today) "HOY" else date.dayOfWeek.shortSpanish().uppercase(SPANISH),
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraColors.Child,
                )
                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(kindIcon(event.kind), contentDescription = null, tint = AuraColors.Child, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.width(AuraSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = buildList {
                event.startMinutes?.let { start ->
                    add(minutesLabel(start) + (event.endMinutes?.let { "–" + minutesLabel(it) } ?: ""))
                }
                add(event.kind)
                if (event.location.isNotBlank()) add(event.location)
            }.joinToString(" · ")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(kindIcon(event.kind), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (event.priority == Priority.HIGH) {
                PriorityPill(event.priority)
            }
        }
        Checkbox(
            checked = event.completed,
            onCheckedChange = { onDone() },
            colors = CheckboxDefaults.colors(checkedColor = AuraColors.Child, uncheckedColor = AuraColors.Child.copy(alpha = 0.6f)),
        )
    }
}

// ---------- Agenda de hoy ----------

private enum class SlotState { ALL_DAY, PAST, NOW, LATER }

private fun slotState(event: EventEntity, nowMinutes: Int): SlotState {
    val start = event.startMinutes ?: return SlotState.ALL_DAY
    val end = event.endMinutes
    return when {
        end != null && nowMinutes in start until end -> SlotState.NOW
        nowMinutes >= (end ?: start) -> SlotState.PAST
        else -> SlotState.LATER
    }
}

@Composable
private fun TimelineEventRow(
    event: EventEntity,
    nowMinutes: Int,
    position: GroupPosition,
    onClick: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val slot = slotState(event, nowMinutes)
    val primary = MaterialTheme.colorScheme.primary
    val barColor = when (slot) {
        SlotState.NOW -> primary
        SlotState.LATER -> primary.copy(alpha = 0.45f)
        SlotState.ALL_DAY -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)
        SlotState.PAST -> MaterialTheme.colorScheme.outline
    }
    val faded = slot == SlotState.PAST
    GroupedRow(position, modifier = modifier, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Columna de horario.
            Column(Modifier.width(50.dp)) {
                if (event.startMinutes != null) {
                    Text(
                        minutesLabel(event.startMinutes),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (faded) AuraColors.OnSurfaceFaint else MaterialTheme.colorScheme.onSurface,
                    )
                    event.endMinutes?.let {
                        Text(minutesLabel(it), style = MaterialTheme.typography.bodySmall, color = AuraColors.OnSurfaceFaint)
                    }
                } else {
                    Text("Todo\nel día", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(Modifier.width(3.dp).fillMaxHeight().heightIn(min = 32.dp).background(barColor, RoundedCornerShape(2.dp)))
            Spacer(Modifier.width(AuraSpacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (faded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildList {
                    if (event.kind != EventKind.EVENT) add(event.kind)
                    if (event.location.isNotBlank()) add(event.location)
                }.joinToString(" · ")
                if (meta.isNotEmpty()) {
                    Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (slot == SlotState.NOW || event.priority != Priority.NORMAL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                        if (slot == SlotState.NOW) Pill("En curso", color = primary, container = primary.copy(alpha = 0.16f))
                        PriorityPill(event.priority)
                    }
                }
            }
            Checkbox(checked = event.completed, onCheckedChange = { onDone() })
        }
    }
}

// ---------- Tareas ----------

@Composable
private fun priorityColor(priority: Int): Color = when (priority) {
    Priority.HIGH -> AuraColors.Error
    Priority.LOW -> AuraColors.OnSurfaceFaint
    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
}

@Composable
private fun CompactTaskRow(
    task: TaskEntity,
    today: LocalDate,
    position: GroupPosition,
    onClick: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val due = task.dueEpochDay?.let { LocalDate.ofEpochDay(it) }
    val overdue = due != null && due.isBefore(today) && !task.completed
    GroupedRow(position, modifier = modifier, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(start = 0.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Franja de prioridad en el borde izquierdo.
            Box(Modifier.width(3.dp).fillMaxHeight().padding(vertical = 12.dp).background(priorityColor(task.priority), RoundedCornerShape(2.dp)))
            Checkbox(checked = task.completed, onCheckedChange = { onDone() })
            Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
                Text(task.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val text = buildString {
                    if (due != null) append(if (overdue) "Vencida · " + due.relativeLabel(today) else due.relativeLabel(today))
                    task.dueMinutes?.let { append(", ").append(minutesLabel(it)) }
                }
                if (text.isNotBlank()) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) AuraColors.Error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (overdue) FontWeight.Medium else null,
                    )
                }
            }
            if (task.priority == Priority.HIGH) {
                Pill("Alta", AuraColors.Error, AuraColors.ErrorContainer)
            }
        }
    }
}

// ---------- Filas compartidas con Agenda, Mi hijo y Tareas (mismo contrato) ----------

@Composable
fun EventRow(event: EventEntity, today: LocalDate, onClick: () -> Unit, onDone: (() -> Unit)? = null) {
    AuraCard(
        onClick = onClick,
        containerColor = if (event.forChild) AuraColors.ChildSurface else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, if (event.forChild) AuraColors.Child.copy(alpha = 0.16f) else MaterialTheme.colorScheme.outlineVariant),
        contentPadding = PaddingValues(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccentStripe(if (event.forChild) AuraColors.Child else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val whenText = buildString {
                    event.epochDay?.let { append(LocalDate.ofEpochDay(it).relativeLabel(today)) }
                    event.startMinutes?.let { append(", ").append(minutesLabel(it)) }
                    event.endMinutes?.let { append("–").append(minutesLabel(it)) }
                    if (event.kind != EventKind.EVENT) append(" · ").append(event.kind)
                    if (event.location.isNotBlank()) append(" · ").append(event.location)
                }
                Text(whenText.ifBlank { event.kind }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (event.forChild || event.priority != Priority.NORMAL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                        if (event.forChild) ChildPill()
                        PriorityPill(event.priority)
                    }
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
    AuraCard(onClick = onClick, contentPadding = PaddingValues(start = 4.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.completed, onCheckedChange = { onDone() })
            Column(Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val due = task.dueEpochDay?.let { LocalDate.ofEpochDay(it) }
                val overdue = due != null && due.isBefore(today) && !task.completed
                val text = buildString {
                    if (due != null) append(if (overdue) "Vencida · " + due.relativeLabel(today) else due.relativeLabel(today))
                    task.dueMinutes?.let { append(", ").append(minutesLabel(it)) }
                }
                if (text.isNotBlank()) {
                    Text(text, style = MaterialTheme.typography.bodySmall, color = if (overdue) AuraColors.Error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PriorityPill(task.priority)
        }
    }
}

// ---------- Rutinas, pagos y clima ----------

@Composable
private fun RoutinesCard(state: TodayState, onToggle: (RoutineToday) -> Unit, onManage: () -> Unit, modifier: Modifier = Modifier) {
    if (state.routines.isEmpty()) {
        InlineEmpty(
            icon = Icons.Outlined.Refresh,
            text = "No hay rutinas para hoy.",
            actionLabel = "Crear rutina",
            onAction = onManage,
            modifier = modifier,
        )
        return
    }
    AuraCard(modifier = modifier, contentPadding = PaddingValues(start = 4.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)) {
        val done = state.routines.count { it.done }
        val total = state.routines.size
        Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$done de $total completadas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
            Text("${(done * 100f / total).roundToInt()} %", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { done.toFloat() / total },
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 4.dp).height(6.dp),
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
        state.routines.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.done, onCheckedChange = { onToggle(item) })
                Text(
                    item.routine.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                item.routine.timeMinutes?.let {
                    Text(minutesLabel(it), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: PaymentEntity, today: LocalDate, position: GroupPosition, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val due = LocalDate.ofEpochDay(payment.dueEpochDay)
    val overdue = due.isBefore(today)
    GroupedRow(position, modifier = modifier, onClick = onClick) {
        ListRowContent(
            title = payment.name,
            supporting = if (overdue) "Vencido · ${due.relativeLabel(today)}" else due.relativeLabel(today),
            supportingColor = if (overdue) AuraColors.Error else MaterialTheme.colorScheme.onSurfaceVariant,
            leading = { IconBadge(Icons.Outlined.Info, tint = if (overdue) AuraColors.Error else MaterialTheme.colorScheme.primary, size = 32.dp, iconSize = 16.dp) },
            trailing = { Text(formatAmount(payment.amountCents), style = MaterialTheme.typography.titleSmall) },
        )
    }
}

@Composable
private fun WeatherSummary(state: TodayState, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val weather = state.weather
    if (weather == null) {
        InlineEmpty(
            icon = Icons.Outlined.Place,
            text = "Clima no disponible todavía. Elige tu ciudad para consultarlo.",
            actionLabel = "Elegir",
            onAction = onOpen,
            modifier = modifier,
        )
        return
    }
    val today = weather.days.firstOrNull { it.epochDay == state.now.toLocalDate().toEpochDay() } ?: weather.days.firstOrNull()
    AuraCard(onClick = onOpen, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${weather.temperature.roundToInt()}°", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(AuraSpacing.lg))
            Column(Modifier.weight(1f)) {
                Text(describeWeatherCode(weather.weatherCode), style = MaterialTheme.typography.titleSmall)
                if (today != null) {
                    val rain = today.precipitationProbability?.let { " · lluvia $it %" } ?: ""
                    Text("Mín ${today.min.roundToInt()}° / Máx ${today.max.roundToInt()}°$rain", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(weather.place, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(96.dp))
            }
        }
    }
}

// ---------- Carga ----------

@Composable
private fun TodaySkeleton() {
    CenteredContent(PaddingValues(0.dp)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = AuraSpacing.screen, vertical = 20.dp)
                .semantics { contentDescription = "Cargando tu día" },
            verticalArrangement = Arrangement.spacedBy(AuraSpacing.md),
        ) {
            SkeletonBlock(Modifier.width(140.dp), height = 14.dp)
            SkeletonBlock(Modifier.width(240.dp), height = 30.dp)
            Spacer(Modifier.height(AuraSpacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(AuraSpacing.sm)) {
                repeat(4) { SkeletonBlock(Modifier.weight(1f), height = 76.dp, shape = MaterialTheme.shapes.medium) }
            }
            SkeletonBlock(height = 120.dp, shape = MaterialTheme.shapes.large)
            SkeletonBlock(Modifier.width(120.dp), height = 16.dp)
            SkeletonBlock(height = 140.dp, shape = MaterialTheme.shapes.large)
        }
    }
}
