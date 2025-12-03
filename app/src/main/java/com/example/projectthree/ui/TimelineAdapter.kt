package com.example.projectthree.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.projectthree.R
import com.example.projectthree.model.Event
import com.example.projectthree.model.Order
import com.example.projectthree.model.TimelineSlot

class TimelineAdapter(
    private val slots: MutableList<TimelineSlot>,
    private val onSlotClick: (Int) -> Unit
) : RecyclerView.Adapter<TimelineAdapter.SlotViewHolder>() {
    
    class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val turnNumber: TextView = itemView.findViewById(R.id.turnNumber)
        val eventIcon: TextView = itemView.findViewById(R.id.eventIcon)
        val eventName: TextView = itemView.findViewById(R.id.eventName)
        val eventSubtitle: TextView = itemView.findViewById(R.id.eventSubtitle)
        val orderIcon: TextView = itemView.findViewById(R.id.orderIcon)
        val slotContainer: View = itemView.findViewById(R.id.slotContainer)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_timeline_slot, parent, false)
        return SlotViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]
        val event = slot.getDisplayEvent()
        
        // Set turn number (just the number)
        holder.turnNumber.text = slot.turnNumber.toString()
        
        // Handle fog display
        if (slot.event is Event.Fog && !slot.isRevealed) {
            // Show fog (unrevealed)
            holder.eventIcon.text = "☁️"
            holder.eventName.text = "Fog / Ambush"
            holder.eventSubtitle.text = "Hidden event. Use Scout to reveal."
        } else {
            // Show the actual event (or revealed hidden event)
            holder.eventIcon.text = event.icon
            holder.eventName.text = event.name
            
            // Set subtitle based on event type
            val subtitle = when (event) {
                is Event.EnemyAttack -> {
                    val strengthText = when (event.strength) {
                        Event.AttackStrength.SMALL -> "Small raid"
                        Event.AttackStrength.MEDIUM -> "Raid"
                        Event.AttackStrength.LARGE -> "Big raid"
                    }
                    "$strengthText. Deals ${event.damage} damage."
                }
                is Event.BossRaid -> "Boss Raid! Deals ${event.damage} damage."
                is Event.SupplyDrop -> "Receive ${event.baseAmount} supplies."
                is Event.FieldHospital -> "Restore ${event.healAmount} HP."
                is Event.DelayField -> "Shifts later events down by ${event.shiftAmount} turn(s)."
                is Event.Fog -> {
                    // This shouldn't happen if revealed, but handle it
                    "Hidden event."
                }
            }
            holder.eventSubtitle.text = subtitle
        }
        
        // Show order if present
        if (slot.order != null) {
            holder.orderIcon.text = slot.order!!.icon
            holder.orderIcon.visibility = View.VISIBLE
        } else {
            holder.orderIcon.visibility = View.GONE
        }
        
        // Color coding for event types (use actual event, not display event for fog)
        val backgroundColor = when (slot.event) {
            is Event.EnemyAttack -> ContextCompat.getColor(holder.itemView.context, R.color.event_attack)
            is Event.BossRaid -> ContextCompat.getColor(holder.itemView.context, R.color.event_attack) // Boss uses attack color
            is Event.SupplyDrop -> ContextCompat.getColor(holder.itemView.context, R.color.event_supply)
            is Event.FieldHospital -> ContextCompat.getColor(holder.itemView.context, R.color.event_heal) // Use heal color for hospital
            is Event.Fog -> ContextCompat.getColor(holder.itemView.context, R.color.event_fog)
            is Event.DelayField -> ContextCompat.getColor(holder.itemView.context, R.color.event_delay)
        }
        holder.slotContainer.setBackgroundColor(backgroundColor)
        
        holder.itemView.setOnClickListener {
            onSlotClick(position)
        }
    }
    
    override fun getItemCount(): Int = slots.size
    
    fun updateSlots(newSlots: List<TimelineSlot>) {
        slots.clear()
        slots.addAll(newSlots)
        notifyDataSetChanged()
    }
}

