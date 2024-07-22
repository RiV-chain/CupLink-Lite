package org.rivchain.cuplink

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service.NOTIFICATION_SERVICE
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.rivchain.cuplink.model.Event
import org.rivchain.cuplink.util.Log

internal object NotificationUtils {

    fun refreshContacts(context: Context) {
        LocalBroadcastManager.getInstance(context.applicationContext)
            .sendBroadcast(Intent("refresh_contact_list"))
    }

    fun refreshEvents(context: Context) {
        LocalBroadcastManager.getInstance(context)
            .sendBroadcast(Intent("refresh_event_list"))
    }

    fun updateNotification(context: Context) {
        Log.d(this, "updateNotification()")

        if (!DatabaseCache.database.settings.disableCallHistory) {
            val eventList = DatabaseCache.database.events.eventList
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Map to track the last event per callerChannelId
            val eventsMap = mutableMapOf<Int, ArrayList<Event>>()

            // Populate the events per callerChannelId
            for (event in eventList) {
                val callerChannelId = org.rivchain.cuplink.util.Utils.byteArrayToCRC32Int(event.publicKey)
                if(eventsMap[callerChannelId] == null){
                    eventsMap[callerChannelId] = ArrayList()
                }
                eventsMap[callerChannelId]!!.add(event)
            }

            // Filter the last events to get only those that are missed calls
            val lastMissedEvents = eventsMap.values.filter { e ->
                e.last().type == Event.Type.INCOMING_MISSED
            }

            // Filter the last events to get only those that are not missed calls
            val nonLastMissedEvents = eventsMap.values.filter { e ->
                e.last().type != Event.Type.INCOMING_MISSED
            }

            // Map to track missed calls per callerChannelId
            val missedCallCounts = mutableMapOf<Int, Int>()

            // Populate the missed call counts map
            for (el in lastMissedEvents) {
                for (e in el.reversed()) {
                    if(e.type != Event.Type.INCOMING_MISSED){
                        break
                    }
                    val callerChannelId =
                        org.rivchain.cuplink.util.Utils.byteArrayToCRC32Int(e.publicKey)
                    val currentCount = missedCallCounts[callerChannelId] ?: 0
                    missedCallCounts[callerChannelId] = currentCount + 1
                }
            }

            // Generate notifications for each callerChannelId if missedCount > 1
            for ((callerChannelId, missedCount) in missedCallCounts) {
                val publicKey = lastMissedEvents.find { event ->
                    org.rivchain.cuplink.util.Utils.byteArrayToCRC32Int(event.last().publicKey) == callerChannelId
                }?.last()!!.publicKey

                val contact = publicKey.let { DatabaseCache.database.contacts.getContactByPublicKey(it) }
                val name = contact?.name ?: context.getString(R.string.unknown_caller)
                val message = String.format(context.getString(R.string.missed_call_from), name, missedCount)

                val notification = createNotification(context, message, false)
                manager.notify(callerChannelId, notification)
            }

            // Cancel notifications for each caller having no missed calls
            for (e in nonLastMissedEvents) {
                val callerChannelId = org.rivchain.cuplink.util.Utils.byteArrayToCRC32Int(e.last().publicKey)
                manager.cancel(callerChannelId)
            }
        }
    }

    private fun createNotification(context: Context, text: String, showSinceWhen: Boolean): Notification {
        Log.d(this, "createNotification() text=$text setShowWhen=$showSinceWhen")
        val channelId = "cuplink_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                "CupLink Call Listener",
                NotificationManager.IMPORTANCE_LOW // display notification as collapsed by default
            )
            chan.lightColor = Color.RED
            chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            val service = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            service.createNotificationChannel(chan)
        }

        // start MainActivity
        val notificationIntent = Intent(context, MainActivity::class.java)
        val pendingNotificationIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(context, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        } else {
            PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        return NotificationCompat.Builder(context.applicationContext, channelId)
            .setSilent(true)
            .setOngoing(true)
            .setShowWhen(showSinceWhen)
            .setUsesChronometer(showSinceWhen)
            .setSmallIcon(R.drawable.cup_link_small)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentText(text)
            .setContentIntent(pendingNotificationIntent)
            .build()
    }
}