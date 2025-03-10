package org.rivchain.cuplink.call

import java.lang.ref.WeakReference

object CallManager {
    private var callActivityRef: WeakReference<CallActivity>? = null

    fun setCallActivity(activity: CallActivity) {
        callActivityRef = WeakReference(activity)
    }

    fun clearCallActivity() {
        callActivityRef?.clear()
        callActivityRef = null
    }

    fun getCallActivity(): CallActivity? {
        return callActivityRef?.get()
    }
}
