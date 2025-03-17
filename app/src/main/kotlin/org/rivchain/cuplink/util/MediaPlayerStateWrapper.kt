package org.rivchain.cuplink.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import java.util.EnumSet

object MediaPlayerStateWrapper {

    private val mediaPlayer: MediaPlayer = MediaPlayer()
    private var currentState: State = State.IDLE
    private var stateListener: StateListener? = null

    enum class State {
        IDLE, ERROR, INITIALIZED, PREPARING, PREPARED, STARTED, STOPPED, PLAYBACK_COMPLETE, PAUSED
    }

    fun setDataSource(context: Context, uri: Uri) {
        if (currentState == State.IDLE) {
            try {
                mediaPlayer.setDataSource(context, uri)
                currentState = State.INITIALIZED
            } catch (e: Exception) {
                Log.e(this, e.message.toString())
            }
        }
    }

    fun setDataSource(afd: AssetFileDescriptor) {
        if (currentState == State.IDLE) {
            try {
                mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.declaredLength)
                currentState = State.INITIALIZED
            } catch (e: Exception) {
                Log.e(this, e.message.toString())
            }
        }
    }

    fun prepareAsync() {
        Log.d(this, "prepareAsync()")
        if (currentState in listOf(State.INITIALIZED, State.STOPPED)) {
            mediaPlayer.prepareAsync()
            currentState = State.PREPARING
        }
    }

    fun prepare() {
        Log.d(this, "prepare()")
        if (currentState in listOf(State.INITIALIZED, State.STOPPED)) {
            currentState = State.PREPARING
            mediaPlayer.prepare()
            currentState = State.PREPARED
        }
    }

    fun isPlaying(): Boolean {
        Log.d(this, "isPlaying()")
        return currentState != State.ERROR && mediaPlayer.isPlaying
    }

    fun seekTo(msec: Int) {
        Log.d(this, "seekTo()")
        if (currentState in listOf(State.PREPARED, State.STARTED, State.PAUSED, State.PLAYBACK_COMPLETE)) {
            mediaPlayer.seekTo(msec)
        }
    }

    fun pause() {
        Log.d(this, "pause()")
        if (currentState in listOf(State.STARTED, State.PAUSED)) {
            mediaPlayer.pause()
            currentState = State.PAUSED
        }
    }

    fun start() {
        Log.d(this, "start()")
        if (currentState in listOf(State.PREPARED, State.STARTED, State.PAUSED, State.PLAYBACK_COMPLETE)) {
            mediaPlayer.start()
            currentState = State.STARTED
        }
    }

    fun stop() {
        Log.d(this, "stop()")
        if (currentState in listOf(State.PREPARED, State.STARTED, State.STOPPED, State.PAUSED, State.PLAYBACK_COMPLETE)) {
            mediaPlayer.stop()
            currentState = State.STOPPED
        }
    }

    fun reset() {
        Log.d(this, "reset()")
        if (currentState in listOf(State.PREPARED, State.STARTED, State.STOPPED, State.PAUSED, State.PLAYBACK_COMPLETE, State.IDLE, State.INITIALIZED)) {
            mediaPlayer.reset()
            currentState = State.IDLE
        }
    }

    fun release() {
        Log.d(this, "release()")
        mediaPlayer.release()
    }

    interface StateListener {
        fun onCompletion(mp: MediaPlayer)
        fun onPrepared(mp: MediaPlayer)
    }

    fun setStateListener(listener: StateListener) {
        stateListener = listener
    }

    fun setAudioAttributes(attributes: AudioAttributes) {
        // Idle, Initialized, Stopped, Prepared, Started, Paused, PlaybackCompleted
        if (EnumSet.of(State.IDLE, State.INITIALIZED, State.STOPPED, State.PREPARED, State.STARTED, State.PAUSED, State.PLAYBACK_COMPLETE).contains(
                currentState)) {
            mediaPlayer.setAudioAttributes(attributes);
        }
    }

    fun setVolume(leftVolume: Float,rightVolume: Float) {
        // Idle, Initialized, Stopped, Prepared, Started, Paused, PlaybackCompleted
        if (EnumSet.of(State.IDLE, State.INITIALIZED, State.STOPPED, State.PREPARED, State.STARTED, State.PAUSED, State.PLAYBACK_COMPLETE).contains(
                currentState)) {
            mediaPlayer.setVolume(leftVolume, rightVolume);
        }
    }
}
