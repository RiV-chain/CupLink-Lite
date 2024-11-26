package org.rivchain.cuplink.rivmesh

import android.os.Bundle
import org.rivchain.cuplink.R
import org.rivchain.cuplink.rivmesh.models.PeerInfo
import org.rivchain.cuplink.rivmesh.models.Status
import kotlin.math.exp
import kotlin.math.pow

open class AutoSelectPeerActivity: PingPeerListActivity() {

    var addedPeers = 0

    var maxPeersSelection = 3

    private val selectedPeers = ArrayList<PeerInfo>()

    override fun peersMap(peersMap: Map<String, Map<String, Status>>) {
        // Calculate total peers with up status
        var totalProbablyOnlinePeers = 0
        for ((country, peers) in peersMap.entries) {
            for ((peer, status) in peers) {
                if (status.up) {
                    totalProbablyOnlinePeers++
                }
            }
        }
        if(totalProbablyOnlinePeers == 0){
            // TODO show up a warning here
            finish()
        }
        if(totalProbablyOnlinePeers < this@AutoSelectPeerActivity.maxPeersSelection){
            this@AutoSelectPeerActivity.maxPeersSelection = totalProbablyOnlinePeers
        }
    }

    override fun addPeer(peerInfo: PeerInfo){
        addedPeers++
        selectedPeers.add(peerInfo)
        // Update progress
        updateProgress()
    }

    override fun addAlreadySelectedPeers(alreadySelectedPeers: ArrayList<PeerInfo>){
        selectedPeers.addAll(alreadySelectedPeers.sortedWith(compareBy { it.ping }))
        if (selectedPeers.size == 0) {
            // TODO show up a warning here
            finish()
        }
        // Do peers selection
        super.saveSelectedPeers(selectPeers(selectedPeers))

        finish()
    }

    private fun selectPeers(currentPeers: ArrayList<PeerInfo>): Set<PeerInfo> {
        val selectedPeers = mutableSetOf<PeerInfo>()
        val size = currentPeers.size
        if (size <= maxPeersSelection) {
            return currentPeers.toSet()
        } else {
            // Calculate selection probabilities for the top 3 peers
            val topProbabilities = ArrayList<Double>()
            for (i in 0 until maxPeersSelection) {
                topProbabilities.add(1.0)
            }
            // Calculate total probability for normalization
            val totalProbability = topProbabilities.sum()

            // Select the top 3 peers
            for (i in 0 until maxPeersSelection) {
                var selection = (totalProbability * Math.random()).toFloat()
                for (j in topProbabilities.indices) {
                    selection -= topProbabilities[j].toFloat()
                    if (selection <= 0) {
                        selectedPeers.add(currentPeers[j])
                        break
                    }
                }
            }

            // If more peers need to be selected, consider the remaining peers
            if (size > maxPeersSelection) {
                val remainingPeers = currentPeers.subList(maxPeersSelection, size)
                val remainingSize = remainingPeers.size
                // Calculate selection probabilities for the remaining peers based on normal distribution
                val remainingProbabilities = ArrayList<Double>()
                for (i in maxPeersSelection until size) {
                    val probability = exp(-((i - maxPeersSelection).toDouble() / remainingSize.toDouble()).pow(2))
                    remainingProbabilities.add(probability)
                }
                val maxRemainingProbability = remainingProbabilities.maxOrNull() ?: 1.0
                for (i in remainingProbabilities.indices) {
                    remainingProbabilities[i] /= maxRemainingProbability // Normalize probabilities
                }

                // Select remaining peers based on probabilities until 3 peers are selected
                for (i in 0 until (maxPeersSelection - selectedPeers.size)) {
                    var selection = (totalProbability * Math.random()).toFloat()
                    for (j in remainingProbabilities.indices) {
                        selection -= remainingProbabilities[j].toFloat()
                        if (selection <= 0) {
                            selectedPeers.add(remainingPeers[j])
                            break
                        }
                    }
                }
            }
        }
        return selectedPeers
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empty)
        // Initialize progress
        updateProgress()
    }

    protected open fun updateProgress() {

    }
}