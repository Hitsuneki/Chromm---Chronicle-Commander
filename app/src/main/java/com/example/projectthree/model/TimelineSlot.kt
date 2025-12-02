package com.example.projectthree.model

/**
 * Represents a single slot on the timeline.
 */
data class TimelineSlot(
    val turnNumber: Int,
    val event: Event,
    var order: Order? = null,
    var isRevealed: Boolean = false, // For Fog events
    var armorBonus: Int = 0 // From previous Defend orders
) {
    /**
     * Get the display event (if Fog is revealed, show hidden event)
     */
    fun getDisplayEvent(): Event {
        return if (event is Event.Fog && isRevealed && event.hiddenEvent != null) {
            event.hiddenEvent
        } else {
            event
        }
    }
    
    /**
     * Check if this slot can accept the given order
     */
    fun canAcceptOrder(order: Order): Boolean {
        // Each slot can only hold one order
        if (this.order != null) return false
        
        // Scout can only be placed on Fog events
        if (order is Order.Scout && event !is Event.Fog) return false
        
        return true
    }
}

