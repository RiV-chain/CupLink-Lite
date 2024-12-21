package org.rivchain.cuplink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import org.rivchain.cuplink.DatabaseCache.Companion.database
import java.lang.Integer.parseInt

class SystemFragment: Fragment(R.layout.fragment_settings_system) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settings = database.settings

        view.findViewById<SwitchMaterial>(R.id.startOnBootupSwitch).apply {
            isChecked = settings.startOnBootup
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                settings.startOnBootup = isChecked
                BootUpReceiver.setEnabled(requireContext(), isChecked) // apply setting
                DatabaseCache.save()
            }
        }
        view.findViewById<SwitchMaterial>(R.id.ignoreBatteryOptimizationsSwitch).apply {
            isChecked = settings.ignoreBatteryOptimizations
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                settings.ignoreBatteryOptimizations = isChecked
                DatabaseCache.save()
            }
        }
        view.findViewById<TextView>(R.id.connectTimeoutTv)
            .text = "${settings.connectTimeout}"
        view.findViewById<View>(R.id.connectTimeoutLayout)
            .setOnClickListener { showChangeConnectTimeoutDialog() }
        view.findViewById<TextView>(R.id.connectRetriesTv)
            .text = "${settings.connectRetries}"
        view.findViewById<View>(R.id.connectRetriesLayout)
            .setOnClickListener { showChangeConnectRetriesDialog() }
    }

    private fun showChangeConnectTimeoutDialog() {
        val settings = database.settings
        val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_change_connect_timeout, null)
        val dialog = (activity as BaseActivity).createBlurredPPTCDialog(view)
        val connectTimeoutEditText = view.findViewById<TextView>(R.id.ConnectTimeoutEditText)
        val okButton = view.findViewById<Button>(R.id.OkButton)
        connectTimeoutEditText.text = "${settings.connectTimeout}"
        okButton.setOnClickListener {
            val minValue = 20
            val maxValue = 4000
            var connectTimeout = -1
            val text = connectTimeoutEditText.text.toString()
            try {
                connectTimeout = parseInt(text)
            } catch (e: Exception) {
                // ignore
            }

            if (connectTimeout in minValue..maxValue) {
                settings.connectTimeout = connectTimeout
                DatabaseCache.save()
                Toast.makeText(activity, R.string.done, Toast.LENGTH_SHORT).show()
            } else {
                val message = String.format(getString(R.string.invalid_number), minValue, maxValue)
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }

            dialog.cancel()
        }
        dialog.show()
    }

    private fun showChangeConnectRetriesDialog() {
        val settings = database.settings
        val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_change_connect_retries, null)
        val dialog = (activity as BaseActivity).createBlurredPPTCDialog(view)
        val connectRetriesEditText = view.findViewById<TextView>(R.id.ConnectRetriesEditText)
        val okButton = view.findViewById<Button>(R.id.OkButton)
        connectRetriesEditText.text = "${settings.connectRetries}"
        okButton.setOnClickListener {
            val minValue = 0
            val maxValue = 4
            var connectRetries = -1
            val text = connectRetriesEditText.text.toString()
            try {
                connectRetries = parseInt(text)
            } catch (e: Exception) {
                // ignore
            }

            if (connectRetries in minValue..maxValue) {
                settings.connectRetries = connectRetries
                DatabaseCache.save()
                Toast.makeText(activity, R.string.done, Toast.LENGTH_SHORT).show()
            } else {
                val message = String.format(getString(R.string.invalid_number), minValue, maxValue)
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }

            dialog.cancel()
        }
        dialog.show()
    }

}