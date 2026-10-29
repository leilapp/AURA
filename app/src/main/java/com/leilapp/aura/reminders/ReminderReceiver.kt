package com.leilapp.aura.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.leilapp.aura.AuraApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Recibe la alarma, comprueba en la base de datos que el aviso siga vigente y lo muestra. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMINDER) return
        val type = intent.getStringExtra(ReminderScheduler.EXTRA_TYPE)
            ?.let { runCatching { ReminderType.valueOf(it) }.getOrNull() } ?: return
        val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, -1)
        if (id < 0) return
        val repository = (context.applicationContext as AuraApp).container.repository
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                repository.deliverReminder(type, id)
            } finally {
                pending.finish()
            }
        }
    }
}

/** Las alarmas se borran al reiniciar el teléfono o al actualizar la app: aquí se reprograman. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = (context.applicationContext as AuraApp).container.repository
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                repository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
