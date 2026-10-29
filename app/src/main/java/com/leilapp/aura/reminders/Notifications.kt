package com.leilapp.aura.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.leilapp.aura.MainActivity
import com.leilapp.aura.R

object Notifications {
    const val CHANNEL_GENERAL = "recordatorios"
    const val CHANNEL_CHILD = "mi_hijo"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_GENERAL, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tareas, eventos, rutinas y pagos"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CHILD, "Mi hijo", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Recordatorios de actividades y compromisos de tu hijo"
            },
        )
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, notificationId: Int, title: String, text: String, child: Boolean) {
        if (!hasPermission(context)) return
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, if (child) CHANNEL_CHILD else CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_stat_aura)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(if (child) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // El permiso se revocó entre la comprobación y el aviso: no hay nada más que hacer.
        }
    }
}
