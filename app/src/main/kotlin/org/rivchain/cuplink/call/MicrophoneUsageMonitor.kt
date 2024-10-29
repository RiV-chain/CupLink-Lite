package org.rivchain.cuplink.call

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*

@RequiresApi(Build.VERSION_CODES.N)
class MicrophoneUsageMonitor(private val context: Context, private val rtcAudioManager: RTCAudioManager) {
    private var monitorJob: Job? = null

    fun startMonitoring(callback: (Boolean) -> Unit) {
        monitorJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val isMultipleRecording = checkMultipleMicrophoneUsage()
                withContext(Dispatchers.Main) {
                    callback(isMultipleRecording)
                }
                delay(1000) // Check every second
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun checkMultipleMicrophoneUsage(): Boolean {
        val audioSessionsCounter = rtcAudioManager.getAudioSessionsCounter(context)
        return audioSessionsCounter > 1
    }
}