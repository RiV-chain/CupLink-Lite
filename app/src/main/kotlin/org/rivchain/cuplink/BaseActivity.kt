package org.rivchain.cuplink

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rivchain.cuplink.model.Contact
import org.rivchain.cuplink.model.Event
import org.rivchain.cuplink.util.Log
import java.util.Date
import java.util.function.Consumer


/*
 * Base class for every Activity
*/
open class BaseActivity : AppCompatActivity() {


    private val mBackgroundBlurRadius = 80
    private val mBlurBehindRadius = 20

    // We set a different dim amount depending on whether window blur is enabled or disabled
    private val mDimAmountWithBlur = 0.1f
    private val mDimAmountNoBlur = 0.4f

    // We set a different alpha depending on whether window blur is enabled or disabled
    private val mWindowBackgroundAlphaWithBlur = 170
    private val mWindowBackgroundAlphaNoBlur = 255

    // Use a rectangular shape drawable for the window background. The outline of this drawable
    // dictates the shape and rounded corners for the window background blur area.
    private var mWindowBackgroundDrawable: Drawable? = null

    private fun buildIsAtLeastS(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    /**
     * Set up a window blur listener.
     *
     * Window blurs might be disabled at runtime in response to user preferences or system states
     * (e.g. battery saving mode). WindowManager#addCrossWindowBlurEnabledListener allows to
     * listen for when that happens. In that callback we adjust the UI to account for the
     * added/missing window blurs.
     *
     * For the window background blur we adjust the window background drawable alpha:
     * - lower when window blurs are enabled to make the blur visible through the window
     * background drawable
     * - higher when window blurs are disabled to ensure that the window contents are readable
     *
     * For window blur behind we adjust the dim amount:
     * - higher when window blurs are disabled - the dim creates a depth of field effect,
     * bringing the user's attention to the dialog window
     * - lower when window blurs are enabled - no need for a high alpha, the blur behind is
     * enough to create a depth of field effect
     */
    @RequiresApi(api = Build.VERSION_CODES.S)
    private fun setupWindowBlurListener(window: Window) {
        val windowBlurEnabledListener: Consumer<Boolean> =
            Consumer<Boolean> { blursEnabled: Boolean -> this.updateWindowForBlurs(window, blursEnabled) }
        window.decorView.addOnAttachStateChangeListener(
            object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    windowManager.addCrossWindowBlurEnabledListener(
                        windowBlurEnabledListener
                    )
                }

                override fun onViewDetachedFromWindow(v: View) {
                    windowManager.removeCrossWindowBlurEnabledListener(
                        windowBlurEnabledListener
                    )
                }
            })
    }

    private fun updateWindowForBlurs(window: Window, blursEnabled: Boolean) {
        mWindowBackgroundDrawable!!.alpha =
            if (blursEnabled) mWindowBackgroundAlphaWithBlur else mWindowBackgroundAlphaNoBlur
        window.setDimAmount(if (blursEnabled) mDimAmountWithBlur else mDimAmountNoBlur)

        if (buildIsAtLeastS()) {
            // Set the window background blur and blur behind radii
            window.setBackgroundBlurRadius(mBackgroundBlurRadius)
            window.attributes.blurBehindRadius = mBlurBehindRadius
            window.attributes = window.attributes
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    private fun createBlurredDialog(builder: AlertDialog.Builder): AlertDialog {
        val dialog = builder.create()
        val window = dialog.window
        window?.setBackgroundDrawable(mWindowBackgroundDrawable)
        if (buildIsAtLeastS()) {
            // Enable blur behind. This can also be done in xml with R.attr#windowBlurBehindEnabled
            window?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);

            // Register a listener to adjust window UI whenever window blurs are enabled/disabled
            setupWindowBlurListener(window!!);
        } else {
            // Window blurs are not available prior to Android S
            updateWindowForBlurs(window!!,false);
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        return dialog
    }

    fun createBlurredPPTCDialog(view: View): AlertDialog {
        val b = AlertDialog.Builder(this, R.style.PPTCDialog)
            .setView(view) // Set the custom view to the dialog
        mWindowBackgroundDrawable = AppCompatResources.getDrawable(this, R.drawable.dialog_pptc_rounded_corner)
        return createBlurredDialog(b)
    }

    fun createBlurredMessageDialog(view: View): AlertDialog {
        val b = AlertDialog.Builder(this, R.style.MessageDialog)
            .setView(view) // Set the custom view to the dialog
        mWindowBackgroundDrawable = AppCompatResources.getDrawable(this, R.drawable.dialog_message_rounded_corner)
        return createBlurredDialog(b)
    }

    fun createBlurredProgressDialog(view: View): AlertDialog {
        val b = AlertDialog.Builder(this, R.style.MessageDialog)
            .setView(view) // Set the custom view to the dialog
        mWindowBackgroundDrawable = AppCompatResources.getDrawable(this, R.drawable.dialog_message_rounded_corner)
        return createBlurredDialog(b)
    }

    protected open fun onServiceRestart(){

    }

    protected open fun restartService(){
        // Inflate the layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_restart_service, null)
        // Create the AlertDialog
        val dialog = createBlurredMessageDialog(dialogView)
        dialog.setCancelable(false)
        // Show the dialog
        dialog.show()

        // Restart service
        val intentStop = Intent(this, MainService::class.java)
        intentStop.action = MainService.ACTION_STOP
        val intentStart = Intent(this@BaseActivity, MainService::class.java).apply {
            action = MainService.ACTION_START
        }
        Log.d(this, "Restarting service ...")
        lifecycleScope.launch {
            startService(intentStop)
            delay(1000)
            startService(intentStart)
            delay(2000)
            dialog.dismiss()
            onServiceRestart()
        }
    }

    protected open fun restartServiceInBackground(){
        // Restart service
        val intentStop = Intent(this, MainService::class.java)
        intentStop.action = MainService.ACTION_STOP
        val intentStart = Intent(this@BaseActivity, MainService::class.java).apply {
            action = MainService.ACTION_START
        }
        Log.d(this, "Restarting service ...")
        lifecycleScope.launch {
            startService(intentStop)
            delay(1000)
            startService(intentStart)
            delay(2000)
            onServiceRestart()
        }
    }

    fun setDefaultNightMode(nightModeSetting: String) {
        nightMode = when (nightModeSetting) {
            "on" -> AppCompatDelegate.MODE_NIGHT_YES
            "off" -> AppCompatDelegate.MODE_NIGHT_NO
            "auto" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                } else {
                    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                }
            else -> {
                Log.e(this, "invalid night mode setting: $nightModeSetting")
                nightMode
            }
        }
    }

    // prefer to be called before super.onCreate()
    fun applyNightMode() {
        if (nightMode != AppCompatDelegate.getDefaultNightMode()) {
            Log.d(this, "Change night mode to $nightMode")
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
    }

    fun getContactOrOwn(otherPublicKey: ByteArray): Contact? {
        val db = DatabaseCache.database
        return if (db.settings.publicKey.contentEquals(otherPublicKey)) {
            db.settings.getOwnContact()
        } else {
            db.contacts.getContactByPublicKey(otherPublicKey)
        }
    }

    open fun addContact(contact: Contact) {
        DatabaseCache.database.contacts.addContact(contact)
        DatabaseCache.save()

        pingContacts(listOf(contact))

        NotificationUtils.refreshEvents(this)
    }

    fun deleteContact(publicKey: ByteArray) {
        DatabaseCache.database.contacts.deleteContact(publicKey)
        DatabaseCache.database.events.deleteEventsByPublicKey(publicKey)
        DatabaseCache.save()

        NotificationUtils.refreshContacts(this)
        NotificationUtils.refreshEvents(this)
    }

    fun deleteEvents(eventDates: List<Date>) {
        DatabaseCache.database.events.deleteEventsByDate(eventDates)
        DatabaseCache.save()

        NotificationUtils.refreshContacts(this)
        NotificationUtils.refreshEvents(this)
    }

    fun pingContacts(contactList: List<Contact>) {
        Log.d(this, "pingContacts()")
        NotificationUtils.refreshContacts(this)
    }

    fun addEvent(event: Event) {
        Log.d(this, "addEvent() event.type=${event.type}")

        if (!DatabaseCache.database.settings.disableCallHistory) {
            DatabaseCache.database.events.addEvent(event)
            DatabaseCache.save()
            NotificationUtils.refreshEvents(this)
        }

        // update notification
        NotificationUtils.updateNotification(this)
    }

    fun clearEvents() {
        DatabaseCache.database.events.clearEvents()
        NotificationUtils.refreshEvents(this)
    }

    fun isDatabaseEncrypted(): Boolean {
        return DatabaseCache.dbEncrypted
    }


    fun importContacts(newDb: Database){
        val oldDatabase = DatabaseCache.database
        for (contact in newDb.contacts.contactList) {
            oldDatabase.contacts.addContact(contact)
        }
    }

    fun importCalls(newDb: Database){
        val oldDatabase = DatabaseCache.database
        for (event in newDb.events.eventList) {
            oldDatabase.events.addEvent(event)
        }
    }

    fun importSettings(newDb: Database){
        val oldDatabase = DatabaseCache.database
        oldDatabase.settings = newDb.settings
        oldDatabase.mesh = newDb.mesh
    }

    companion object {
        private var nightMode = AppCompatDelegate.getDefaultNightMode()

        fun isNightmodeEnabled(context: Context): Boolean {
            val mode = context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK)
            return (mode == Configuration.UI_MODE_NIGHT_YES)
        }
    }
}