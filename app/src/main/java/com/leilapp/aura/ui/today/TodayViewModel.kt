package com.leilapp.aura.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leilapp.aura.data.AuraRepository
import com.leilapp.aura.data.EventEntity
import com.leilapp.aura.data.PaymentEntity
import com.leilapp.aura.data.Priority
import com.leilapp.aura.data.RoutineEntity
import com.leilapp.aura.data.SettingsRepository
import com.leilapp.aura.data.TaskEntity
import com.leilapp.aura.data.WeatherRepository
import com.leilapp.aura.domain.TimeSlot
import com.leilapp.aura.domain.WeatherSnapshot
import com.leilapp.aura.domain.Weekdays
import com.leilapp.aura.domain.findConflicts
import com.leilapp.aura.domain.slotOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class RoutineToday(val routine: RoutineEntity, val done: Boolean)

/** Un compromiso con hora (evento o tarea) para calcular "lo siguiente". */
data class Upcoming(val title: String, val at: LocalDateTime, val forChild: Boolean, val location: String)

data class TodayState(
    val loading: Boolean = true,
    val now: LocalDateTime = LocalDateTime.now(),
    val userName: String = "",
    val events: List<EventEntity> = emptyList(),
    val childUpcoming: List<EventEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val overdueCount: Int = 0,
    val routines: List<RoutineToday> = emptyList(),
    val next: Upcoming? = null,
    val conflicts: List<Pair<TimeSlot, TimeSlot>> = emptyList(),
    val paymentsDue: List<PaymentEntity> = emptyList(),
    val weather: WeatherSnapshot? = null,
    val showNotificationsIntro: Boolean = false,
    /** Progreso del día (solo lectura, para el indicador del encabezado). */
    val doneToday: Int = 0,
    val plannedToday: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repository: AuraRepository,
    private val settings: SettingsRepository,
    private val weatherRepository: WeatherRepository,
) : ViewModel() {

    init {
        // Actualiza el clima en segundo plano si la caché tiene más de una hora. Si falla, no pasa nada.
        viewModelScope.launch { weatherRepository.refresh() }
    }

    /** Reloj que avanza cada minuto para que el saludo y "lo siguiente" se mantengan al día. */
    private val clock = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(60_000)
        }
    }

    private val routinesToday = clock.map { it.toLocalDate() }
        .flatMapLatest { day ->
            combine(repository.routines, repository.routineCompletions(day)) { routines, done ->
                val doneIds = done.map { it.routineId }.toSet()
                routines.filter { it.active && Weekdays.contains(it.daysMask, day.dayOfWeek) }
                    .map { RoutineToday(it, it.id in doneIds) }
            }
        }

    val state: StateFlow<TodayState> = combine(
        clock,
        combine(repository.events, repository.tasks, repository.payments, ::Triple),
        routinesToday,
        settings.settings,
        weatherRepository.snapshot,
    ) { now, (events, tasks, payments), routines, prefs, weather ->
        val today = now.toLocalDate()
        val todayDay = today.toEpochDay()
        val nowMinutes = now.hour * 60 + now.minute

        // Lo de "Mi hijo" primero, luego prioridad, luego hora.
        val todayEvents = events.filter { it.epochDay == todayDay && !it.completed }
            .sortedWith(compareByDescending<EventEntity> { it.forChild }.thenByDescending { it.priority }.thenBy { it.startMinutes ?: Int.MAX_VALUE })

        val childUpcoming = events.filter {
            it.forChild && !it.completed && it.epochDay != null && it.epochDay in todayDay..todayDay + 7
        }.sortedWith(compareBy<EventEntity> { it.epochDay }.thenByDescending { it.priority }.thenBy { it.startMinutes ?: Int.MAX_VALUE })

        val openTasks = tasks.filter { !it.completed }
        val dueTasks = openTasks.filter { it.dueEpochDay != null && it.dueEpochDay <= todayDay }
            .sortedWith(compareByDescending<TaskEntity> { it.priority == Priority.HIGH }.thenBy { it.dueEpochDay }.thenBy { it.dueMinutes ?: Int.MAX_VALUE })

        val candidates = events.filter { !it.completed && it.epochDay != null && it.startMinutes != null }
            .map { Upcoming(it.title, LocalDate.ofEpochDay(it.epochDay!!).atStartOfDay().plusMinutes(it.startMinutes!!.toLong()), it.forChild, it.location) } +
            openTasks.filter { it.dueEpochDay != null && it.dueMinutes != null }
                .map { Upcoming(it.title, LocalDate.ofEpochDay(it.dueEpochDay!!).atStartOfDay().plusMinutes(it.dueMinutes!!.toLong()), false, "") }
        val next = candidates.filter { it.at.isAfter(now) }.minByOrNull { it.at }

        val slots = events.filter { !it.completed }.mapNotNull { slotOf(it.id, it.title, it.epochDay, it.startMinutes, it.endMinutes) }
            .filter { it.epochDay == todayDay }

        // Progreso visual: eventos de hoy + tareas que vencen hoy + rutinas de hoy.
        val eventsOfDay = events.filter { it.epochDay == todayDay }
        val tasksOfDay = tasks.filter { it.dueEpochDay == todayDay }

        TodayState(
            loading = false,
            now = now,
            userName = prefs.userName,
            events = todayEvents,
            childUpcoming = childUpcoming,
            tasks = dueTasks,
            overdueCount = dueTasks.count { it.dueEpochDay!! < todayDay || (it.dueEpochDay == todayDay && (it.dueMinutes ?: Int.MAX_VALUE) < nowMinutes) },
            routines = routines,
            next = next,
            conflicts = findConflicts(slots),
            paymentsDue = payments.filter { !it.paid && it.dueEpochDay <= todayDay + 7 },
            weather = weather,
            showNotificationsIntro = !prefs.notificationsIntroSeen,
            doneToday = eventsOfDay.count { it.completed } + tasksOfDay.count { it.completed } + routines.count { it.done },
            plannedToday = eventsOfDay.size + tasksOfDay.size + routines.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayState())

    fun toggleRoutine(item: RoutineToday) = viewModelScope.launch {
        repository.setRoutineDone(item.routine.id, LocalDate.now(), !item.done)
    }

    fun completeTask(task: TaskEntity) = viewModelScope.launch { repository.setTaskCompleted(task, true) }

    fun completeEvent(event: EventEntity) = viewModelScope.launch { repository.setEventCompleted(event, true) }

    fun dismissNotificationsIntro() = viewModelScope.launch { settings.setNotificationsIntroSeen() }
}
