package org.rivchain.cuplink.call

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecordingConfiguration
import android.os.Build
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.annotation.RequiresApi
import org.rivchain.cuplink.util.ServiceUtil

@RequiresApi(Build.VERSION_CODES.Q)
class MicrophoneUsageObserver(private val context: Context, private val notificationText: TextView) {

    private val audioManager = ServiceUtil.getAudioManager(context)

    private val recordingCallback = object : AudioManager.AudioRecordingCallback() {
        override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
            super.onRecordingConfigChanged(configs)

            // Check the number of active recording sessions
            if (configs.isNotEmpty()) {
                // Microphone is being used by one or more apps
                val clientIds = configs.map { it.clientAudioSessionId }.toSet()
                if (clientIds.size > 1) {
                    // notify user about MultipleMicrophoneUsage
                    notificationText.visibility = VISIBLE
                } else {
                    notificationText.visibility = INVISIBLE
                }
            } else {
                // No app is using the microphone
                println("No apps are currently using the microphone.")
            }
        }
    }

    fun startObserving() {
        audioManager.registerAudioRecordingCallback(recordingCallback, null)
    }

    fun stopObserving() {
        audioManager.unregisterAudioRecordingCallback(recordingCallback)
    }
}