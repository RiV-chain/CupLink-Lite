package org.rivchain.cuplink

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import org.rivchain.cuplink.CallService.Companion.SERVICE_CONTACT_KEY
import org.rivchain.cuplink.model.Contact


class CallStatusService : Service() {

    companion object {
        const val CHANNEL_ID = "CallStatusChannel"
        const val NOTIFICATION_ID = 101
    }

    override fun onCreate() {
        super.onCreate()
        val notification = NotificationUtils.buildNotification(this, null)
        // Start the service as a foreground service with microphone permission for Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            // Start the service as a regular foreground service for older versions
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle service tasks here
        if(intent == null) {
            return super.onStartCommand(intent, flags, startId)
        }
        val contact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(SERVICE_CONTACT_KEY, Contact::class.java)
        } else {
            intent.getSerializableExtra(SERVICE_CONTACT_KEY)
        } as Contact
        val manager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationUtils.buildNotification(this, contact)
        manager.notify(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This service is not designed for binding, return null
        return null
    }
}
