package com.leilapp.aura.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leilapp.aura.data.AuraRepository
import com.leilapp.aura.data.Category
import com.leilapp.aura.data.TaskEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class TaskFilter(val label: String) { TODAY("Hoy"), UPCOMING("Próximas"), OVERDUE("Vencidas"), ALL("Todas"), DONE("Completadas") }

data class TasksState(
    val loading: Boolean = true,
    val tasks: List<TaskEntity> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filter: TaskFilter = TaskFilter.TODAY,
    val categoryId: Long? = null,
)

/** Filtro puro, separado para poder probarlo sin Android. */
fun filterTasks(tasks: List<TaskEntity>, filter: TaskFilter, categoryId: Long?, today: LocalDate): List<TaskEntity> {
    val day = today.toEpochDay()
    return tasks.filter { t ->
        val due = t.dueEpochDay
        val byStatus = when (filter) {
            TaskFilter.TODAY -> !t.completed && due != null && due <= day
            TaskFilter.UPCOMING -> !t.completed && (due == null || due > day)
            TaskFilter.OVERDUE -> !t.completed && due != null && due < day
            TaskFilter.ALL -> true
            TaskFilter.DONE -> t.completed
        }
        byStatus && (categoryId == null || t.categoryId == categoryId)
    }
}

class TasksViewModel(private val repository: AuraRepository) : ViewModel() {
    private val filter = MutableStateFlow(TaskFilter.TODAY)
    private val category = MutableStateFlow<Long?>(null)

    val state: StateFlow<TasksState> = combine(repository.tasks, repository.categories, filter, category) { tasks, cats, f, c ->
        TasksState(false, filterTasks(tasks, f, c, LocalDate.now()), cats, f, c)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksState())

    val categories = repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(value: TaskFilter) { filter.value = value }
    fun setCategory(value: Long?) { category.value = value }

    fun toggle(task: TaskEntity) = viewModelScope.launch { repository.setTaskCompleted(task, !task.completed) }
    fun delete(task: TaskEntity, after: () -> Unit = {}) = viewModelScope.launch {
        repository.deleteTask(task.id)
        after()
    }

    suspend fun load(id: Long): TaskEntity? = repository.getTask(id)
    fun save(task: TaskEntity, onSaved: () -> Unit) = viewModelScope.launch {
        repository.saveTask(task)
        onSaved()
    }
    fun addCategory(name: String) = viewModelScope.launch { repository.addCategory(name) }
}
