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
                var intelGain = Order.Analyze.intelGain
                // Check for Analyze chain bonus (will be checked in combo bonuses)
                intelChange += intelGain
                messages.add("Analyze gained $intelGain Intel")
                if (slot.event is Event.Fog && !slot.isRevealed) {
                    slot.isRevealed = true
                    messages.add("Analyze also revealed the fog!")
                }
            }
            is Order.Forage -> {
                // Forage gains Supplies
                var suppliesGain = Order.Forage.suppliesGain
                // Check for Forage chain bonus (will be checked in combo bonuses)
                suppliesChange += suppliesGain
                messages.add("Forage gained $suppliesGain Supplies")
            }
            is Order.Medkit -> {
                // Medkit heals HP (no cap)
                var healAmount = Order.Medkit.healAmount
                // Check for Medkit chain bonus (will be checked in combo bonuses)
                hpChange += healAmount
                messages.add("Medkit healed $healAmount HP")
            }
            is Order.Fortify -> {
                // Fortify grants armor for next 2 turns (handled in side effects)
                messages.add("Fortify activated - armor for next 2 turns")
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
                
                // Apply Defend orders (generic or lane-specific)
                val isDefended = order is Order.Defend || 
                        (order is Order.DefendTop && slot.lane == Lane.TOP) ||
                        (order is Order.DefendBottom && slot.lane == Lane.BOTTOM)
                
                if (isDefended) {
                    damage = 0
                    messages.add("Defend blocked all damage!")
                    // Defend grants armor for next turn (handled separately)
                } else {
                    hpChange -= damage
                    messages.add("Took $damage damage")
                }
            }
            
            is Event.BossRaid -> {
                var damage = event.damage
                
                // Apply armor bonus from previous turn
                damage = (damage - slot.armorBonus).coerceAtLeast(0)
                if (slot.armorBonus > 0) {
                    messages.add("Armor reduced Boss damage by ${slot.armorBonus}")
                }
                
                // Apply Defend order (Boss Raid is stronger, Defend reduces but doesn't block completely)
                if (order is Order.Defend) {
                    damage = (damage * 0.5f).toInt()  // Defend reduces Boss damage by 50%
                    messages.add("Defend reduced Boss damage!")
                }
                
                hpChange -= damage
                messages.add("Boss Raid! Took $damage damage")
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
                // Convoy routes supplies to other lane (handled in GameEngine)
                if (order is Order.Convoy) {
                    messages.add("Convoy routing supplies to other lane")
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
                    when {
                        hiddenEvent is Event.EnemyAttack -> {
                            var damage = hiddenEvent.damage
                            // Apply armor
                            damage = (damage - slot.armorBonus).coerceAtLeast(0)
                            if (order is Order.Defend) {
                                damage = 0
                                messages.add("Defend blocked hidden attack!")
                            } else {
                                hpChange -= damage
                                messages.add("Ambush! Took $damage damage")
                            }
                        }
                        hiddenEvent is Event.BossRaid -> {
                            var damage = hiddenEvent.damage
                            // Apply armor
                            damage = (damage - slot.armorBonus).coerceAtLeast(0)
                            if (order is Order.Defend) {
                                damage = (damage * 0.5f).toInt()  // Defend reduces Boss damage by 50%
                                messages.add("Defend reduced hidden Boss damage!")
                            }
                            hpChange -= damage
                            messages.add("Boss Ambush! Took $damage damage")
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
     * Apply side effects from orders (like Defend's armor bonus, Fortify's multi-turn armor)
     */
    fun applyOrderSideEffects(slot: TimelineSlot, timeline: MutableList<TimelineSlot>, slotIndex: Int) {
        when (slot.order) {
            is Order.Defend -> {
                // Grant armor to next turn (same lane)
                if (slotIndex < timeline.size - 1) {
                    val nextSlot = timeline[slotIndex + 1]
                    if (nextSlot.lane == slot.lane) {
                        timeline[slotIndex + 1] = nextSlot.copy(
                            armorBonus = nextSlot.armorBonus + Order.Defend.armorBonus
                        )
                    }
                }
            }
            is Order.DefendTop -> {
                // Grant armor to next turn in top lane
                if (slot.lane == Lane.TOP && slotIndex < timeline.size - 1) {
                    val nextSlot = timeline[slotIndex + 1]
                    if (nextSlot.lane == Lane.TOP) {
                        timeline[slotIndex + 1] = nextSlot.copy(
                            armorBonus = nextSlot.armorBonus + Order.DefendTop.armorBonus
                        )
                    }
                }
            }
            is Order.DefendBottom -> {
                // Grant armor to next turn in bottom lane
                if (slot.lane == Lane.BOTTOM && slotIndex < timeline.size - 1) {
                    val nextSlot = timeline[slotIndex + 1]
                    if (nextSlot.lane == Lane.BOTTOM) {
                        timeline[slotIndex + 1] = nextSlot.copy(
                            armorBonus = nextSlot.armorBonus + Order.DefendBottom.armorBonus
                        )
                    }
                }
            }
            is Order.Fortify -> {
                // Grant armor to next 2 turns (same lane)
                for (i in 1..Order.Fortify.duration) {
                    if (slotIndex + i < timeline.size) {
                        val targetSlot = timeline[slotIndex + i]
                        if (targetSlot.lane == slot.lane) {
                            timeline[slotIndex + i] = targetSlot.copy(
                                armorBonus = targetSlot.armorBonus + Order.Fortify.armorBonus
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
    
    /**
     * Check for combo bonuses: same event type streaks and order chains
     */
    fun checkComboBonuses(
        timeline: MutableList<TimelineSlot>,
        currentIndex: Int,
        result: TurnResult
    ): TurnResult {
        var hpChange = result.hpChange
        var suppliesChange = result.suppliesChange
        var intelChange = result.intelChange
        val messages = mutableListOf<String>()
        
        // Check for event type streaks (2-3 of same type in a row)
        if (currentIndex >= 2) {
            val event1 = timeline[currentIndex - 2].getDisplayEvent()
            val event2 = timeline[currentIndex - 1].getDisplayEvent()
            val event3 = timeline[currentIndex].getDisplayEvent()
            
            // Check for 3 in a row
            if (isSameEventType(event1, event2) && isSameEventType(event2, event3)) {
                when {
                    event3 is Event.SupplyDrop -> {
                        // Supply Chain: third Supply Drop gives double
                        val bonus = event3.baseAmount
                        suppliesChange += bonus
                        messages.add("Supply Chain! +$bonus bonus Supplies")
                    }
                    event3 is Event.FieldHospital -> {
                        // Healing Chain: third Field Hospital gives bonus heal
                        val bonus = 5
                        hpChange += bonus
                        messages.add("Healing Chain! +$bonus bonus HP")
                    }
                }
            } else if (isSameEventType(event2, event3)) {
                // 2 in a row (smaller bonus)
                if (event3 is Event.SupplyDrop) {
                    val bonus = event3.baseAmount / 2
                    suppliesChange += bonus
                    messages.add("Supply Pair! +$bonus bonus Supplies")
                }
            }
        }
        
        // Check for order chains (same order on consecutive turns)
        if (currentIndex >= 1) {
            val prevOrder = timeline[currentIndex - 1].order
            val currentOrder = timeline[currentIndex].order
            
                if (prevOrder != null && currentOrder != null && prevOrder::class == currentOrder::class) {
                when (currentOrder) {
                    is Order.Defend -> {
                        // Defend chain: extra armor bonus for next turn
                        messages.add("Defend Chain! Extra protection")
                        // Extra armor is applied in side effects
                        if (currentIndex < timeline.size - 1) {
                            val nextSlot = timeline[currentIndex + 1]
                            timeline[currentIndex + 1] = nextSlot.copy(
                                armorBonus = nextSlot.armorBonus + 1
                            )
                        }
                    }
                    is Order.Analyze -> {
                        // Analyze chain: bonus Intel
                        val bonus = 2
                        intelChange += bonus
                        messages.add("Analyze Chain! +$bonus bonus Intel")
                    }
                    is Order.Forage -> {
                        // Forage chain: bonus Supplies
                        val bonus = 2
                        suppliesChange += bonus
                        messages.add("Forage Chain! +$bonus bonus Supplies")
                    }
                    is Order.Medkit -> {
                        // Medkit chain: bonus heal
                        val bonus = 3
                        hpChange += bonus
                        messages.add("Medkit Chain! +$bonus bonus HP")
                    }
                    else -> {}
                }
            }
        }
        
        return TurnResult(
            hpChange = hpChange,
            suppliesChange = suppliesChange,
            intelChange = intelChange,
            message = (result.message + " " + messages.joinToString(". ")).trim()
        )
    }
    
    /**
     * Check if two events are of the same type (for combo detection)
     */
    private fun isSameEventType(event1: Event, event2: Event): Boolean {
        return when {
            event1 is Event.SupplyDrop && event2 is Event.SupplyDrop -> true
            event1 is Event.FieldHospital && event2 is Event.FieldHospital -> true
            event1 is Event.EnemyAttack && event2 is Event.EnemyAttack -> true
            event1 is Event.BossRaid && event2 is Event.BossRaid -> true
            else -> false
        }
    }
}

