package com.ventgui.app.data.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ventgui.app.R

object NotificationHelper {
    const val CHANNEL_ID_BIRTHDAY = "channel_birthday"
    const val CHANNEL_NAME = "Aniversários de Atletas"
    const val NOTIF_ID_BASE_2DAYS = 20000
    const val NOTIF_ID_BASE_1DAY  = 21000

    fun createBirthdayChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID_BIRTHDAY, CHANNEL_NAME, importance).apply {
                description = "Notificações de aniversário de atletas"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showBirthdayNotification(
        context: Context,
        athleteId: String,
        athleteName: String,
        daysBefore: Int
    ) {
        val title = if (daysBefore == 2) "🎂 Aniversário em 2 dias" else "🎉 Aniversário amanhã!"
        val content = if (daysBefore == 2) {
            "O atleta $athleteName faz anos daqui a 2 dias."
        } else {
            "O atleta $athleteName faz anos amanhã."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_BIRTHDAY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notifId = if (daysBefore == 2) NOTIF_ID_BASE_2DAYS else NOTIF_ID_BASE_1DAY
        val uniqueId = notifId + athleteId.hashCode().and(0xFFFF)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(uniqueId, builder.build())
    }
}
