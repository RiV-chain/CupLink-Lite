package org.rivchain.cuplink.util

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;

import androidx.annotation.MainThread;

import android.media.AudioDeviceInfo.TYPE_BLE_HEADSET;
import android.media.AudioDeviceInfo.TYPE_BLE_SPEAKER;
import android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP;
import android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO;
import android.media.AudioDeviceInfo.TYPE_USB_HEADSET;
import android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES;
import android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET;
import org.rivchain.cuplink.MainApplication

object SoundUtil {

    private const val FLAG_BYPASS_INTERRUPTION_POLICY = 0x1 shl 6

    @MainThread
    fun play(resId: Int) {
        val audioManager = MainApplication.getAppContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringerMode = audioManager.ringerMode
        val isSilent = ringerMode == AudioManager.RINGER_MODE_SILENT || ringerMode == AudioManager.RINGER_MODE_VIBRATE

        if (!isSilent) {
            val mediaPlayer = MediaPlayerStateWrapper.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setVolume(0.1f, 0.1f)
                setStateListener(object : MediaPlayerStateWrapper.StateListener {
                    override fun onCompletion(mp: MediaPlayer) {
                        if (mp.isPlaying) {
                            mp.stop()
                        }
                        mp.reset()
                        mp.release()
                    }

                    override fun onPrepared(mp: MediaPlayer) {
                        // ignore prepared state as we use synchronous prepare() method
                    }
                })
            }

            try {
                MainApplication.getAppContext().resources.openRawResourceFd(resId).use { afd ->
                    mediaPlayer.setDataSource(afd)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                }
            } catch (e: Exception) {
                Log.d(this, "could not play in-app sound.")
                mediaPlayer.release()
            }
        }
    }


    fun getAudioAttributesForUsage(usage: Int): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
            .setUsage(usage)
            .build()
    }

    fun getAudioAttributesForCallNotification(): AudioAttributes {
        return AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setFlags(FLAG_BYPASS_INTERRUPTION_POLICY)
            .build()
    }

    fun isHeadsetOn(audioManager: AudioManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
                it.type in listOf(
                    TYPE_BLUETOOTH_SCO,
                    TYPE_BLE_HEADSET,
                    TYPE_BLE_SPEAKER,
                    TYPE_BLUETOOTH_A2DP,
                    TYPE_WIRED_HEADPHONES,
                    TYPE_WIRED_HEADSET,
                    TYPE_USB_HEADSET
                )
            }.also { if (it) Log.i(this, "Headphones are connected.") }
        } else {
            when {
                audioManager.isWiredHeadsetOn -> {
                    Log.i(this, "Wired headset is connected.")
                    true
                }
                audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn -> {
                    Log.i(this, "Bluetooth headset is connected.")
                    true
                }
                else -> false
            }
        }
    }
}