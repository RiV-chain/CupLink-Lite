package org.rivchain.cuplink.call

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.rivchain.cuplink.call.RTCPeerConnection.CallState
import org.rivchain.cuplink.call.RTCPeerConnection.Companion.incomingRTCCall
import org.rivchain.cuplink.call.RTCPeerConnection.Companion.outgoingRTCCall
import org.rivchain.cuplink.message.ActionMessageDispatcher
import org.rivchain.cuplink.util.ServiceUtil

class CallStatusHandler(private val context: Context, private val dispatcher: ActionMessageDispatcher) {

    private var telephonyCallback: TelephonyCallback? = null
    private var callStatusListener: PhoneStateListener? = null
    private lateinit var telephonyManager: TelephonyManager
    private var onHold = false

    fun startCallStatusListening() {

        if(ActivityCompat.checkSelfPermission(this.context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

            telephonyManager = ServiceUtil.getTelephonyManager(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback =
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {

                        override fun onCallStateChanged(state: Int) {
                            processCallStateChanged(state)
                        }
                    }
                telephonyManager.registerTelephonyCallback(
                    context.mainExecutor,
                    telephonyCallback!!
                )

            } else {
                callStatusListener = object : PhoneStateListener() {

                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        super.onCallStateChanged(state, phoneNumber)
                        processCallStateChanged(state)
                    }
                }
                callStatusListener?.let {
                    telephonyManager.listen(it, PhoneStateListener.LISTEN_CALL_STATE)
                }
            }
        }
    }

    private fun processCallStateChanged(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                if (!onHold) {
                    onHold = true
                    Log.d("CallStatusHandler", "Incoming GSM call detected")
                    Log.d("VoIP", "Muting VoIP call")
                    val obj = JSONObject().apply {
                        put("action", "on_hold")
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        dispatcher.sendMessage(obj)
                        dispatcher.closeSocket()
                    }
                    incomingRTCCall?.apply {
                        reportStateChange(CallState.ON_HOLD)
                        callOnHold()
                    }
                    outgoingRTCCall?.apply {
                        reportStateChange(CallState.ON_HOLD)
                        callOnHold()
                    }
                }
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (!onHold) {
                    onHold = true
                    Log.d("CallStatusHandler", "GSM call in progress")
                    val obj = JSONObject().apply {
                        put("action", "on_hold")
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        dispatcher.sendMessage(obj)
                        dispatcher.closeSocket()
                    }
                    incomingRTCCall?.callOnHold()
                    outgoingRTCCall?.callOnHold()
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                Log.d("CallStatusHandler", "No active GSM call")
                if (onHold) {
                    onHold = false
                    val obj = JSONObject().apply {
                        put("action", "resume")
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        dispatcher.sendMessage(obj)
                        dispatcher.closeSocket()
                    }
                    incomingRTCCall?.apply {
                        reportStateChange(CallState.RESUME)
                        callResume()
                    }
                    outgoingRTCCall?.apply {
                        reportStateChange(CallState.RESUME)
                        callResume()
                    }
                }
            }
        }
    }

    fun stopCallStatusListening() {
        callStatusListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        callStatusListener = null
        telephonyCallback?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager.unregisterTelephonyCallback(telephonyCallback!!)
            }
        }
        telephonyCallback = null
    }
}