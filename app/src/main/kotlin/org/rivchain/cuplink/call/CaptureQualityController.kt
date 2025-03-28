package org.rivchain.cuplink.call

import android.os.Handler
import android.os.Looper
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import org.rivchain.cuplink.R
import org.rivchain.cuplink.model.Settings
import org.rivchain.cuplink.renderer.VerticalSlider
import org.rivchain.cuplink.util.Log
import org.webrtc.CameraEnumerationAndroid.CaptureFormat


/**
 * Control capture format based on a seekbar listeners.
 */
class CaptureQualityController(private val callActivity: CallActivity) {
    private val resolutionSlider = callActivity.findViewById<VerticalSlider>(R.id.captureResolutionSlider)
    private val framerateSlider = callActivity.findViewById<VerticalSlider>(R.id.captureFramerateSlider)
    private val moreImageButton = callActivity.findViewById<ImageButton>(R.id.more)
    private val formatText = callActivity.findViewById<TextView>(R.id.captureFormatText)
    private val framerateText = callActivity.findViewById<TextView>(R.id.captureFramerateText)
    private val hiddenControlsContainer = callActivity.findViewById<ConstraintLayout>(R.id.hiddenControlsContainer)
    private val buttonsContainer = callActivity.findViewById<ConstraintLayout>(R.id.buttonsContainer)


    private val degradationValues =
        moreImageButton.resources.getStringArray(R.array.videoDegradationModeValues)
    private val resolutionNames = mapOf(
        "160x120" to "SD", "240x160" to "SD", "320x240" to "SD", "400x240" to "SD",
        "480x320" to "SD", "640x360" to "SD", "640x480" to "SD", "768x480" to "SD",
        "854x480" to "VGA", "800x600" to "VGA", "960x540" to "VGA", "960x640" to "VGA",
        "1024x576" to "VGA", "1024x600" to "SGA", "1280x720" to "HD", "1280x1024" to "XGA",
        "1920x1080" to "FHD", "1920x1440" to "FHD", "2560x1440" to "2k", "3840x2160" to "4k"
    )
    private var cameraName = ""
    private val defaultFormats = listOf(
        CaptureFormat(256, 144, 0, 60000), CaptureFormat(320, 240, 0, 60000),
        CaptureFormat(480, 360, 0, 60000), CaptureFormat(640, 480, 0, 60000),
        CaptureFormat(960, 540, 0, 60000), CaptureFormat(1280, 720, 0, 60000),
        CaptureFormat(1920, 1080, 0, 60000), CaptureFormat(3840, 2160, 0, 60000),
        CaptureFormat(7680, 4320, 0, 60000)
    )

    private var resolutionSliderFraction = 0.5
    private var resolutionSliderInitialized = false
    private var framerateSliderFraction = 0.5
    private var framerateSliderInitialized = false

    // from settings
    private var defaultHeight = 0
    private var defaultWidth = 0
    private var defaultFramerate = 0

    // Create a handler
    val handler = Handler(Looper.getMainLooper())

    // Create a Runnable that hides the TextView
    val hideFormatText = Runnable {
        formatText.visibility = INVISIBLE
    }

    // Create a Runnable that hides the TextView
    val hideFramerateText = Runnable {
        framerateText.visibility = INVISIBLE
    }

    init {

        // Here are captureResolution and captureFramerate toggle button listener
        moreImageButton.setOnClickListener {
            if (buttonsContainer.visibility == GONE ){
                buttonsContainer.visibility = VISIBLE
                hiddenControlsContainer.visibility = GONE
                moreImageButton.setImageResource(R.drawable.ic_more_on)
            } else
            if (buttonsContainer.visibility == VISIBLE){
                buttonsContainer.visibility = GONE
                hiddenControlsContainer.visibility = VISIBLE
                moreImageButton.setImageResource(R.drawable.ic_more_off)
            }
        }

        resolutionSlider.setOnSeekBarChangeListener(object : VerticalSlider.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: VerticalSlider, progress: Int, fromUser: Boolean) {
                resolutionSliderFraction = (progress.toDouble() / 100.0)
                resolutionSliderInitialized = true
                updateView()
                // Show the TextView when progress changes
                formatText.visibility = VISIBLE
                // Remove any existing hide callbacks
                handler.removeCallbacks(hideFormatText)
                // Post a new hide callback with a 1-second delay
                handler.postDelayed(hideFormatText, 1000)
            }

            override fun onStartTrackingTouch(seekBar: VerticalSlider) {}
            override fun onStopTrackingTouch(seekBar: VerticalSlider) {
                updateView()
                changeCameraFormat()
            }
        })

        framerateSlider.setOnSeekBarChangeListener(object : VerticalSlider.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: VerticalSlider, progress: Int, fromUser: Boolean) {
                framerateSliderFraction = (progress.toDouble() / 100.0)
                framerateSliderInitialized = true
                updateView()
                // Show the TextView when progress changes
                framerateText.visibility = VISIBLE
                // Remove any existing hide callbacks
                handler.removeCallbacks(hideFramerateText)
                // Post a new hide callback with a 1-second delay
                handler.postDelayed(hideFramerateText, 1000)
            }

            override fun onStartTrackingTouch(seekBar: VerticalSlider) {}
            override fun onStopTrackingTouch(seekBar: VerticalSlider) {
                updateView()
                changeCameraFormat()
            }
        })
    }

    fun initFromSettings(settings: Settings) {
        Log.d(this, "initFromSettings() "
            + "videoDegradationMode=${settings.videoDegradationMode}, "
            + "cameraFramerate=${settings.cameraFramerate}, "
            + "cameraResolution=${settings.cameraResolution}")

        // parse framerate setting
        if (settings.cameraFramerate == "auto") {
            defaultFramerate = RTCCall.DEFAULT_FRAMERATE
        } else try {
            defaultFramerate = settings.cameraFramerate.toInt()
        } catch (e: Exception) {
            Log.e(this, "applySettings() unhandled cameraFramerate=${settings.cameraFramerate}")
            defaultFramerate = RTCCall.DEFAULT_FRAMERATE
        }

        // parse resolution setting
        if (settings.cameraResolution == "auto") {
            defaultWidth = RTCCall.DEFAULT_WIDTH
            defaultHeight = RTCCall.DEFAULT_HEIGHT
        } else try {
            val parts = settings.cameraResolution.split("x")
            defaultWidth = parts[0].toInt()
            defaultHeight = parts[1].toInt()
        } catch (e: Exception) {
            Log.e(this, "applySettings() unhandled cameraResolution=${settings.cameraResolution}")
            defaultWidth = RTCCall.DEFAULT_WIDTH
            defaultHeight = RTCCall.DEFAULT_HEIGHT
        }

        updateView()
    }

    private fun changeCameraFormat() {
        Log.d(this, "changeCameraFormat()")

        val format =  getSelectedFormat()
        val framerate = getSelectedFramerate()
        val degradation = getSelectedDegradation()
        if(CallActivity.isCallInProgress) {
            callActivity
                .getCurrentCall()
                .changeCaptureFormat(degradation, format.width, format.height, framerate)
        }
    }

    private val compareFormats = Comparator<CaptureFormat> { first, second ->
        val firstPixels = first.width * first.height
        val secondPixels = second.width * second.height
        if (firstPixels != secondPixels) {
            secondPixels - firstPixels
        } else {
            second.framerate.max - first.framerate.max
        }
    }

    private fun updateView() {
        Log.d(this, "updateView()")

        // "<name> <resolution>@<framerate>"
        var formatTextLabel = ""
        if (resolutionSlider.visibility == VISIBLE) {
            val format =  getSelectedFormat()
            val resolution = "${format.width}x${format.height}"
            Log.d(this, "resolution: $resolution")
            if (resolution in resolutionNames) {
                formatTextLabel = "${resolutionNames[resolution]} "
            } else {
                formatTextLabel = "..."
            }
        }
        formatText.text = formatTextLabel

        var framerateTextLabel = ""
        if (framerateSlider.visibility == VISIBLE) {
            val framerate = getSelectedFramerate()
            framerateTextLabel += "$framerate"
        }
        framerateText.text = framerateTextLabel
    }

    fun getSelectedDegradation(): String {

        if (framerateSlider.visibility == VISIBLE && resolutionSlider.visibility == VISIBLE) {
            val degradationValue = degradationValues[3]
            Log.d(this, "getSelectedDegradation: from slider: $degradationValue")
            return degradationValue
        } else {
            // default
            val degradationValue = degradationValues[0]
            Log.d(this, "getSelectedDegradation: by default: $degradationValue")
            return degradationValue
        }
    }

    fun getSelectedFormat(): CaptureFormat {
        if (resolutionSliderInitialized) {
            val index = (resolutionSliderFraction * (defaultFormats.size - 1)).toInt()
            return defaultFormats[index]
        } else {
            // default
            return CaptureFormat(defaultWidth, defaultHeight, defaultFramerate, defaultFramerate)
        }
    }

    fun getSelectedFramerate(): Int {
        if (framerateSliderInitialized) {
            val min = 5
            val max = 55
            return (min + framerateSliderFraction * (max - min)).toInt()
        } else {
            // default
            return defaultFramerate
        }
    }

    fun onCameraChange(newCameraName: String, isFrontFacing: Boolean, newFormats: List<CaptureFormat>) {
        Log.d(this, "onCameraChange() newCameraName=$newCameraName")

        resolutionSliderInitialized = false
        framerateSliderInitialized = false

        // newCameraName is rather bad
        cameraName = callActivity.getString(if (isFrontFacing) {
            R.string.camera_front
        } else {
            R.string.camera_back
        })

        updateView()
    }
}
