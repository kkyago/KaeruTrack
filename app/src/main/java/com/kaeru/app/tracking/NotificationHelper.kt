package com.kaeru.app.tracking

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.kaeru.app.R
import androidx.core.net.toUri
import com.kaeru.app.MainActivity

class NotificationHelper(private val context: Context) {
    private val TRACKING_CHANNEL_ID = "kaerutrack_updates"
    private val APP_UPDATE_CHANNEL_ID = "kaeru_app_updates"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TRACKING_CHANNEL_ID,
                "Atualizações de Encomendas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisa quando sua encomenda muda de status"
            }
            val updateChannel = NotificationChannel(
                APP_UPDATE_CHANNEL_ID,
                "Atualizações do app",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisa quando há uma nova versão disponível"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    fun showNotification(trackingName: String, trackingCode: String, newStatus: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("tracking_code", trackingCode)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            trackingCode.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, TRACKING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(trackingName)
            .setContentText(newStatus)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(trackingCode.hashCode(), notification)
    }
    fun showUpdateNotification(version: String, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, APP_UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Atualização disponível!")
            .setContentText("$version")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(999, notification)
    }
}