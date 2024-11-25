package org.rivchain.cuplink.rivmesh

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textview.MaterialTextView
import org.rivchain.cuplink.R
import org.rivchain.cuplink.rivmesh.models.PeerInfo
import org.rivchain.cuplink.rivmesh.models.Status
import kotlin.math.exp
import kotlin.math.pow

class ProgressSelectPeerActivity: AutoSelectPeerActivity() {

    private lateinit var currentStageTextView: MaterialTextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var progressDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty)
        // Inflate the layout for the dialog
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null)

        // Create the AlertDialog
        progressDialog = createBlurredProgressDialog(dialogView)
        progressDialog.setCancelable(false)
        currentStageTextView = dialogView.findViewById(R.id.current_stage)
        progressBar = dialogView.findViewById(R.id.progress_bar)

        // Show the dialog
        progressDialog.show()

        // Initialize progress
        updateProgress()
    }

    override fun updateProgress() {
        if (maxPeersSelection > 0) {
            val progress = (addedPeers.toFloat() / maxPeersSelection) * 100
            progressBar.progress = progress.toInt()
            currentStageTextView.text = "Adding Peers ($addedPeers / $maxPeersSelection)"
        }
    }
}