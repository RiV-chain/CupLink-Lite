package org.rivchain.cuplink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        title = getString(R.string.menu_about)

        val toolbar = findViewById<Toolbar>(R.id.about_toolbar)
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

        findViewById<View>(R.id.termsOfService).setOnClickListener {
            val intent = Intent(this, TermsAndConditionsActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.privacyPolicy).setOnClickListener {
            val intent = Intent(this, TermsAndConditionsActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.licenses).setOnClickListener {
            val intent = Intent(this, LicenseActivity::class.java)
            startActivity(intent)
        }
        findViewById<View>(R.id.docs).setOnClickListener {

        }
        findViewById<View>(R.id.x).setOnClickListener {
            openWebsite(R.string.x_website)
        }
        findViewById<View>(R.id.gitHub).setOnClickListener {
            openWebsite(R.string.app_website)
        }
        findViewById<View>(R.id.linkedIn).setOnClickListener {

        }
        findViewById<View>(R.id.telegram).setOnClickListener {
            openWebsite(R.string.telegram_website)
        }
        findViewById<TextView>(R.id.splashText).text = "CupLink v${BuildConfig.VERSION_NAME} © 2024 RiV Chain Ltd"
    }

    private fun openWebsite(id: Int){
        val url = getString(id) // Get URL from strings.xml
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        }

        // Verify that the intent resolves to an activity
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showMessage("No application can handle this request.")
        }
    }
}