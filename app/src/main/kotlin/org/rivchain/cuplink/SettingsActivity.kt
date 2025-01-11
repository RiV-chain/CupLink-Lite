package org.rivchain.cuplink

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.textfield.TextInputEditText
import org.rivchain.cuplink.DatabaseCache.Companion.database
import org.rivchain.cuplink.util.Log
import org.rivchain.cuplink.util.Utils

class SettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setTitle(R.string.title_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.apply {
            setNavigationOnClickListener {
                finish()
            }
        }

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        initViews()
    }

    private fun showClearEventsDialog() {
        Log.d(this, "showClearEventsDialog()")

        val view: View = LayoutInflater.from(this).inflate(R.layout.dialog_yes_no, null)
        val dialog = this.createBlurredPPTCDialog(view)
        val titleText = view.findViewById<TextView>(R.id.title)
        titleText.text = getString(R.string.clear_history)
        val messageText = view.findViewById<TextView>(R.id.message)
        messageText.text = getString(R.string.remove_all_events)
        val yesButton = view.findViewById<Button>(R.id.yes)
        yesButton.setOnClickListener {
            this.clearEvents()
            DatabaseCache.save()
            Toast.makeText(this@SettingsActivity, R.string.done, Toast.LENGTH_SHORT).show()
            dialog.cancel()
        }
        dialog.show()
    }

    private fun showConfirmExitDialog() {
        Log.d(this, "showConfirmExitDialog()")

        val view: View = LayoutInflater.from(this).inflate(R.layout.dialog_yes_no, null)
        val dialog = this.createBlurredPPTCDialog(view)
        val titleText = view.findViewById<TextView>(R.id.title)
        titleText.text = getString(R.string.exit_app)
        val messageText = view.findViewById<TextView>(R.id.message)
        messageText.text = getString(R.string.exit_app_message)
        val yesButton = view.findViewById<Button>(R.id.yes)
        yesButton.setOnClickListener {
            MainService.stopPacketsStream(this)
            finishAffinity()
        }
        dialog.show()
    }

    private fun toggleFragment(
        fragment: Fragment,
        containerId: Int,
        fragmentContainers: List<FrameLayout>,
        buttons: List<View>,
        clickedButton: View
    ) {
        val transaction = supportFragmentManager.beginTransaction()

        // Set custom animations for fragment transitions
        transaction.setCustomAnimations(
            R.anim.fragment_fade_in,  // Enter animation
            R.anim.fragment_fade_out, // Exit animation
            R.anim.fragment_fade_in,  // Pop enter animation
            R.anim.fragment_fade_out  // Pop exit animation
        )

        // Find the current container and fragment
        val targetContainer = fragmentContainers.find { it.id == containerId }
        val currentFragment = supportFragmentManager.findFragmentById(containerId)

        if (targetContainer?.visibility == View.VISIBLE) {
            // If the target fragment is already visible, hide it
            targetContainer.visibility = View.GONE
            currentFragment?.let { transaction.hide(it) }
            clickedButton.isSelected = false
        } else {
            // Otherwise, hide all fragments and unselect all buttons
            fragmentContainers.forEach { container ->
                container.visibility = View.GONE
                supportFragmentManager.findFragmentById(container.id)?.let {
                    transaction.hide(it)
                }
            }
            buttons.forEach { it.isSelected = false }

            // Show the selected fragment
            if (currentFragment != null) {
                transaction.show(currentFragment)
            } else {
                transaction.replace(containerId, fragment)
            }

            targetContainer?.visibility = View.VISIBLE
            clickedButton.isSelected = true
        }

        transaction.commit()
    }

    private fun initViews() {
        val settings = database.settings

        // Navigate to external activities
        findViewById<View>(R.id.about).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        findViewById<View>(R.id.backup).setOnClickListener {
            startActivity(Intent(this, BackupActivity::class.java))
        }

        findViewById<View>(R.id.clearHistory).setOnClickListener {
            showClearEventsDialog()
        }

        findViewById<View>(R.id.exit).setOnClickListener {
            showConfirmExitDialog()
        }

        // Fragments
        val privacyAndSecurityFragment = PrivacyAndSecurityFragment()
        val soundNotificationsFragment = SoundNotificationsFragment()
        val mediaFragment = MediaFragment()
        val networkFragment = NetworkFragment()
        val systemFragment = SystemFragment()
        val qualityFragment = QualityFragment()

        // Fragment containers and buttons
        val fragmentContainers = listOf(
            findViewById<FrameLayout>(R.id.fragmentContainer1),
            findViewById<FrameLayout>(R.id.fragmentContainer2),
            findViewById<FrameLayout>(R.id.fragmentContainer3),
            findViewById<FrameLayout>(R.id.fragmentContainer4),
            findViewById<FrameLayout>(R.id.fragmentContainer5),
            findViewById<FrameLayout>(R.id.fragmentContainer6)
        )

        val buttons = listOf(
            findViewById<View>(R.id.toggleButtonPrivacy),
            findViewById<View>(R.id.soundNotifications),
            findViewById<View>(R.id.media),
            findViewById<View>(R.id.network),
            findViewById<View>(R.id.system),
            findViewById<View>(R.id.quality)
        )

        // Button click listeners
        findViewById<Button>(R.id.toggleButtonPrivacy).setOnClickListener {
            toggleFragment(
                privacyAndSecurityFragment,
                R.id.fragmentContainer1,
                fragmentContainers,
                buttons,
                it
            )
        }

        findViewById<Button>(R.id.soundNotifications).setOnClickListener {
            toggleFragment(
                soundNotificationsFragment,
                R.id.fragmentContainer2,
                fragmentContainers,
                buttons,
                it
            )
        }

        findViewById<Button>(R.id.media).setOnClickListener {
            toggleFragment(
                mediaFragment,
                R.id.fragmentContainer3,
                fragmentContainers,
                buttons,
                it
            )
        }

        findViewById<Button>(R.id.network).setOnClickListener {
            toggleFragment(
                networkFragment,
                R.id.fragmentContainer4,
                fragmentContainers,
                buttons,
                it
            )
        }

        findViewById<Button>(R.id.system).setOnClickListener {
            toggleFragment(
                systemFragment,
                R.id.fragmentContainer5,
                fragmentContainers,
                buttons,
                it
            )
        }

        findViewById<Button>(R.id.quality).setOnClickListener {
            toggleFragment(
                qualityFragment,
                R.id.fragmentContainer6,
                fragmentContainers,
                buttons,
                it
            )
        }

        // Update UI with user settings
        findViewById<Button>(R.id.edit).text = settings.username.ifEmpty { getString(R.string.no_value) }
        findViewById<View>(R.id.editIcon).setOnClickListener {
            showChangeUsernameDialog()
        }

        findViewById<TextView>(R.id.splashText).text = "CupLink v${BuildConfig.VERSION_NAME}"
    }

    private fun showChangeUsernameDialog() {
        findViewById<Button>(R.id.edit).isSelected = true
        Log.d(this, "showChangeUsernameDialog()")
        val settings = DatabaseCache.database.settings
        val view: View = LayoutInflater.from(this).inflate(R.layout.dialog_change_name, null)
        val nameEditText = view.findViewById<TextInputEditText>(R.id.NameEditText)
        nameEditText.setText(settings.username, TextView.BufferType.EDITABLE)
        val okButton = view.findViewById<Button>(R.id.OkButton)
        val dialog: AlertDialog = createBlurredPPTCDialog(view)
        okButton.setOnClickListener {
            val newUsername = nameEditText.text.toString().trim { it <= ' ' }
            if (Utils.isValidName(newUsername)) {
                settings.username = newUsername
                DatabaseCache.save()
                initViews()
                dialog.cancel()
            } else {
                nameEditText.error = getString(R.string.invalid_name)
                Toast.makeText(this, R.string.invalid_name, Toast.LENGTH_SHORT).show()
            }
        }
        dialog.setOnCancelListener {
            dialog.cancel()
            findViewById<Button>(R.id.edit).isSelected = false
        }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        initViews()
    }
}
