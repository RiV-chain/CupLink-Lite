package org.rivchain.cuplink.rivmesh

import android.content.ComponentName
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.Html
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import org.rivchain.cuplink.BuildConfig
import org.rivchain.cuplink.R
import org.rivchain.cuplink.util.Utils.readResourceFile


open class AutoTestPublicPeerActivity: TestPortActivity() {

    private var port = 0
    private lateinit var publicPeerAgreementDialog: AlertDialog
    private lateinit var agreeCheckbox: MaterialCheckBox
    private lateinit var okButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findViewById<TextView>(R.id.splashText).text = "CupLink v${BuildConfig.VERSION_NAME} © 2023 RiV Chain Ltd"
    }

    override fun onServiceConnected(name: ComponentName?, iBinder: IBinder?) {
        super.onServiceConnected(name, iBinder)
        port = getPublicPeerPort()
        portTest(port)
    }

    override fun portOpen(port: Int) {
        // Inflate the layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_autotest_public_peer_activity, null)

        // Create the AlertDialog
        publicPeerAgreementDialog = createBlurredPPTCDialog(dialogView)
        val msg = dialogView.findViewById<TextView>(R.id.guideInfoText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            msg.text = Html.fromHtml(readResourceFile(this, R.raw.public_peer), Html.FROM_HTML_MODE_COMPACT)
        } else {
            msg.text = Html.fromHtml(readResourceFile(this, R.raw.public_peer))
        }
        agreeCheckbox = dialogView.findViewById(R.id.checkbox)
        okButton = dialogView.findViewById(R.id.OkButton)
        okButton.setOnClickListener {
            // confirm public peer creation
            connectAsPublicPeer(port)
        }
        publicPeerAgreementDialog.setOnCancelListener {
            publicPeerAgreementDialog.dismiss()
            finish()
        }

        agreeCheckbox.addOnCheckedStateChangedListener { materialCheckBox: MaterialCheckBox, i: Int ->
            okButton.isEnabled = materialCheckBox.isChecked
        }

        // Show the dialog
        publicPeerAgreementDialog.show()
    }

    override fun portClosed(port: Int) {
        finish()
    }

    override fun connectedAsPublicPeer(port: Int) {
        finish()
    }

    override fun notConnectedAsPublicPeer(port: Int) {
        //do nothing and retry to connect from dialog
    }

}