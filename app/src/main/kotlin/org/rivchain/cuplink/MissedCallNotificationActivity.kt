package org.rivchain.cuplink

import android.app.Activity
import android.os.Bundle
import org.rivchain.cuplink.util.ServiceUtil

class MissedCallNotificationActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get notification manager
        val notificationManager = ServiceUtil.getNotificationManager(this)

        // Retrieve notification ID
        val notificationId = intent.getIntExtra("EXTRA_NOTIFICATION_ID", -1)
        if (notificationId != -1) {
            notificationManager.cancel(notificationId)
        }

        // Close the activity immediately
        finish()
    }
}
