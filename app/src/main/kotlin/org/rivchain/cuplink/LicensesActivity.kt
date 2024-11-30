package org.rivchain.cuplink

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class LicensesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_licenses)

        // Use repeatOnLifecycle to launch a coroutine tied to lifecycle state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                try {
                    // Read the license file on a background thread
                    val licenseText = withContext(Dispatchers.IO) {
                        readLicenseFile()
                    }
                    // Update the UI on the main thread
                    findViewById<ProgressBar>(R.id.licenseLoadingBar).visibility = View.GONE
                    findViewById<TextView>(R.id.licenceText).text = licenseText
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun readLicenseFile(): String {
        val buffer = StringBuffer()
        val reader = BufferedReader(InputStreamReader(assets.open("license.txt")))
        reader.use {
            it.forEachLine { line ->
                if (line.trim().isEmpty()) {
                    buffer.append("\n")
                } else {
                    buffer.append(line).append("\n")
                }
            }
        }
        return buffer.toString()
    }
}
