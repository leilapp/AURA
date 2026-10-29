package com.leilapp.aura.data

import android.content.Context
import com.leilapp.aura.domain.Weekdays
import com.leilapp.aura.domain.formatAmount
import com.leilapp.aura.domain.minutesLabel
import com.leilapp.aura.domain.reminderAt
import com.leilapp.aura.reminders.Notifications
import com.leilapp.aura.reminders.ReminderScheduler
import com.leilapp.aura.reminders.ReminderType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

/** Punto único de acceso a los datos locales. Cada cambio mantiene sincronizados los recordatorios. */
class AuraRepository(
    private val context: Context,
    private val db: AuraDatabase,
    private val scheduler: ReminderScheduler,
) {
    val categories: Flow<List<Category>> = db.categoryDao().observeAll()
    val tasks: Flow<List<TaskEntity>> = db.taskDao().observeAll()
    val events: Flow<List<EventEntity>> = db.eventDao().observeAll()
    val routines: Flow<List<RoutineEntity>> = db.routineDao().observeAll()
    val shoppingLists: Flow<List<ShoppingList>> = db.shoppingDao().observeLists()
    val shoppingItems: Flow<List<ShoppingItem>> = db.shoppingDao().observeItems()
    val payments: Flow<List<PaymentEntity>> = db.paymentDao().observeAll()

    fun routineCompletions(day: LocalDate): Flow<List<RoutineCompletion>> =
        db.routineDao().observeCompletions(day.toEpochDay())

    suspend fun ensureDefaultCategories() {
        if (db.categoryDao().count() == 0) {
            db.categoryDao().insertAll(AuraDatabase.DEFAULT_CATEGORIES.map { Category(name = it, builtIn = true) })
        }
    }

    // ---------- Categorías ----------
    suspend fun addCategory(name: String) {
        db.categoryDao().insert(Category(name = name.trim()))
    }

    suspend fun deleteCategory(category: Category) {
        db.taskDao().clearCategory(category.id)
        db.categoryDao().delete(category)
    }

    // ---------- Tareas ----------
    suspend fun getTask(id: Long) = db.taskDao().get(id)

    suspend fun saveTask(task: TaskEntity): Long {
        val toSave = task.copy(updatedAt = System.currentTimeMillis())
        val id = db.taskDao().upsert(toSave).let { if (task.id == 0L) it else task.id }
        scheduleTask(toSave.copy(id = id))
        return id
    }

    suspend fun setTaskCompleted(task: TaskEntity, completed: Boolean) {
        db.taskDao().setCompleted(task.id, completed)
        scheduleTask(task.copy(completed = completed))
    }

    suspend fun deleteTask(id: Long) {
        db.taskDao().delete(id)
        scheduler.cancel(ReminderType.TASK, id)
    }

    private fun scheduleTask(task: TaskEntity) {
        val at = if (task.completed || task.dueEpochDay == null) null
        else reminderAt(task.dueEpochDay, task.dueMinutes, task.reminderMinutesBefore, LocalDateTime.now())
        if (at == null) scheduler.cancel(ReminderType.TASK, task.id) else scheduler.schedule(ReminderType.TASK, task.id, at)
    }

    // ---------- Eventos y Mi hijo ----------
    suspend fun getEvent(id: Long) = db.eventDao().get(id)

    suspend fun saveEvent(event: EventEntity): Long {
        val toSave = event.copy(updatedAt = System.currentTimeMillis())
        val id = db.eventDao().upsert(toSave).let { if (event.id == 0L) it else event.id }
        scheduleEvent(toSave.copy(id = id))
        return id
    }

    suspend fun setEventCompleted(event: EventEntity, completed: Boolean) {
        db.eventDao().setCompleted(event.id, completed)
        scheduleEvent(event.copy(completed = completed))
    }

    suspend fun deleteEvent(id: Long) {
        db.eventDao().delete(id)
        scheduler.cancel(ReminderType.EVENT, id)
    }

    private fun scheduleEvent(event: EventEntity) {
        val at = if (event.completed || event.epochDay == null) null
        else reminderAt(event.epochDay, event.startMinutes, event.reminderMinutesBefore, LocalDateTime.now())
        if (at == null) scheduler.cancel(ReminderType.EVENT, event.id) else scheduler.schedule(ReminderType.EVENT, event.id, at)
    }

    // ---------- Rutinas ----------
    suspend fun getRoutine(id: Long) = db.routineDao().get(id)

    suspend fun saveRoutine(routine: RoutineEntity): Long {
        val toSave = routine.copy(updatedAt = System.currentTimeMillis())
        val id = db.routineDao().upsert(toSave).let { if (routine.id == 0L) it else routine.id }
        scheduleRoutine(toSave.copy(id = id))
        return id
    }

    suspend fun deleteRoutine(id: Long) {
        db.routineDao().delete(id)
        scheduler.cancel(ReminderType.ROUTINE, id)
    }

    suspend fun setRoutineDone(routineId: Long, day: LocalDate, done: Boolean) {
        val completion = RoutineCompletion(routineId, day.toEpochDay())
        if (done) db.routineDao().addCompletion(completion) else db.routineDao().removeCompletion(completion)
    }

    private fun scheduleRoutine(routine: RoutineEntity) {
        val at = if (!routine.active || !routine.reminderEnabled || routine.timeMinutes == null) null
        else Weekdays.nextOccurrence(routine.daysMask, routine.timeMinutes, LocalDateTime.now())
        if (at == null) scheduler.cancel(ReminderType.ROUTINE, routine.id) else scheduler.schedule(ReminderType.ROUTINE, routine.id, at)
    }

    // ---------- Compras ----------
    suspend fun addShoppingList(name: String): Long = db.shoppingDao().insertList(ShoppingList(name = name.trim()))
    suspend fun renameShoppingList(list: ShoppingList, name: String) = db.shoppingDao().updateList(list.copy(name = name.trim()))
    suspend fun deleteShoppingList(list: ShoppingList) = db.shoppingDao().deleteList(list)
    suspend fun saveShoppingItem(item: ShoppingItem) { db.shoppingDao().upsertItem(item.copy(updatedAt = System.currentTimeMillis())) }
    suspend fun deleteShoppingItem(item: ShoppingItem) = db.shoppingDao().deleteItem(item)
    suspend fun clearPurchased(listId: Long) = db.shoppingDao().clearPurchased(listId)

    // ---------- Pagos ----------
    suspend fun getPayment(id: Long) = db.paymentDao().get(id)

    suspend fun savePayment(payment: PaymentEntity): Long {
        val toSave = payment.copy(updatedAt = System.currentTimeMillis())
        val id = db.paymentDao().upsert(toSave).let { if (payment.id == 0L) it else payment.id }
        schedulePayment(toSave.copy(id = id))
        return id
    }

    suspend fun setPaymentPaid(payment: PaymentEntity, paid: Boolean) {
        db.paymentDao().setPaid(payment.id, paid)
        schedulePayment(payment.copy(paid = paid))
    }

    suspend fun deletePayment(id: Long) {
        db.paymentDao().delete(id)
        scheduler.cancel(ReminderType.PAYMENT, id)
    }

    private fun schedulePayment(payment: PaymentEntity) {
        val at = if (payment.paid) null
        else reminderAt(payment.dueEpochDay, null, payment.reminderMinutesBefore, LocalDateTime.now())
        if (at == null) scheduler.cancel(ReminderType.PAYMENT, payment.id) else scheduler.schedule(ReminderType.PAYMENT, payment.id, at)
    }

    // ---------- Recordatorios ----------
    suspend fun rescheduleAll() {
        db.taskDao().withReminders().forEach { scheduleTask(it) }
        db.eventDao().withReminders().forEach { scheduleEvent(it) }
        db.routineDao().withReminders().forEach { scheduleRoutine(it) }
        db.paymentDao().withReminders().forEach { schedulePayment(it) }
    }

    /** Llamado por la alarma: solo avisa si el elemento sigue existiendo y pendiente. */
    suspend fun deliverReminder(type: ReminderType, id: Long) {
        val notificationId = ReminderScheduler.requestCode(type, id)
        when (type) {
            ReminderType.TASK -> db.taskDao().get(id)?.takeIf { !it.completed }?.let { task ->
                val time = task.dueMinutes?.let { " a las ${minutesLabel(it)}" } ?: ""
                Notifications.show(context, notificationId, task.title, "Tarea pendiente$time. ${task.notes}".trim(), child = false)
            }
            ReminderType.EVENT -> db.eventDao().get(id)?.takeIf { !it.completed }?.let { event ->
                val time = event.startMinutes?.let { "A las ${minutesLabel(it)}" } ?: event.kind
                val place = event.location.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                val title = if (event.forChild) "Mi hijo: ${event.title}" else event.title
                Notifications.show(context, notificationId, title, "$time$place", child = event.forChild)
            }
            ReminderType.ROUTINE -> db.routineDao().get(id)?.takeIf { it.active && it.reminderEnabled }?.let { routine ->
                Notifications.show(context, notificationId, routine.title, "Es momento de tu rutina", child = false)
                scheduleRoutine(routine) // siguiente día que corresponda
            }
            ReminderType.PAYMENT -> db.paymentDao().get(id)?.takeIf { !it.paid }?.let { payment ->
                val date = LocalDate.ofEpochDay(payment.dueEpochDay)
                Notifications.show(
                    context, notificationId, "Pago: ${payment.name}",
                    "${formatAmount(payment.amountCents)} · vence el ${date.dayOfMonth}/${date.monthValue}", child = false,
                )
            }
        }
    }

    // ---------- Datos de ejemplo ----------
    suspend fun loadDemoData() {
        val today = LocalDate.now().toEpochDay()
        saveTask(TaskEntity(title = "Ejemplo: revisar correo del trabajo", dueEpochDay = today, dueMinutes = 10 * 60, isDemo = true))
        saveTask(TaskEntity(title = "Ejemplo: comprar despensa", dueEpochDay = today + 1, priority = 2, isDemo = true))
        saveEvent(EventEntity(title = "Ejemplo: clase de natación", kind = EventKind.ACTIVITY, forChild = true, epochDay = today, startMinutes = 17 * 60, endMinutes = 18 * 60, priority = 2, location = "Ejemplo: deportivo", isDemo = true))
        saveEvent(EventEntity(title = "Ejemplo: llamada con cliente", epochDay = today, startMinutes = 17 * 60 + 30, endMinutes = 18 * 60 + 15, isDemo = true))
        saveEvent(EventEntity(title = "Ejemplo: preparar cartulina", kind = EventKind.MATERIAL, forChild = true, epochDay = today + 2, isDemo = true))
        saveRoutine(RoutineEntity(title = "Ejemplo: tomar agua", daysMask = Weekdays.ALL, timeMinutes = 8 * 60, isDemo = true))
    }

    suspend fun deleteDemoData() {
        db.taskDao().demoIds().forEach { scheduler.cancel(ReminderType.TASK, it) }
        db.eventDao().demoIds().forEach { scheduler.cancel(ReminderType.EVENT, it) }
        db.routineDao().demoIds().forEach { scheduler.cancel(ReminderType.ROUTINE, it) }
        db.taskDao().deleteDemo()
        db.eventDao().deleteDemo()
        db.routineDao().deleteDemo()
    }
}
