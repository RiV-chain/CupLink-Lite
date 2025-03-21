package org.rivchain.cuplink.adapter

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.rivchain.cuplink.R
import org.rivchain.cuplink.model.Contact
import org.rivchain.cuplink.model.Event
import org.rivchain.cuplink.util.Log
import org.rivchain.cuplink.util.NetworkUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

internal class EventExpandableListAdapter(
    private val context: Context,
    private var events: List<Event>,
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

        // find name
        val name = contacts.find { it.publicKey.contentEquals(event.publicKey) }?.name

        val nameTv = view.findViewById<TextView>(R.id.call_name)
        if (name.isNullOrEmpty()) {
            nameTv.text = view.context.getString(R.string.unknown_caller)
        } else {
            nameTv.text = name
        }

        // display event date and time
        val dateTV = view.findViewById<TextView>(R.id.call_date)
        val dateTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        dateTV.text = dateTimeFormatter.format(event.date)

        // add one icon for each event type add one extra for the last event
        val iconsView = view.findViewById<LinearLayout>(R.id.call_icons)
        iconsView.removeAllViews()
        appendIcon(iconsView, event.type)

        return view
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

        // group consecutive events of a single contact
        fun groupAndCompactEvents(events: List<Event>): List<Pair<String, List<Event>>> {
            val groupedEvents = mutableMapOf(
                "Today" to mutableListOf<Event>(),
                "Yesterday" to mutableListOf<Event>(),
                "Older" to mutableListOf<Event>()
            )
            val now = System.currentTimeMillis()
            val todayStart = now - (now % DateUtils.DAY_IN_MILLIS)
            val yesterdayStart = todayStart - DateUtils.DAY_IN_MILLIS

            var lastEvent: Event? = null
            var lastEventList: MutableList<Event>? = null
            var lastCategory: String? = null

            for (event in events) {
                val eventTime = event.date.time
                val category = when {
                    eventTime >= todayStart -> "Today"
                    eventTime >= yesterdayStart -> "Yesterday"
                    else -> "Older"
                }

                if (lastEvent == null || lastEventList == null || !lastEvent.publicKey.contentEquals(event.publicKey) || lastCategory != category) {
                    lastEventList = mutableListOf(event)
                    groupedEvents[category]?.addAll(lastEventList)
                } else {
                    lastEventList.add(event)
                }
                lastEvent = event
                lastCategory = category
            }

            // Sort each group by descending date
            groupedEvents.forEach { (_, eventList) ->
                eventList.sortByDescending { it.date.time }
            }

            return groupedEvents.entries.map { it.key to it.value }
        }
    }
}
