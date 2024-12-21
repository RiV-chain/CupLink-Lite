package org.rivchain.cuplink

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import org.rivchain.cuplink.DatabaseCache.Companion.database

class PrivacyAndSecurityFragment: Fragment(R.layout.fragment_settings_privacy_and_security) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settings = database.settings

        view.findViewById<SwitchMaterial>(R.id.blockUnknownSwitch).apply {
            isChecked = settings.blockUnknown
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                settings.blockUnknown = isChecked
                DatabaseCache.save()
            }
        }

        val databasePassword = DatabaseCache.databasePassword
        view.findViewById<TextView>(R.id.databasePasswordTv)
            .text = if (databasePassword.isEmpty()) getString(R.string.no_value) else "*".repeat(databasePassword.length)
        view.findViewById<View>(R.id.databasePasswordLayout)
            .setOnClickListener { showDatabasePasswordDialog() }

        val menuPassword = settings.menuPassword
        view.findViewById<TextView>(R.id.menuPasswordTv)
            .text = if (menuPassword.isEmpty()) getString(R.string.no_value) else "*".repeat(menuPassword.length)
        view.findViewById<View>(R.id.menuPasswordLayout)
            .setOnClickListener { showMenuPasswordDialog() }

        view.findViewById<SwitchMaterial>(R.id.autoAcceptCallsSwitch).apply {
            isChecked = settings.autoAcceptCalls
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                if(Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(activity) && isChecked) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$activity.packageName")
                    )
                    requestDrawOverlaysPermissionLauncher.launch(intent)
                } else {
                    settings.autoAcceptCalls = isChecked
                    DatabaseCache.save()
                }
            }
        }

    }

    private fun showDatabasePasswordDialog() {
        val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_change_database_password, null)
        val dialog = (activity as BaseActivity).createBlurredPPTCDialog(view)
        val passwordEditText = view.findViewById<TextInputEditText>(R.id.DatabasePasswordEditText)
        val okButton = view.findViewById<Button>(R.id.OkButton)
        passwordEditText.setText(DatabaseCache.databasePassword)
        okButton.setOnClickListener {
            val newPassword = passwordEditText.text.toString()
            DatabaseCache.databasePassword = newPassword
            DatabaseCache.save()
            DatabaseCache.dbEncrypted = newPassword != ""
            Toast.makeText(activity, R.string.done, Toast.LENGTH_SHORT).show()
            dialog.cancel()
        }
        dialog.show()
    }

    private fun showMenuPasswordDialog() {
        val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_change_menu_password, null)
        val dialog = (activity as BaseActivity).createBlurredPPTCDialog(view)
        val passwordEditText = view.findViewById<TextInputEditText>(R.id.MenuPasswordEditText)
        val okButton = view.findViewById<Button>(R.id.OkButton)
        passwordEditText.setText(database.settings.menuPassword)
        okButton.setOnClickListener {
            val newPassword = passwordEditText.text.toString()
            database.settings.menuPassword = newPassword
            DatabaseCache.save()
            Toast.makeText(activity, R.string.done, Toast.LENGTH_SHORT).show()
            dialog.cancel()
        }
        dialog.show()
    }

    private var requestDrawOverlaysPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (!Settings.canDrawOverlays(activity)) {
                    Toast.makeText(activity, R.string.overlay_permission_missing, Toast.LENGTH_LONG).show()
                }
            }
        } else {
            database.settings.autoAcceptCalls = true
            DatabaseCache.save()
        }
    }
}