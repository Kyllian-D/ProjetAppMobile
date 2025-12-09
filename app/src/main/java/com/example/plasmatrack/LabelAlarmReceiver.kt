package com.example.plasmatrack

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat

@SuppressLint("MissingPermission") // runtime permission POST_NOTIFICATIONS should be requested by the app when needed
class LabelAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val labelId = intent.getLongExtra("labelId", -1L)
        val endoscope = intent.getStringExtra("endoscope") ?: "Label"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "labels_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Label alarms", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(ch)
        }
        val pi = PendingIntent.getActivity(context, 0, Intent(context, ParametersActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notif = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Label expired: $endoscope")
            .setContentText("A label has reached its 7-day countdown")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(labelId.toInt(), notif)
    }
}
