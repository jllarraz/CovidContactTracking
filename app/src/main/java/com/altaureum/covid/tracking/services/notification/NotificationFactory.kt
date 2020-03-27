package com.altaureum.covid.tracking.services.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.altaureum.covid.tracking.R
import com.altaureum.covid.tracking.ui.activities.ContactListActivity


object NotificationFactory {
    val NOTIFICATION_ID=1214
    val NOTIFICATION_CHANNEL_ID = NotificationFactory::class.java.simpleName

    private var notification:Notification?=null

    @RequiresApi(Build.VERSION_CODES.O)
    fun getNotification(context: Context):Notification{
        if(notification==null){
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, context.getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_HIGH)
            channel.description = context.getString(R.string.notification_description)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            val notificationIntent = Intent(context, ContactListActivity::class.java)
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val contentIntent =
                PendingIntent.getActivity(context, 0, notificationIntent, 0)

            val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            notification = notificationBuilder
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentTitle(context.getString(R.string.app_name))
                .setSmallIcon(R.drawable.ic_virus_outline)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_virus_outline))
                .setAutoCancel(false)
                .setTimeoutAfter(-1)
                .setTicker(context.getString(
                    R.string.ble_client_notification))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(contentIntent)
                .setContentText(
                    context.getString(
                        R.string.ble_client_notification)).build()

        }
        return notification!!
    }

}