package com.leilapp.aura.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Category::class,
        TaskEntity::class,
        EventEntity::class,
        RoutineEntity::class,
        RoutineCompletion::class,
        ShoppingList::class,
        ShoppingItem::class,
        PaymentEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun eventDao(): EventDao
    abstract fun routineDao(): RoutineDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            "Personal", "Trabajo", "Mi hijo", "Hogar", "Compras", "Salud y bienestar", "Pagos", "Otros",
        )

        fun build(context: Context): AuraDatabase =
            Room.databaseBuilder(context, AuraDatabase::class.java, "aura.db").build()
    }
}
