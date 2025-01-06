package org.rivchain.cuplink.rivmesh

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.webkit.URLUtil
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.switchmaterial.SwitchMaterial
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rivchain.cuplink.BuildConfig
import org.rivchain.cuplink.DatabaseCache
import org.rivchain.cuplink.R
import org.rivchain.cuplink.rivmesh.models.PeerInfo
import org.rivchain.cuplink.rivmesh.util.Utils.ping
import java.net.InetAddress
import java.util.Locale
import org.rivchain.cuplink.DatabaseCache.Companion.database

class PeerListActivity : PingPeerListActivity() {

    private var popup: PopupWindow? = null
    private lateinit var adapter: SelectPeerInfoListAdapter

    override fun setAlreadySelectedPeers(alreadySelectedPeers: MutableSet<PeerInfo>) {
        adapter = SelectPeerInfoListAdapter(this, arrayListOf(), alreadySelectedPeers)
        val peerList = findViewById<ListView>(R.id.peerList)
        peerList.adapter = adapter
    }
    override fun addPeer(peerInfo: PeerInfo){
        adapter.addItem(peerInfo)
        if (adapter.count % 5 == 0) {
            adapter.sort()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onPeersCollected() {
        super.onPeersCollected()
        adapter.sort()
    }

    override fun addAlreadySelectedPeers(alreadySelectedPeers: ArrayList<PeerInfo>){
        val sortedSelectedPeers = alreadySelectedPeers.sortedWith(compareBy { it.ping })
        adapter.addAll(0, sortedSelectedPeers)
        adapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_peer_list)
        super.onCreate(savedInstanceState)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
        val settings = database.settings

        val peerSelectionSettings = findViewById<ConstraintLayout>(R.id.peersSelectionSettings)
        val automaticPeersSelectionSwitch = findViewById<SwitchMaterial>(R.id.automaticPeersSelectionSwitch)

        automaticPeersSelectionSwitch.apply {
            isChecked = settings.automaticPeersSelection
            setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                settings.automaticPeersSelection = isChecked
                DatabaseCache.save()
                peerSelectionSettings.visibility = if (automaticPeersSelectionSwitch.isChecked) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }
        }
        peerSelectionSettings.visibility = if (automaticPeersSelectionSwitch.isChecked) {
            View.GONE
        } else {
            View.VISIBLE
        }

        findViewById<Button>(R.id.addPeer).setOnClickListener { _ ->
            addNewPeer()
        }

        findViewById<TextView>(R.id.edit).text = peerListUrl

        findViewById<View>(R.id.edit).setOnClickListener { _ ->
            editPeerListUrl()
        }

        findViewById<View>(R.id.editIcon).setOnClickListener { _ ->
            editPeerListUrl()
        }

        findViewById<TextView>(R.id.splashText).text = "CupLink v${BuildConfig.VERSION_NAME}"
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        super.onBackPressedDispatcher.onBackPressed()
        savePeers()
    }

    private fun savePeers(){
        cancelPeerListPing()
        val selectedPeers = adapter.getSelectedPeers()
        if(selectedPeers.isNotEmpty()) {
            saveSelectedPeers(selectedPeers)
            setResult(RESULT_OK)
        }
    }

    private fun editPeerListUrl() {
        val view: View = LayoutInflater.from(this).inflate(R.layout.dialog_edit_peer_list_url, null)
        val dialog = createBlurredPPTCDialog(view)
        val saveButton = view.findViewById<Button>(R.id.save)
        val urlInput = view.findViewById<TextView>(R.id.urlInput)
        urlInput.text = peerListUrl
        saveButton.setOnClickListener{

            val url = urlInput.text.toString()
            if(!URLUtil.isValidUrl(url)){
                urlInput.error = "The URL is invalid!"
                return@setOnClickListener;
            }
            peerListUrl = url
            preferences.edit().putString(PEER_LIST, peerListUrl).apply()
            dialog.dismiss()
        }
        dialog.show()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun addNewPeer() {
        val view: View = LayoutInflater.from(this).inflate(R.layout.dialog_add_peer, null)
        val dialog = createBlurredPPTCDialog(view)

        val countryCode: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.resources.configuration.locales[0].country
        } else {
            this.resources.configuration.locale.country
        }
        val schemaInput = view.findViewById<TextView>(R.id.schemaInput)
        val ipInput = view.findViewById<TextView>(R.id.ipInput)
        ipInput.requestFocus()
        schemaInput.showSoftInputOnFocus = false
        schemaInput.setOnFocusChangeListener { v, _ ->
            if(schemaInput.isFocused) {
                onClickSchemaList(v)
            }
        }
        schemaInput.setOnClickListener { v->
            onClickSchemaList(v)
        }
        getPopupWindow(
            R.layout.spinner_item,
            resources.getStringArray(R.array.schemas),
            schemaInput
        )
        view.findViewById<CountryCodePicker>(R.id.ccp).setCountryForNameCode(countryCode)
        val addButton = view.findViewById<Button>(R.id.add)

        dialog.setCancelable(true)
        addButton.setOnClickListener{
            val portInput = view.findViewById<TextView>(R.id.portInput)
            val ccpInput = view.findViewById<CountryCodePicker>(R.id.ccp)
            val schema = schemaInput.text.toString().lowercase(Locale.ROOT)
            if(schema.isEmpty()){
                schemaInput.error = "Schema is required"
                return@setOnClickListener
            }
            val ip = ipInput.text.toString().lowercase(Locale.ROOT)
            if(ip.isEmpty()){
                ipInput.error = "IP address is required"
                return@setOnClickListener
            }
            if(portInput.text.isEmpty()){
                portInput.error = "Port is required"
                return@setOnClickListener
            }
            val port = portInput.text.toString().toInt()
            if(port<=0){
                portInput.error = "Port should be > 0"
                return@setOnClickListener
            }
            if(port>=Short.MAX_VALUE){
                portInput.error = "Port should be < "+Short.MAX_VALUE
                return@setOnClickListener
            }
            val ccp = ccpInput.selectedCountryNameCode
            GlobalScope.launch {
                val pi = PeerInfo(schema,
                    withContext(Dispatchers.IO) {
                        InetAddress.getByName(ip)
                    }, port, ccp, false)
                try {
                    val ping = ping(pi.hostName, pi.port)
                    pi.ping = ping
                } catch (e: Throwable){
                    pi.ping = Int.MAX_VALUE
                }
                withContext(Dispatchers.Main) {
                    val selectAdapter = (findViewById<ListView>(R.id.peerList).adapter as SelectPeerInfoListAdapter)
                    selectAdapter.addItem(0, pi)
                    selectAdapter.notifyDataSetChanged()
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
        // Set the width of the dialog to match the screen width
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window!!.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window!!.attributes = layoutParams
    }

    private fun onClickSchemaList(v: View) {
        val height = -1 * v.height +30
        getAddressListPopup()?.showAsDropDown(v, -5, height)
    }

    private fun getAddressListPopup(): PopupWindow? {
        return popup
    }

    private fun getPopupWindow(
        textViewResourceId: Int,
        objects: Array<String>,
        editText: TextView,
    ): PopupWindow {
        // initialize a pop up window type
        val popupWindow = PopupWindow(this)
        // the drop down list is a list view
        val listView = ListView(this)
        listView.dividerHeight = 0
        // set our adapter and pass our pop up window contents
        val adapter = DropDownAdapter(this, textViewResourceId, objects, popupWindow, editText)
        listView.adapter = adapter
        // set the item click listener
        listView.onItemClickListener = adapter
        // some other visual settings
        popupWindow.isFocusable = true
        popupWindow.width = 320
        popupWindow.height = WindowManager.LayoutParams.WRAP_CONTENT
        // set the list view as pop up window content
        popupWindow.contentView = listView
        popup = popupWindow
        return popupWindow
    }

    override fun onPause() {
        super.onPause()
        savePeers()
    }

    override fun onServiceRestart() {
        super.onServiceRestart()
        finish()
    }
}