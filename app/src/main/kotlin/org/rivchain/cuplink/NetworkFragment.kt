package org.rivchain.cuplink

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import org.json.JSONArray
import org.rivchain.cuplink.DatabaseCache.Companion.database
import org.rivchain.cuplink.renderer.DescriptiveTextView
import org.rivchain.cuplink.rivmesh.ConfigurePublicPeerActivity
import org.rivchain.cuplink.rivmesh.PeerListActivity
import org.rivchain.cuplink.rivmesh.PingPeerListActivity.Companion.PEER_LIST
import org.rivchain.cuplink.rivmesh.models.PeerInfo
import org.rivchain.cuplink.rivmesh.util.Utils.serializePeerInfoSet2StringList
import org.rivchain.cuplink.util.Utils

class NetworkFragment: Fragment(R.layout.fragment_settings_network) {

    private var currentPeers = setOf<PeerInfo>()
    private var requestListenLauncher: ActivityResultLauncher<Intent>? = null
    private var requestPeersLauncher: ActivityResultLauncher<Intent>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settings = database.settings

        requestPeersLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == RESULT_OK) {
                    (activity as BaseActivity).restartService()
                }
            }
        requestListenLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                view.findViewById<SwitchMaterial>(R.id.publicPeerLayoutSwitch).isChecked = result.resultCode == RESULT_OK
                val publicPeerUrl = view.findViewById<DescriptiveTextView>(R.id.publicPeerLayout)
                publicPeerUrl.subtitleTextView.text = jsonArrayToString(database.mesh.getListen())
                // refresh settings
                DatabaseCache.save()
                (activity as BaseActivity).restartService()
            }

        view.findViewById<DescriptiveTextView>(R.id.addressLayout)
            .subtitleTextView.text = if (settings.addresses.isEmpty()) getString(R.string.no_value) else settings.addresses.joinToString()
        view.findViewById<View>(R.id.addressLayout)
            .setOnClickListener {
                startActivity(Intent(activity, AddressManagementActivity::class.java))
            }
        view.findViewById<DescriptiveTextView>(R.id.publicKeyLayout)
            .subtitleTextView.text = Utils.byteArrayToHexString(settings.publicKey)
        view.findViewById<View>(R.id.publicKeyLayout)
            .setOnClickListener { Toast.makeText(activity, R.string.setting_read_only, Toast.LENGTH_SHORT).show() }

        val peersNumber = view.findViewById<TextView>(R.id.configuredPeers)
        peersNumber.text = database.mesh.getPeers().length().toString()
        peersNumber.setOnClickListener {
            val intent = Intent(activity, PeerListActivity::class.java)
            intent.putStringArrayListExtra(PEER_LIST, serializePeerInfoSet2StringList(currentPeers))
            requestPeersLauncher!!.launch(intent)
        }
        view.findViewById<SwitchMaterial>(R.id.searchMulticastPeersSwitch).apply {
            isChecked = database.mesh.multicastListen
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                database.mesh.multicastListen = isChecked
                DatabaseCache.save()
                (activity as BaseActivity).restartService()
            }
        }
        view.findViewById<SwitchMaterial>(R.id.discoverableOverMulticastSwitch).apply {
            isChecked = database.mesh.multicastBeacon
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                database.mesh.multicastBeacon = isChecked
                DatabaseCache.save()
                (activity as BaseActivity).restartService()
            }
        }
        val publicPeerUrl = view.findViewById<DescriptiveTextView>(R.id.publicPeerLayout)
        val url = jsonArrayToString(database.mesh.getListen())
        if(url.isNotEmpty()) {
            publicPeerUrl.subtitleTextView.text = url
        }
        view.findViewById<SwitchMaterial>(R.id.publicPeerLayoutSwitch).apply {
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    val intent =
                        Intent(activity, ConfigurePublicPeerActivity::class.java)
                    requestListenLauncher!!.launch(intent)
                } else {
                    database.mesh.setListen(setOf())
                    val disabledColor = Color.parseColor("#d3d3d3")
                    publicPeerUrl.subtitleTextView.setTextColor(disabledColor)
                    DatabaseCache.save()
                    (activity as BaseActivity).restartService()
                }
            }
        }
        view.findViewById<SwitchMaterial>(R.id.automaticStatusUpdatesSwitch).apply {
            isChecked = settings.automaticStatusUpdates
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                settings.automaticStatusUpdates = isChecked
                DatabaseCache.save()
            }
        }
        view.findViewById<SwitchMaterial>(R.id.useNeighborTableSwitch).apply {
            isChecked = settings.useNeighborTable
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                settings.useNeighborTable = isChecked
                DatabaseCache.save()
            }
        }
    }

    private fun jsonArrayToString(listen: JSONArray): String {
        val stringBuilder = StringBuilder()

        for (i in 0 until listen.length()) {
            if (i > 0) {
                stringBuilder.append(", ")
            }
            stringBuilder.append(listen.get(i))
        }

        return stringBuilder.toString()
    }

}