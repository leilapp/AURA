package com.leilapp.aura

import android.app.Application
import com.leilapp.aura.data.AuraDatabase
import com.leilapp.aura.data.AuraRepository
import com.leilapp.aura.data.SettingsRepository
import com.leilapp.aura.data.WeatherClient
import com.leilapp.aura.data.WeatherRepository
import com.leilapp.aura.reminders.Notifications
import com.leilapp.aura.reminders.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Contenedor manual de dependencias: suficiente para una app de este tamaño, sin librerías extra. */
class AppContainer(app: Application) {
    val database = AuraDatabase.build(app)
    val scheduler = ReminderScheduler(app)
    val repository = AuraRepository(app, database, scheduler)
    val settings = SettingsRepository(app)
    val weather = WeatherRepository(settings, WeatherClient())
}

class AuraApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
        appScope.launch {
            container.repository.ensureDefaultCategories()
            // Reprogramar es idempotente: cada elemento reemplaza su propia alarma.
            container.repository.rescheduleAll()
        }
    }
}
