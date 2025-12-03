package com.example.projectthree.game

import com.example.projectthree.model.*

/**
 * Resolves a single turn and applies all effects.
 */
data class TurnResult(
    val hpChange: Int = 0,
    val suppliesChange: Int = 0,
    val intelChange: Int = 0,
    val message: String = ""
)

object TurnResolver {
    /**
     * Resolve a single timeline slot.
     * Returns the result of the turn and any side effects.
     */
    fun resolveSlot(slot: TimelineSlot, gameState: GameState): TurnResult {
        var hpChange = 0
        var suppliesChange = 0
        var intelChange = 0
        val messages = mutableListOf<String>()
        
        val event = slot.getDisplayEvent()
        val order = slot.order
        
        // Apply order effects first (before event resolution)
        when (order) {
            is Order.Scout -> {
                if (slot.event is Event.Fog && !slot.isRevealed) {
                    slot.isRevealed = true
                    messages.add("Scout revealed: ${event.name}")
                }
            }
            is Order.Analyze -> {
                // Analyze gains Intel and reveals Fog if placed on it
                intelChange += Order.Analyze.intelGain
                messages.add("Analyze gained ${Order.Analyze.intelGain} Intel")
                if (slot.event is Event.Fog && !slot.isRevealed) {
                    slot.isRevealed = true
                    messages.add("Analyze also revealed the fog!")
                }
            }
            is Order.Forage -> {
                // Forage gains Supplies
                suppliesChange += Order.Forage.suppliesGain
                messages.add("Forage gained ${Order.Forage.suppliesGain} Supplies")
            }
            is Order.Medkit -> {
                // Medkit heals HP (no cap)
                hpChange += Order.Medkit.healAmount
                messages.add("Medkit healed ${Order.Medkit.healAmount} HP")
            }
            // Other order effects are applied during event resolution
            else -> {}
        }
        
        // Apply event effects
        when (event) {
            is Event.EnemyAttack -> {
                var damage = event.damage
                
                // Apply armor bonus from previous turn
                damage = (damage - slot.armorBonus).coerceAtLeast(0)
                if (slot.armorBonus > 0) {
                    messages.add("Armor reduced damage by ${slot.armorBonus}")
                }
                
                // Apply Defend order
                if (order is Order.Defend) {
                    damage = 0
                    messages.add("Defend blocked all damage!")
                    // Defend grants armor for next turn (handled separately)
                } else {
                    hpChange -= damage
                    messages.add("Took $damage damage")
                }
            }
            
            is Event.SupplyDrop -> {
                var amount = event.baseAmount
                if (order is Order.Harvest) {
                    amount = (amount * Order.Harvest.bonusMultiplier).toInt()
                    messages.add("Harvest increased supplies!")
                }
                // Forage stacks with Supply Drop
                if (order is Order.Forage) {
                    amount += Order.Forage.suppliesGain
                    messages.add("Forage bonus on Supply Drop!")
                }
                suppliesChange += amount
                messages.add("Gained $amount supplies")
            }
            
            is Event.FieldHospital -> {
                var heal = event.healAmount
                // Medkit on Field Hospital gives bonus
                if (order is Order.Medkit) {
                    heal += Order.Medkit.bonusOnHospital
                    messages.add("Medkit + Field Hospital bonus!")
                }
                hpChange += heal
                messages.add("Field Hospital healed $heal HP")
            }
            
            is Event.DelayField -> {
                messages.add("Delay Field activated")
                // Delay field shifting is handled at timeline level
            }
            
            is Event.Fog -> {
                if (!slot.isRevealed) {
                    // If fog is not revealed, treat it as a hidden attack
                    val hiddenEvent = event.hiddenEvent
                    if (hiddenEvent is Event.EnemyAttack) {
                        var damage = hiddenEvent.damage
                        if (order is Order.Defend) {
                            damage = 0
                            messages.add("Defend blocked hidden attack!")
                        } else {
                            hpChange -= damage
                            messages.add("Ambush! Took $damage damage")
                        }
                    }
                }
            }
        }
        
        return TurnResult(
            hpChange = hpChange,
            suppliesChange = suppliesChange,
            intelChange = intelChange,
            message = messages.joinToString(". ")
        )
    }
    
    /**
     * Apply side effects from orders (like Defend's armor bonus)
     */
    fun applyOrderSideEffects(slot: TimelineSlot, nextSlot: TimelineSlot?) {
        when (slot.order) {
            is Order.Defend -> {
                // Grant armor to next turn
                nextSlot?.let {
                    it.armorBonus += Order.Defend.armorBonus
                }
            }
            else -> {}
        }
    }
}

