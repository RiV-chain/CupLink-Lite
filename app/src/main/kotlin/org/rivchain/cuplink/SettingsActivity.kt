package org.rivchain.cuplink

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import org.rivchain.cuplink.DatabaseCache.Companion.database
import org.rivchain.cuplink.renderer.DescriptiveTextView
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

    private fun toggleFragmentVisibility(fragment: Fragment, containerId: Int, container: FrameLayout) : Boolean{
        val transaction = supportFragmentManager.beginTransaction()

        transaction.setCustomAnimations(
            R.anim.fragment_fade_in,  // Enter animation
            R.anim.fragment_fade_out, // Exit animation
            R.anim.fragment_fade_in,  // Pop enter animation (when returning)
            R.anim.fragment_fade_out  // Pop exit animation (when leaving)
        )
        if (container.visibility == View.VISIBLE) {
            transaction
                .remove(fragment)
                .commit()
            container.visibility = View.GONE
            return false
        } else {
            transaction
                .replace(containerId, fragment)
                .commit()
            container.visibility = View.VISIBLE
            return true
        }
    }

    private fun initViews() {

        val settings = database.settings

        findViewById<View>(R.id.about)
            .setOnClickListener {
                startActivity(Intent(this, AboutActivity::class.java))
            }

        findViewById<View>(R.id.backup)
            .setOnClickListener {
                startActivity(Intent(this, BackupActivity::class.java))
            }
        findViewById<View>(R.id.clearHistory)
            .setOnClickListener {
                showClearEventsDialog()
        }

        findViewById<View>(R.id.exit)
            .setOnClickListener {
                showConfirmExitDialog()
            }

        val privacyAndSecurityFragment = PrivacyAndSecurityFragment()
        val soundNotificationsFragment = SoundNotificationsFragment()
        val mediaFragment = MediaFragment()
        val networkFragment = NetworkFragment()
        val systemFragment = SystemFragment()
        val qualityFragment = QualityFragment()


        val toggleButtonPrivacy: Button = findViewById(R.id.toggleButtonPrivacy)
        val fragmentContainer1: FrameLayout = findViewById(R.id.fragmentContainer1)
        toggleButtonPrivacy.setOnClickListener { view ->
            val isVisible = toggleFragmentVisibility(privacyAndSecurityFragment, R.id.fragmentContainer1, fragmentContainer1)
            view.isSelected = isVisible
        }

        val soundNotifications: Button = findViewById(R.id.soundNotifications)
        val fragmentContainer2: FrameLayout = findViewById(R.id.fragmentContainer2)
        soundNotifications.setOnClickListener { view ->
            val isVisible = toggleFragmentVisibility(soundNotificationsFragment, R.id.fragmentContainer2, fragmentContainer2)
            view.isSelected = isVisible
        }

        val media: Button = findViewById(R.id.media)
        val fragmentContainer3: FrameLayout = findViewById(R.id.fragmentContainer3)
        media.setOnClickListener { view ->
            val isVisible = toggleFragmentVisibility(mediaFragment, R.id.fragmentContainer3, fragmentContainer3)
            view.isSelected = isVisible
        }

        val network: Button = findViewById(R.id.network)
        val fragmentContainer4: FrameLayout = findViewById(R.id.fragmentContainer4)
        network.setOnClickListener { view ->
            val isVisible = toggleFragmentVisibility(networkFragment, R.id.fragmentContainer4, fragmentContainer4)
            view.isSelected = isVisible
        }

        val system: Button = findViewById(R.id.system)
        val fragmentContainer5: FrameLayout = findViewById(R.id.fragmentContainer5)
        system.setOnClickListener { view ->
            val isVisible = toggleFragmentVisibility(systemFragment, R.id.fragmentContainer5, fragmentContainer5)
            view.isSelected = isVisible
        }

        val quality: Button = findViewById(R.id.quality)
        val fragmentContainer6: FrameLayout = findViewById(R.id.fragmentContainer6)
        quality.setOnClickListener { view ->
            val isVisible = toggleFragmentVisibility(qualityFragment, R.id.fragmentContainer6, fragmentContainer6)
            view.isSelected = isVisible
        }


        findViewById<Button>(R.id.nickname)
            .text = settings.username.ifEmpty { getString(R.string.no_value) }
        findViewById<View>(R.id.nicknameIcon)
            .setOnClickListener {
                showChangeUsernameDialog()
            }

        findViewById<TextView>(R.id.splashText).text = "CupLink v${BuildConfig.VERSION_NAME}"
    }

    private fun showChangeUsernameDialog() {
        findViewById<Button>(R.id.nickname).isSelected = true
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
            findViewById<Button>(R.id.nickname).isSelected = false
        }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        initViews()
    }
}
