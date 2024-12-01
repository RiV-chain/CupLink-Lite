package org.rivchain.cuplink

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.rivchain.cuplink.util.Log
import org.rivchain.cuplink.util.Utils

class AcceptTermsAndConditionsActivity : AppCompatActivity() {

    private var preferences: SharedPreferences? = null
    private val POLICY = "policy"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accept)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }

        val msg: TextView = findViewById(R.id.message)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            msg.text = Html.fromHtml(Utils.readResourceFile(this, R.raw.pp_tc), Html.FROM_HTML_OPTION_USE_CSS_COLORS)
        } else {
            msg.text = Html.fromHtml(Utils.readResourceFile(this, R.raw.pp_tc))
        }

        preferences = this.getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val yesButton = findViewById<Button>(R.id.yes)
        yesButton.setOnClickListener {
            preferences!!.edit().putString(POLICY, "accepted").apply()
            Log.d(this, "Accepted T&C")
            finish()
        }
    }
}