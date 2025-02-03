package org.rivchain.cuplink

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.textfield.TextInputEditText
import org.rivchain.cuplink.adapter.EventExpandableListAdapter
import org.rivchain.cuplink.call.CallActivity
import org.rivchain.cuplink.model.Event
import org.rivchain.cuplink.util.Log
import org.rivchain.cuplink.util.Utils


class EventListFragment() : Fragment() {

    private lateinit var activity: BaseActivity
    private lateinit var eventListAdapter: EventExpandableListAdapter
    private lateinit var eventListView: ExpandableListView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_event_list, container, false)
        eventListView = view.findViewById(R.id.eventList)
        activity = requireActivity() as BaseActivity

        eventListAdapter = EventExpandableListAdapter(activity, emptyList(), emptyList())
        eventListView.setAdapter(eventListAdapter)
        eventListView.setOnChildClickListener(onEventClickListener)
        eventListView.setOnItemLongClickListener(onEventLongClickListener)

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(refreshEventListReceiver, IntentFilter("refresh_event_list"))

        refreshEventListBroadcast()

        return view
    }

    private val onEventClickListener = ExpandableListView.OnChildClickListener { _, _, groupPosition, childPosition, _ ->
        Log.d(this, "onChildClick")

        val eventGroup = eventListAdapter.getChild(groupPosition, childPosition)
        // get last event that has an address
        val latestEvent = eventGroup.lastOrNull { it.address != null } ?: eventGroup.last()

        val contact = DatabaseCache.database.contacts.getContactByPublicKey(latestEvent.publicKey)
            ?: latestEvent.createUnknownContact("")

        if (contact.addresses.isEmpty()) {
            Toast.makeText(activity, R.string.contact_has_no_address_warning, Toast.LENGTH_SHORT)
                .show()
        } else {
            // start call
            Log.d(this, "start CallActivity")
            val intent = Intent(activity, CallActivity::class.java)
            intent.action = "ACTION_OUTGOING_CALL"
            intent.putExtra("EXTRA_CONTACT", contact)
            startActivity(intent)
        }

        true
    }

    private val onEventLongClickListener = AdapterView.OnItemLongClickListener { _, _, i, id ->
        Log.d(this, "onItemLongClick")

        if (ExpandableListView.getPackedPositionType(id) == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            val groupPosition = ExpandableListView.getPackedPositionGroup(id)
            val childPosition = ExpandableListView.getPackedPositionChild(id)

            // You now have everything that you would as if this was an OnChildClickListener()
            // Add your logic here.

            // Return true as we are handling the event.
            val eventGroup = eventListAdapter.getChild(groupPosition, childPosition)
            val latestEvent = eventGroup.last()
            val res = resources
            val add = res.getString(R.string.contact_menu_add)
            val delete = res.getString(R.string.contact_menu_delete)
            val block = res.getString(R.string.contact_menu_block)
            val unblock = res.getString(R.string.contact_menu_unblock)
            val contact = DatabaseCache.database.contacts.getContactByPublicKey(latestEvent.publicKey)

            // Create a list of options
            val options = mutableListOf<String>()

            // Allow to add unknown caller
            if (contact == null) {
                options.add(add)
            }

            if (contact != null) {
                if (contact.blocked) {
                    options.add(unblock)
                } else {
                    options.add(block)
                }
            }

            options.add(delete)

            // Inflate the dialog layout
            val inflater = LayoutInflater.from(activity)
            val dialogView = inflater.inflate(R.layout.dialog_select_one_listview_item, null)
            val listViewEventOptions: ListView = dialogView.findViewById(R.id.listView)

            // Create an ArrayAdapter
            val adapter = ArrayAdapter(activity, android.R.layout.simple_list_item_1, options)
            listViewEventOptions.adapter = adapter

            // Create and show the dialog
            val dialog = activity.createBlurredPPTCDialog(dialogView)
            listViewEventOptions.setOnItemClickListener { _, _, position, _ ->
                val selectedOption = options[position]
                when (selectedOption) {
                    add -> {
                        showAddDialog(eventGroup)
                    }

                    block -> {
                        setBlocked(latestEvent, true)
                    }

                    unblock -> {
                        setBlocked(latestEvent, false)
                    }

                    delete -> {
                        deleteEventGroup(eventGroup)
                    }
                }
                dialog.dismiss()
            }

            dialog.show()

            return@OnItemLongClickListener true
        }

        return@OnItemLongClickListener false
    }

    private val refreshEventListReceiver = object : BroadcastReceiver() {
        //private var lastTimeRefreshed = 0L

        override fun onReceive(context: Context, intent: Intent) {
            Log.d(this@EventListFragment, "trigger refreshEventList() from broadcast at ${this@EventListFragment.lifecycle.currentState}")
            // prevent this method from being called too often
            //val now = System.currentTimeMillis()
            //if ((now - lastTimeRefreshed) > 1000) {
            //    lastTimeRefreshed = now
                refreshEventList()
            //}
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshEventListReceiver)
        super.onDestroy()
    }

    private fun refreshEventList() {
        Log.d(this, "refreshEventList")

        val activity = requireActivity() as MainActivity
        val events = DatabaseCache.database.events.eventList
        val contacts = DatabaseCache.database.contacts.contactList

        activity.runOnUiThread {
            activity.updateEventTabTitle()

            eventListAdapter.update(events, contacts)
            eventListAdapter.notifyDataSetChanged()
            eventListView.setAdapter(eventListAdapter)
            for (i in 0 until eventListAdapter.getGroupCount()) eventListView.expandGroup(i)
        }
    }

    private fun refreshEventListBroadcast() {
        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(Intent("refresh_event_list"))
    }

    // only available for known contacts
    private fun setBlocked(event: Event, blocked: Boolean) {

        val contact = DatabaseCache.database.contacts.getContactByPublicKey(event.publicKey)
        if (contact != null) {
            contact.blocked = blocked
            DatabaseCache.save()
            LocalBroadcastManager.getInstance(requireContext())
                .sendBroadcast(Intent("refresh_contact_list"))
            LocalBroadcastManager.getInstance(requireContext())
                .sendBroadcast(Intent("refresh_event_list"))
        } else {
            // ignore - not expected to happen
        }
    }

    override fun onResume() {
        Log.d(this, "onResume()")
        super.onResume()
        //service.updateNotification()
        NotificationUtils.refreshEvents(this.activity)
    }

    private fun deleteEventGroup(eventGroup: List<Event>) {
        Log.d(this, "removeEventGroup()")
        activity.deleteEvents(eventGroup.map { it.date })
    }

    // only available for unknown contacts
    private fun showAddDialog(eventGroup: List<Event>) {
        Log.d(this, "showAddDialog")
        val activity = requireActivity() as MainActivity
        // prefer latest event that has an address
        val latestEvent = eventGroup.lastOrNull { it.address != null } ?: eventGroup.last()
        val view: View = LayoutInflater.from(activity).inflate(R.layout.dialog_add_contact, null)
        val dialog = activity.createBlurredPPTCDialog(view)
        val nameEditText = view.findViewById<TextInputEditText>(R.id.NameEditText)
        val okButton = view.findViewById<Button>(R.id.OkButton)
        okButton.setOnClickListener {
            val name = nameEditText.text.toString()
            if (!Utils.isValidName(name)) {
                Toast.makeText(activity, R.string.contact_name_invalid, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (DatabaseCache.database.contacts.getContactByName(name) != null) {
                Toast.makeText(activity, R.string.contact_name_exists, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val contact = latestEvent.createUnknownContact(name)
            activity.addContact(contact)

            Toast.makeText(activity, R.string.done, Toast.LENGTH_SHORT).show()

            // close dialog
            dialog.dismiss()
        }
        dialog.show()
    }
}
