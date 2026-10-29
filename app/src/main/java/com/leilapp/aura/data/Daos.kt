package com.leilapp.aura.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY builtIn DESC, name COLLATE NOCASE")
    fun observeAll(): Flow<List<Category>>

    @Insert
    suspend fun insert(category: Category): Long

    @Insert
    suspend fun insertAll(categories: List<Category>)

    @Delete
    suspend fun delete(category: Category)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY completed, dueEpochDay IS NULL, dueEpochDay, dueMinutes IS NULL, dueMinutes, priority DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE completed = 0 AND reminderMinutesBefore IS NOT NULL AND dueEpochDay IS NOT NULL")
    suspend fun withReminders(): List<TaskEntity>

    @Upsert
    suspend fun upsert(task: TaskEntity): Long

    @Query("UPDATE tasks SET completed = :completed, updatedAt = :now WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategory(categoryId: Long)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT id FROM tasks WHERE isDemo = 1")
    suspend fun demoIds(): List<Long>

    @Query("DELETE FROM tasks WHERE isDemo = 1")
    suspend fun deleteDemo()
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY epochDay IS NULL, epochDay, startMinutes IS NULL, startMinutes")
    fun observeAll(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun get(id: Long): EventEntity?

    @Query("SELECT * FROM events WHERE completed = 0 AND reminderMinutesBefore IS NOT NULL AND epochDay IS NOT NULL")
    suspend fun withReminders(): List<EventEntity>

    @Upsert
    suspend fun upsert(event: EventEntity): Long

    @Query("UPDATE events SET completed = :completed, updatedAt = :now WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT id FROM events WHERE isDemo = 1")
    suspend fun demoIds(): List<Long>

    @Query("DELETE FROM events WHERE isDemo = 1")
    suspend fun deleteDemo()
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY timeMinutes IS NULL, timeMinutes, title COLLATE NOCASE")
    fun observeAll(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun get(id: Long): RoutineEntity?

    @Query("SELECT * FROM routines WHERE active = 1 AND reminderEnabled = 1 AND timeMinutes IS NOT NULL")
    suspend fun withReminders(): List<RoutineEntity>

    @Upsert
    suspend fun upsert(routine: RoutineEntity): Long

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM routine_completions WHERE epochDay = :epochDay")
    fun observeCompletions(epochDay: Long): Flow<List<RoutineCompletion>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addCompletion(completion: RoutineCompletion)

    @Delete
    suspend fun removeCompletion(completion: RoutineCompletion)

    @Query("SELECT id FROM routines WHERE isDemo = 1")
    suspend fun demoIds(): List<Long>

    @Query("DELETE FROM routines WHERE isDemo = 1")
    suspend fun deleteDemo()
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_lists ORDER BY createdAt")
    fun observeLists(): Flow<List<ShoppingList>>

    @Query("SELECT * FROM shopping_items ORDER BY purchased, name COLLATE NOCASE")
    fun observeItems(): Flow<List<ShoppingItem>>

    @Insert
    suspend fun insertList(list: ShoppingList): Long

    @Update
    suspend fun updateList(list: ShoppingList)

    @Delete
    suspend fun deleteList(list: ShoppingList)

    @Upsert
    suspend fun upsertItem(item: ShoppingItem): Long

    @Delete
    suspend fun deleteItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE listId = :listId AND purchased = 1")
    suspend fun clearPurchased(listId: Long)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY paid, dueEpochDay")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun get(id: Long): PaymentEntity?

    @Query("SELECT * FROM payments WHERE paid = 0 AND reminderMinutesBefore IS NOT NULL")
    suspend fun withReminders(): List<PaymentEntity>

    @Upsert
    suspend fun upsert(payment: PaymentEntity): Long

    @Query("UPDATE payments SET paid = :paid, updatedAt = :now WHERE id = :id")
    suspend fun setPaid(id: Long, paid: Boolean, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun delete(id: Long)
}
