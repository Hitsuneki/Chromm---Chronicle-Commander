package com.example.projectthree.game

import com.example.projectthree.model.*

/**
 * Main game engine that manages the game state and timeline.
 */
class GameEngine(private val difficulty: Difficulty = Difficulty.NORMAL) {
    var gameState = GameState()
    var timeline: MutableList<TimelineSlot> = mutableListOf()
    private var currentTurnNumber = 1
    private val eventWeights = DifficultyConfig.getEventWeights(difficulty)
    
    init {
        initializeGame()
    }
    
    private fun initializeGame() {
        gameState = GameState(
            hp = DifficultyConfig.getStartingHP(difficulty),
            supplies = DifficultyConfig.getStartingSupplies(difficulty),
            intel = DifficultyConfig.getStartingIntel(difficulty),
            wave = 1,
            maxOrdersPerRound = GameConfig.INITIAL_ORDERS_PER_ROUND
        )
        generateNewTimeline()
    }
    
    /**
     * Generate a new timeline with events.
     */
    fun generateNewTimeline() {
        val slotCount = GameConfig.getSlotsForWave(gameState.wave)
        val events = EventGenerator.generateTimelineEvents(
            wave = gameState.wave,
            count = slotCount,
            difficulty = difficulty,
            eventWeights = eventWeights
        )
        
        timeline.clear()
        events.forEachIndexed { index, event ->
            timeline.add(
                TimelineSlot(
                    turnNumber = currentTurnNumber + index,
                    event = event,
                    isRevealed = event !is Event.Fog
                )
            )
        }
    }
    
    /**
     * Try to place an order on a timeline slot.
     * Returns true if successful.
     */
    fun placeOrder(slotIndex: Int, order: Order): Boolean {
        if (slotIndex < 0 || slotIndex >= timeline.size) return false
        // No limit on orders per round
        if (!gameState.canAffordOrder(order)) return false
        
        val slot = timeline[slotIndex]
        if (!slot.canAcceptOrder(order)) return false
        
        // Place the order
        slot.order = order
        gameState.spendOrderCosts(order)
        gameState.ordersPlacedThisRound++
        
        // Scout and Analyze reveal fog immediately
        if ((order is Order.Scout || order is Order.Analyze) && slot.event is Event.Fog) {
            slot.isRevealed = true
        }
        
        return true
    }
    
    /**
     * Remove an order from a slot (for drag-and-drop).
     */
    fun removeOrder(slotIndex: Int): Order? {
        if (slotIndex < 0 || slotIndex >= timeline.size) return null
        val slot = timeline[slotIndex]
        val order = slot.order ?: return null
        
        // Refund costs
        gameState.supplies += order.suppliesCost
        gameState.intel += order.intelCost
        gameState.ordersPlacedThisRound--
        
        slot.order = null
        return order
    }
    
    /**
     * Resolve all turns in the timeline.
     * Returns a list of results for each turn.
     */
    fun resolveAllTurns(): List<TurnResult> {
        val results = mutableListOf<TurnResult>()
        val slotsToSkip = mutableSetOf<Int>() // Slots delayed (skip during resolution)
        
        // First pass: identify delayed slots and apply delay field effects
        applyDelayEffectsBeforeResolution(slotsToSkip)
        
        // Process each slot in order (skip delayed ones)
        timeline.forEachIndexed { index, slot ->
            if (slotsToSkip.contains(index)) {
                // This slot was delayed, skip it
                results.add(TurnResult(message = "${slot.getDisplayEvent().name} was delayed"))
                return@forEachIndexed
            }
            
            var result = TurnResolver.resolveSlot(slot, gameState)
            
            // Apply side effects (armor bonuses, etc.)
            TurnResolver.applyOrderSideEffects(slot, timeline, index)
            
            // Check for combo bonuses
            result = TurnResolver.checkComboBonuses(timeline, index, result)
            
            // Update game state
            gameState.hp += result.hpChange
            gameState.supplies += result.suppliesChange
            gameState.intel += result.intelChange
            
            results.add(result)
        }
        
        // Advance timeline (delayed events will appear in next round)
        advanceTimeline()
        
        // Reset round
        gameState.resetRound()
        
        return results
    }
    
    /**
     * Apply delay effects before resolution.
     * Delay orders: skip the slot during resolution (it will appear in next round)
     * Delay Field events: shift later events down by swapping positions
     */
    private fun applyDelayEffectsBeforeResolution(slotsToSkip: MutableSet<Int>) {
        // First, handle Delay Field events (shift later events down)
        // Process from bottom to top to avoid index issues
        for (index in timeline.size - 1 downTo 0) {
            val slot = timeline[index]
            val event = slot.getDisplayEvent()
            if (event is Event.DelayField) {
                // Shift the next event(s) down by one position
                val shiftAmount = event.shiftAmount
                for (shift in 1..shiftAmount) {
                    val sourceIndex = index + shift
                    val targetIndex = index + shift + 1
                    if (sourceIndex < timeline.size && targetIndex < timeline.size) {
                        // Swap slots to shift down - need to create new instances since event is val
                        val sourceSlot = timeline[sourceIndex]
                        val targetSlot = timeline[targetIndex]
                        // Swap: source gets target's event/order, target gets source's event/order
                        timeline[sourceIndex] = sourceSlot.copy(
                            event = targetSlot.event,
                            order = targetSlot.order,
                            isRevealed = targetSlot.isRevealed,
                            armorBonus = targetSlot.armorBonus
                        )
                        timeline[targetIndex] = targetSlot.copy(
                            event = sourceSlot.event,
                            order = sourceSlot.order,
                            isRevealed = sourceSlot.isRevealed,
                            armorBonus = sourceSlot.armorBonus
                        )
                    }
                }
            }
        }
        
        // Then, handle Delay orders (mark slot to skip during resolution)
        timeline.forEachIndexed { index, slot ->
            if (slot.order is Order.Delay) {
                slotsToSkip.add(index)
            }
        }
    }
    
    /**
     * Advance the timeline: remove resolved slots and add new ones.
     * Delayed events will be preserved and added to the new timeline.
     */
    private fun advanceTimeline() {
        val slotsToRemove = timeline.size
        currentTurnNumber += slotsToRemove
        
        // Remove all resolved slots
        // (Delayed events are lost - they would need to be preserved separately
        //  but for simplicity, we'll just generate a new timeline)
        timeline.clear()
        
        // Generate new timeline
        generateNewTimeline()
        
        // Increment wave
        gameState.wave++
    }
    
    /**
     * Check if game is over.
     */
    fun isGameOver(): Boolean = gameState.isGameOver()
    
    /**
     * Get available orders for the current round.
     */
    fun getAvailableOrders(): List<Order> {
        return listOf(
            Order.Defend,
            Order.Harvest,
            Order.Delay,
            Order.Scout,
            Order.Analyze,
            Order.Forage,
            Order.Medkit,
            Order.Fortify
        )
    }
}

