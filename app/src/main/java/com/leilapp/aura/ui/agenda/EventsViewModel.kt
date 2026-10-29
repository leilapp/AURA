package com.leilapp.aura.ui.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leilapp.aura.data.AuraRepository
import com.leilapp.aura.data.EventEntity
import com.leilapp.aura.data.TaskEntity
import com.leilapp.aura.domain.TimeSlot
import com.leilapp.aura.domain.findConflicts
import com.leilapp.aura.domain.slotOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EventsViewModel(private val repository: AuraRepository) : ViewModel() {
    val events: StateFlow<List<EventEntity>?> =
        repository.events.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val tasks: StateFlow<List<TaskEntity>> =
        repository.tasks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(event: EventEntity) = viewModelScope.launch { repository.setEventCompleted(event, !event.completed) }

    fun toggleTask(task: TaskEntity) = viewModelScope.launch { repository.setTaskCompleted(task, !task.completed) }

    fun delete(event: EventEntity, after: () -> Unit = {}) = viewModelScope.launch {
        repository.deleteEvent(event.id)
        after()
    }

    suspend fun load(id: Long): EventEntity? = repository.getEvent(id)

    fun save(event: EventEntity, onSaved: () -> Unit) = viewModelScope.launch {
        repository.saveEvent(event)
        onSaved()
    }
}

fun conflictsForDay(events: List<EventEntity>, epochDay: Long): List<Pair<TimeSlot, TimeSlot>> =
    findConflicts(
        events.filter { !it.completed && it.epochDay == epochDay }
            .mapNotNull { slotOf(it.id, it.title, it.epochDay, it.startMinutes, it.endMinutes) },
    )

/** Eventos que chocarían con el que se está editando (para avisar antes de guardar). */
fun conflictsWith(candidate: TimeSlot?, others: List<EventEntity>): List<EventEntity> {
    if (candidate == null) return emptyList()
    return others.filter { o ->
        val slot = slotOf(o.id, o.title, o.epochDay, o.startMinutes, o.endMinutes) ?: return@filter false
        o.id != candidate.id && !o.completed && slot.epochDay == candidate.epochDay &&
            slot.start < candidate.end && candidate.start < slot.end
    }
}
