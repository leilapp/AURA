package com.leilapp.aura.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/*
 * Convenciones de almacenamiento:
 * - Fechas como "epochDay" (días desde 1970-01-01, LocalDate.toEpochDay()).
 * - Horas como minutos desde medianoche (0..1439).
 * - reminderMinutesBefore = null significa "sin recordatorio".
 * - updatedAt facilita una futura sincronización opcional.
 * - isDemo marca los datos de ejemplo para poder borrarlos todos de una vez.
 */

object Priority {
    const val LOW = 0
    const val NORMAL = 1
    const val HIGH = 2

    fun label(value: Int) = when (value) {
        HIGH -> "Alta"
        LOW -> "Baja"
        else -> "Normal"
    }
}

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val builtIn: Boolean = false,
)

@Entity(tableName = "tasks", indices = [Index("dueEpochDay"), Index("categoryId")])
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val dueEpochDay: Long? = null,
    val dueMinutes: Int? = null,
    val priority: Int = Priority.NORMAL,
    val categoryId: Long? = null,
    val completed: Boolean = false,
    val reminderMinutesBefore: Int? = null,
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Tipos de registro de agenda. Los de "Mi hijo" usan los mismos tipos con forChild = true. */
object EventKind {
    const val EVENT = "Evento"
    const val ACTIVITY = "Actividad"
    const val APPOINTMENT = "Cita"
    const val SCHOOL = "Escolar"
    const val MATERIAL = "Material por preparar"
    const val TO_BUY = "Artículo por comprar"
    const val IMPORTANT_DATE = "Fecha importante"
    const val REMINDER = "Recordatorio"

    val general = listOf(EVENT, APPOINTMENT, IMPORTANT_DATE, REMINDER)
    val child = listOf(ACTIVITY, APPOINTMENT, SCHOOL, MATERIAL, TO_BUY, IMPORTANT_DATE, REMINDER)
}

@Entity(tableName = "events", indices = [Index("epochDay"), Index("forChild")])
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val kind: String = EventKind.EVENT,
    /** Opcional: un material por preparar puede no tener fecha. */
    val epochDay: Long? = null,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val location: String = "",
    val notes: String = "",
    val forChild: Boolean = false,
    val priority: Int = Priority.NORMAL,
    val completed: Boolean = false,
    val reminderMinutesBefore: Int? = null,
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "routines")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    /** Bit 0 = lunes ... bit 6 = domingo. */
    val daysMask: Int,
    val timeMinutes: Int? = null,
    val active: Boolean = true,
    val reminderEnabled: Boolean = false,
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/** Una fila por rutina y día completado: así lo de hoy no se arrastra a mañana. */
@Entity(
    tableName = "routine_completions",
    primaryKeys = ["routineId", "epochDay"],
    foreignKeys = [ForeignKey(
        entity = RoutineEntity::class,
        parentColumns = ["id"],
        childColumns = ["routineId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("epochDay")],
)
data class RoutineCompletion(
    val routineId: Long,
    val epochDay: Long,
)

@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "shopping_items",
    foreignKeys = [ForeignKey(
        entity = ShoppingList::class,
        parentColumns = ["id"],
        childColumns = ["listId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("listId")],
)
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val name: String,
    val quantity: String = "",
    val purchased: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "payments", indices = [Index("dueEpochDay")])
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Importe en centavos para evitar errores de redondeo. */
    val amountCents: Long,
    val dueEpochDay: Long,
    val category: String = "",
    val paid: Boolean = false,
    val reminderMinutesBefore: Int? = null,
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
)
