package org.rivchain.cuplink.adapter

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ArrayAdapter
import android.widget.BaseExpandableListAdapter
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.textfield.TextInputEditText
import org.rivchain.cuplink.BaseActivity
import org.rivchain.cuplink.DatabaseCache
import org.rivchain.cuplink.MainActivity
import org.rivchain.cuplink.QRShowActivity
import org.rivchain.cuplink.R
import org.rivchain.cuplink.call.CallActivity
import org.rivchain.cuplink.model.Contact
import org.rivchain.cuplink.model.Event
import org.rivchain.cuplink.util.Log
import org.rivchain.cuplink.util.RlpUtils
import org.rivchain.cuplink.util.Utils
import java.text.SimpleDateFormat
import java.util.Locale

internal class EventExpandableListAdapter(
    private val context: Context,
    events: List<Event>,
    private var contacts: List<Contact>
) : BaseExpandableListAdapter() {

    var groupedEvents = groupAndCompactEvents(events)

    fun update(events: List<Event>, contacts: List<Contact>) {
        Log.d(this, "update() events=${events.size}, contacts=${contacts.size}")
        this.groupedEvents = groupAndCompactEvents(events)
        this.contacts = contacts
        notifyDataSetChanged()
    }

    override fun getGroupCount(): Int = groupedEvents.size

    override fun getChildrenCount(groupPosition: Int): Int = groupedEvents[groupPosition].second.size

    override fun getGroup(groupPosition: Int): String = groupedEvents[groupPosition].first

    override fun getChild(groupPosition: Int, childPosition: Int): Event = groupedEvents[groupPosition].second[childPosition]

    override fun getGroupId(groupPosition: Int): Long = groupPosition.toLong()

    override fun getChildId(groupPosition: Int, childPosition: Int): Long = childPosition.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_event_group, parent, false)
        val headerTv = view.findViewById<TextView>(R.id.call_header)
        headerTv.text = getGroup(groupPosition)
        return view
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_event, parent, false)
        val event = getChild(groupPosition, childPosition)

        val latestEvent = this.getChild(groupPosition, childPosition)

        val contact = DatabaseCache.database.contacts.getContactByPublicKey(latestEvent.publicKey)
            ?: latestEvent.createUnknownContact(view.context.getString(R.string.unknown_caller))

        val nameTv = view.findViewById<TextView>(R.id.call_name)
        nameTv.text = contact.name.lowercase().replaceFirstChar {
            it.titlecase()
        }

        val dateTV = view.findViewById<TextView>(R.id.call_date)
        val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        dateTV.text = dateTimeFormatter.format(event.date)

        val iconsView = view.findViewById<LinearLayout>(R.id.call_icons)
        iconsView.removeAllViews()
        appendIcon(iconsView, event.type)

        val shareButton = view.findViewById<ImageButton>(R.id.share_button)
        val qrButton = view.findViewById<ImageButton>(R.id.qr_button)

        nameTv.setOnClickListener {

            Log.d(this, "onChildClick")

            if (contact.addresses.isEmpty()) {
                Toast.makeText(context, R.string.contact_has_no_address_warning, Toast.LENGTH_SHORT)
                    .show()
            } else {
                val intent = Intent(context, CallActivity::class.java)
                if (CallActivity.isCallInProgress) {
                    Log.d(this, "bring CallActivity to front")
                    // Redirect to CallActivity if a call is active
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                } else {
                    Log.d(this, "start CallActivity")
                    intent.action = "ACTION_OUTGOING_CALL"
                    intent.putExtra("EXTRA_CONTACT", contact)
                }
                context.startActivity(intent)
            }
        }

        nameTv.setOnLongClickListener {
            Log.d(this, "onItemLongClick")

            val res = (context as BaseActivity).resources
            val add = res.getString(R.string.contact_menu_add)
            val delete = res.getString(R.string.contact_menu_delete)
            val block = res.getString(R.string.contact_menu_block)
            val unblock = res.getString(R.string.contact_menu_unblock)
            val contact = DatabaseCache.database.contacts.getContactByPublicKey(event.publicKey)

            val options = mutableListOf<String>()
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

            val inflater = LayoutInflater.from(context)
            val dialogView = inflater.inflate(R.layout.dialog_select_one_listview_item, null)
            val listViewEventOptions: ListView = dialogView.findViewById(R.id.listView)

            val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, options)
            listViewEventOptions.adapter = adapter

            val dialog = context.createBlurredPPTCDialog(dialogView)
            listViewEventOptions.setOnItemClickListener { _, _, position, _ ->
                val selectedOption = options[position]
                when (selectedOption) {
                    add -> showAddDialog(event)
                    block -> setBlocked(event, true)
                    unblock -> setBlocked(event, false)
                    delete -> deleteEventGroup(event)
                }
                dialog.dismiss()
            }

            dialog.show()
            true
        }

        shareButton.setOnClickListener {
            try {
                Thread {
                    val data = RlpUtils.generateLink(contact)
                    val i = Intent(Intent.ACTION_SEND)
                    i.putExtra(Intent.EXTRA_TEXT, data)
                    i.type = "text/plain"
                    context.startActivity(i)
                }.start()
            } catch (e: Exception) {
                // ignore
            }
        }

        qrButton.setOnClickListener {
            try {
                Thread {
                    val deeplink = RlpUtils.generateLink(contact)
                    val intent = Intent(context, QRShowActivity::class.java)
                    intent.putExtra("EXTRA_CONTACT_DEEPLINK", deeplink)
                    context.startActivity(intent)
                }.start()
            } catch (e: Exception) {
                // ignore
            }
        }

        return view
    }

    private fun deleteEventGroup(latestEvent: Event) {
        Log.d(this, "removeEventGroup()")
        (context as BaseActivity).deleteEvents(listOf(latestEvent.date))
    }

    // only available for known contacts
    private fun setBlocked(event: Event, blocked: Boolean) {

        val contact = DatabaseCache.database.contacts.getContactByPublicKey(event.publicKey)
        if (contact != null) {
            contact.blocked = blocked
            DatabaseCache.save()
            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(Intent("refresh_contact_list"))
            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(Intent("refresh_event_list"))
        } else {
            // ignore - not expected to happen
        }
    }

    // only available for unknown contacts
    private fun showAddDialog(latestEvent: Event) {
        Log.d(this, "showAddDialog")
        val activity = context as MainActivity
        // prefer latest event that has an address
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

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean = true

    private fun appendIcon(iconsView: LinearLayout, eventType: Event.Type) {
        val imageView = ImageView(iconsView.context)
        imageView.setImageResource(
            when (eventType) {
                Event.Type.UNKNOWN -> R.drawable.ic_incoming_call_error
                Event.Type.INCOMING_ACCEPTED -> R.drawable.ic_incoming_call_accepted
                Event.Type.INCOMING_MISSED -> R.drawable.ic_incoming_call_missed
                Event.Type.INCOMING_ERROR -> R.drawable.ic_incoming_call_error
                Event.Type.OUTGOING_ACCEPTED -> R.drawable.ic_outgoing_call_accepted
                Event.Type.OUTGOING_MISSED -> R.drawable.ic_outgoing_call_missed
                Event.Type.OUTGOING_ERROR -> R.drawable.ic_outgoing_call_error
            }
        )
        imageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        iconsView.addView(imageView)
    }

    companion object {
        fun groupAndCompactEvents(events: List<Event>): List<Pair<String, List<Event>>> {
            val groupedEvents = mutableMapOf(
                "Today" to mutableListOf<Event>(),
                "Yesterday" to mutableListOf<Event>(),
                "Older" to mutableListOf<Event>()
            )

            val now = System.currentTimeMillis()
            val todayStart = now - (now % DateUtils.DAY_IN_MILLIS)
            val yesterdayStart = todayStart - DateUtils.DAY_IN_MILLIS

            // For tracking unique groups per contact per day
            val contactDayMap = mutableMapOf<String, MutableMap<String, MutableList<Event>>>()

            for (event in events.sortedByDescending { it.date.time }) {
                val eventTime = event.date.time
                val category = when {
                    eventTime >= todayStart -> "Today"
                    eventTime >= yesterdayStart -> "Yesterday"
                    else -> "Older"
                }

                val contactKey = Utils.byteArrayToHexString(event.publicKey)
                val dayKey = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(event.date)

                val dayMap = contactDayMap.getOrPut(category) { mutableMapOf() }
                val key = "$contactKey-$dayKey"

                dayMap.getOrPut(key) { mutableListOf() }.add(event)
            }

            // Flatten the grouped maps into the list
            for ((category, dayMap) in contactDayMap) {
                val eventList = groupedEvents[category]!!
                for ((_, eventsPerContactDay) in dayMap) {
                    // Sort each group and add all to list
                    eventsPerContactDay.sortByDescending { it.date.time }
                    eventList.addAll(eventsPerContactDay)
                }
            }

            return groupedEvents.entries.map { it.key to it.value }
        }
    }
}
