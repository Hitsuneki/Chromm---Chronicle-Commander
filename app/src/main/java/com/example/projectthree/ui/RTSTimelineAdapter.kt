package com.example.projectthree.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.projectthree.R
import com.example.projectthree.model.Event
import com.example.projectthree.model.Lane
import com.example.projectthree.model.TimelineSlot

class RTSTimelineAdapter(
    private val topLane: MutableList<TimelineSlot>,
    private val bottomLane: MutableList<TimelineSlot>,
    private val onTopSlotClick: (Int) -> Unit,
    private val onBottomSlotClick: (Int) -> Unit
) : RecyclerView.Adapter<RTSTimelineAdapter.RowViewHolder>() {
    
    class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val topTurnNumber: TextView = itemView.findViewById(R.id.topTurnNumber)
        val topEventIcon: TextView = itemView.findViewById(R.id.topEventIcon)
        val topEventLabel: TextView = itemView.findViewById(R.id.topEventLabel)
        val topEventCard: View = itemView.findViewById(R.id.topEventCard)
        val topTurnNumberCard: View = itemView.findViewById(R.id.topTurnNumberCard)
        
        val bottomTurnNumber: TextView = itemView.findViewById(R.id.bottomTurnNumber)
        val bottomEventIcon: TextView = itemView.findViewById(R.id.bottomEventIcon)
        val bottomEventLabel: TextView = itemView.findViewById(R.id.bottomEventLabel)
        val bottomEventCard: View = itemView.findViewById(R.id.bottomEventCard)
        val bottomTurnNumberCard: View = itemView.findViewById(R.id.bottomTurnNumberCard)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rts_timeline_row, parent, false)
        return RowViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        // Top Lane
        if (position < topLane.size) {
            val topSlot = topLane[position]
            val topEvent = topSlot.getDisplayEvent()
            
            holder.topTurnNumber.text = topSlot.turnNumber.toString()
            holder.topEventIcon.text = topEvent.icon
            
            // Set label and color for top event
            val (topLabel, topColor) = getEventLabelAndColor(topSlot, topEvent, holder.itemView.context)
            holder.topEventLabel.text = topLabel
            holder.topEventCard.setBackgroundColor(topColor)
            
            holder.topEventCard.setOnClickListener {
                onTopSlotClick(position)
            }
            holder.topTurnNumberCard.setOnClickListener {
                onTopSlotClick(position)
            }
        } else {
            holder.topTurnNumber.text = ""
            holder.topEventIcon.text = ""
            holder.topEventLabel.text = ""
            holder.topEventCard.setOnClickListener(null)
            holder.topTurnNumberCard.setOnClickListener(null)
        }
        
        // Bottom Lane
        if (position < bottomLane.size) {
            val bottomSlot = bottomLane[position]
            val bottomEvent = bottomSlot.getDisplayEvent()
            
            holder.bottomTurnNumber.text = bottomSlot.turnNumber.toString()
            holder.bottomEventIcon.text = bottomEvent.icon
            
            // Set label and color for bottom event
            val (bottomLabel, bottomColor) = getEventLabelAndColor(bottomSlot, bottomEvent, holder.itemView.context)
            holder.bottomEventLabel.text = bottomLabel
            holder.bottomEventCard.setBackgroundColor(bottomColor)
            
            holder.bottomEventCard.setOnClickListener {
                onBottomSlotClick(position)
            }
            holder.bottomTurnNumberCard.setOnClickListener {
                onBottomSlotClick(position)
            }
        } else {
            holder.bottomTurnNumber.text = ""
            holder.bottomEventIcon.text = ""
            holder.bottomEventLabel.text = ""
            holder.bottomEventCard.setOnClickListener(null)
            holder.bottomTurnNumberCard.setOnClickListener(null)
        }
    }
    
    private fun getEventLabelAndColor(slot: TimelineSlot, event: Event, context: android.content.Context): Pair<String, Int> {
        return when (event) {
            is Event.EnemyAttack -> {
                val label = "Attack"
                val color = ContextCompat.getColor(context, R.color.event_attack)
                Pair(label, color)
            }
            is Event.BossRaid -> {
                val label = "Boss"
                val color = ContextCompat.getColor(context, R.color.event_attack)
                Pair(label, color)
            }
            is Event.SupplyDrop -> {
                val label = "Supplies"
                val color = ContextCompat.getColor(context, R.color.event_supply)
                Pair(label, color)
            }
            is Event.FieldHospital -> {
                val label = "Heal +${event.healAmount}"
                val color = ContextCompat.getColor(context, R.color.event_heal)
                Pair(label, color)
            }
            is Event.Fog -> {
                val label = if (slot.isRevealed) {
                    // Show hidden event label if revealed
                    when (val hidden = event.hiddenEvent) {
                        is Event.EnemyAttack -> "Attack"
                        is Event.BossRaid -> "Boss"
                        is Event.SupplyDrop -> "Supplies"
                        is Event.FieldHospital -> "Heal +${hidden.healAmount}"
                        else -> "Fog"
                    }
                } else {
                    "Fog"
                }
                val color = ContextCompat.getColor(context, R.color.event_fog)
                Pair(label, color)
            }
            is Event.DelayField -> {
                val label = "Delay"
                val color = ContextCompat.getColor(context, R.color.event_delay)
                Pair(label, color)
            }
        }
    }
    
    override fun getItemCount(): Int = maxOf(topLane.size, bottomLane.size)
    
    fun updateSlots(newTopLane: List<TimelineSlot>, newBottomLane: List<TimelineSlot>) {
        topLane.clear()
        topLane.addAll(newTopLane)
        bottomLane.clear()
        bottomLane.addAll(newBottomLane)
        notifyDataSetChanged()
    }
}

