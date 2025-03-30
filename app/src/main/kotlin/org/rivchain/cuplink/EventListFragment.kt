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

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(refreshEventListReceiver, IntentFilter("refresh_event_list"))

        refreshEventListBroadcast()

        return view
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

    override fun onResume() {
        Log.d(this, "onResume()")
        super.onResume()
        //service.updateNotification()
        NotificationUtils.refreshEvents(this.activity)
    }
}
