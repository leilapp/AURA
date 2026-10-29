package com.leilapp.aura.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

enum class ReminderType(val code: Int) { TASK(1), EVENT(2), ROUTINE(3), PAYMENT(4) }

/**
 * Programa recordatorios con AlarmManager.
 * - Si la usuaria permitió "alarmas exactas" se usan alarmas exactas.
 * - Si no, se usa setAndAllowWhileIdle: Android puede retrasarlas unos minutos para ahorrar batería.
 * Cada elemento tiene un único PendingIntent (tipo + id), así que reprogramar reemplaza
 * la alarma anterior en lugar de duplicarla.
 */
class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun schedule(type: ReminderType, id: Long, at: LocalDateTime) {
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pending = pendingIntent(type, id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(type: ReminderType, id: Long) {
        pendingIntent(type, id, PendingIntent.FLAG_NO_CREATE)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun pendingIntent(type: ReminderType, id: Long, flag: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .putExtra(EXTRA_TYPE, type.name)
            .putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(context, requestCode(type, id), intent, flag or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        const val ACTION_REMINDER = "com.leilapp.aura.REMINDER"
        const val EXTRA_TYPE = "type"
        const val EXTRA_ID = "id"

        fun requestCode(type: ReminderType, id: Long): Int = (type.code shl 27) or (id and 0x7FFFFFF).toInt()
    }
}
