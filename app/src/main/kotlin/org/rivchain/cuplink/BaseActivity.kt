package org.rivchain.cuplink

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import org.rivchain.cuplink.model.Contact
import org.rivchain.cuplink.model.Event
import org.rivchain.cuplink.util.Log
import java.util.Date

/*
 * Base class for every Activity
*/
open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        DatabaseCache.databasePath = this.filesDir.toString() + "/database.bin"
    }

    protected open fun onServiceRestart(){

    }

    protected open fun restartService(){
        // Inflate the layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_restart_service, null)
        // Create the AlertDialog
        val serviceRestartDialog = AlertDialog.Builder(this, R.style.PPTCDialog)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        // Show the dialog
        serviceRestartDialog.show()

        // Restart service
        val intentStop = Intent(this, MainService::class.java)
        intentStop.action = MainService.ACTION_STOP
        startService(intentStop)
        Thread {
            Thread.sleep(1000)
            val intentStart = Intent(this, MainService::class.java)
            intentStart.action = MainService.ACTION_START
            startService(intentStart)
            Thread.sleep(2000)
            runOnUiThread {
                serviceRestartDialog.dismiss()
                onServiceRestart()
            }
        }.start()
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