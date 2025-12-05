package com.example.projectthree.game

import com.example.projectthree.model.*

/**
 * Main game engine that manages the game state and timeline.
 */
class GameEngine(private val difficulty: Difficulty = Difficulty.NORMAL) {
    var gameState = GameState()
    var topLane: MutableList<TimelineSlot> = mutableListOf()
    var bottomLane: MutableList<TimelineSlot> = mutableListOf()
    private var currentTurnNumber = 1
    private val eventWeights = DifficultyConfig.getEventWeights(difficulty)
    var tacticalPauseActive: Boolean = false
    var airstrikeTarget: TimelineSlot? = null
    
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
        generateNewTimelines()
    }
    
    /**
     * Generate new timelines for both lanes.
     */
    fun generateNewTimelines() {
        val slotCount = GameConfig.getSlotsForWave(gameState.wave)
        
        // Generate events for top lane
        val topEvents = EventGenerator.generateTimelineEvents(
            wave = gameState.wave,
            count = slotCount,
            difficulty = difficulty,
            eventWeights = eventWeights
        )
        
        // Generate events for bottom lane
        val bottomEvents = EventGenerator.generateTimelineEvents(
            wave = gameState.wave,
            count = slotCount,
            difficulty = difficulty,
            eventWeights = eventWeights
        )
        
        topLane.clear()
        bottomLane.clear()
        
        topEvents.forEachIndexed { index, event ->
            topLane.add(
                TimelineSlot(
                    turnNumber = currentTurnNumber + index,
                    lane = Lane.TOP,
                    event = event,
                    isRevealed = event !is Event.Fog
                )
            )
        }
        
        bottomEvents.forEachIndexed { index, event ->
            bottomLane.add(
                TimelineSlot(
                    turnNumber = currentTurnNumber + index,
                    lane = Lane.BOTTOM,
                    event = event,
                    isRevealed = event !is Event.Fog
                )
            )
        }
    }
    
    /**
     * Get all timeline slots (both lanes combined, for UI display)
     */
    fun getAllSlots(): List<TimelineSlot> {
        val allSlots = mutableListOf<TimelineSlot>()
        // Interleave top and bottom lanes for display
        for (i in 0 until maxOf(topLane.size, bottomLane.size)) {
            if (i < topLane.size) allSlots.add(topLane[i])
            if (i < bottomLane.size) allSlots.add(bottomLane[i])
        }
        return allSlots
    }
    
    /**
     * Get slot by lane and index
     */
    fun getSlot(lane: Lane, index: Int): TimelineSlot? {
        return when (lane) {
            Lane.TOP -> if (index < topLane.size) topLane[index] else null
            Lane.BOTTOM -> if (index < bottomLane.size) bottomLane[index] else null
        }
    }
    
    /**
     * Try to place an order on a timeline slot.
     * Returns true if successful.
     */
    fun placeOrder(lane: Lane, slotIndex: Int, order: Order): Boolean {
        val slot = getSlot(lane, slotIndex) ?: return false
        // No limit on orders per round
        if (!gameState.canAffordOrder(order)) return false
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
    fun removeOrder(lane: Lane, slotIndex: Int): Order? {
        val slot = getSlot(lane, slotIndex) ?: return null
        val order = slot.order ?: return null
        
        // Refund costs
        gameState.supplies += order.suppliesCost
        gameState.intel += order.intelCost
        gameState.ordersPlacedThisRound--
        
        slot.order = null
        return order
    }
    
    /**
     * Use Airstrike commander power to cancel an enemy attack
     */
    fun useAirstrike(lane: Lane, slotIndex: Int): Boolean {
        val slot = getSlot(lane, slotIndex) ?: return false
        val event = slot.getDisplayEvent()
        
        if (event is Event.EnemyAttack || event is Event.BossRaid) {
            // Replace with a neutral event (Supply Drop)
            val newSlot = slot.copy(
                event = Event.SupplyDrop(GameConfig.SUPPLY_DROP_BASE),
                isRevealed = true
            )
            when (lane) {
                Lane.TOP -> topLane[slotIndex] = newSlot
                Lane.BOTTOM -> bottomLane[slotIndex] = newSlot
            }
            airstrikeTarget = newSlot
            return true
        }
        return false
    }
    
    /**
     * Resolve all turns in both lanes.
     * Returns a list of results for each turn.
     */
    fun resolveAllTurns(): List<TurnResult> {
        val results = mutableListOf<TurnResult>()
        
        // Apply delay effects to both lanes
        applyDelayEffects(topLane)
        applyDelayEffects(bottomLane)
        
        // Resolve both lanes simultaneously (process turn by turn)
        val maxSlots = maxOf(topLane.size, bottomLane.size)
        
        for (turnIndex in 0 until maxSlots) {
            // Resolve top lane slot
            if (turnIndex < topLane.size) {
                val topSlot = topLane[turnIndex]
                var topResult = TurnResolver.resolveSlot(topSlot, gameState)
                
                // Handle Convoy order (transfer supplies between lanes)
                if (topSlot.order is Order.Convoy && topSlot.event is Event.SupplyDrop) {
                    val transferAmount = Order.Convoy.transferAmount
                    gameState.bottomLaneSupplies += transferAmount
                    topResult = topResult.copy(
                        message = topResult.message + ". Convoy transferred $transferAmount supplies to bottom lane"
                    )
                }
                
                // Apply side effects
                TurnResolver.applyOrderSideEffects(topSlot, topLane, turnIndex)
                
                // Check combo bonuses
                topResult = TurnResolver.checkComboBonuses(topLane, turnIndex, topResult)
                
                // Update game state
                gameState.hp += topResult.hpChange
                gameState.supplies += topResult.suppliesChange
                gameState.intel += topResult.intelChange
                
                results.add(topResult.copy(message = "[TOP] ${topResult.message}"))
            }
            
            // Resolve bottom lane slot
            if (turnIndex < bottomLane.size) {
                val bottomSlot = bottomLane[turnIndex]
                var bottomResult = TurnResolver.resolveSlot(bottomSlot, gameState)
                
                // Handle Convoy order
                if (bottomSlot.order is Order.Convoy && bottomSlot.event is Event.SupplyDrop) {
                    val transferAmount = Order.Convoy.transferAmount
                    gameState.topLaneSupplies += transferAmount
                    bottomResult = bottomResult.copy(
                        message = bottomResult.message + ". Convoy transferred $transferAmount supplies to top lane"
                    )
                }
                
                // Apply side effects
                TurnResolver.applyOrderSideEffects(bottomSlot, bottomLane, turnIndex)
                
                // Check combo bonuses
                bottomResult = TurnResolver.checkComboBonuses(bottomLane, turnIndex, bottomResult)
                
                // Update game state
                gameState.hp += bottomResult.hpChange
                gameState.supplies += bottomResult.suppliesChange
                gameState.intel += bottomResult.intelChange
                
                results.add(bottomResult.copy(message = "[BOTTOM] ${bottomResult.message}"))
            }
        }
        
        // Apply routed supplies from lanes
        gameState.supplies += gameState.topLaneSupplies + gameState.bottomLaneSupplies
        gameState.topLaneSupplies = 0
        gameState.bottomLaneSupplies = 0
        
        // Advance timeline (continuous wave pressure - slide in new events)
        advanceTimelineContinuous()
        
        // Reset round
        gameState.resetRound()
        
        return results
    }
    
    /**
     * Advance timeline continuously - remove all resolved slots, add new ones
     * This creates a short planning window before the next resolution phase
     */
    private fun advanceTimelineContinuous() {
        // Remove all resolved slots (all 7 slots were just resolved)
        val slotsResolved = topLane.size
        currentTurnNumber += slotsResolved
        
        topLane.clear()
        bottomLane.clear()
        
        // Generate new events for both lanes (7 slots each)
        val slotCount = GameConfig.getSlotsForWave(gameState.wave)
        val topEvents = EventGenerator.generateTimelineEvents(
            wave = gameState.wave,
            count = slotCount,
            difficulty = difficulty,
            eventWeights = eventWeights
        )
        
        val bottomEvents = EventGenerator.generateTimelineEvents(
            wave = gameState.wave,
            count = slotCount,
            difficulty = difficulty,
            eventWeights = eventWeights
        )
        
        topEvents.forEachIndexed { index, event ->
            topLane.add(
                TimelineSlot(
                    turnNumber = currentTurnNumber + index,
                    lane = Lane.TOP,
                    event = event,
                    isRevealed = event !is Event.Fog
                )
            )
        }
        
        bottomEvents.forEachIndexed { index, event ->
            bottomLane.add(
                TimelineSlot(
                    turnNumber = currentTurnNumber + index,
                    lane = Lane.BOTTOM,
                    event = event,
                    isRevealed = event !is Event.Fog
                )
            )
        }
        
        // Increment wave after each full resolution cycle
        gameState.wave++
    }
    
    /**
     * Check if game is over.
     */
    fun isGameOver(): Boolean = gameState.isGameOver()
    
    /**
     * Apply delay effects to a lane - shift events forward where Delay orders are placed
     */
    private fun applyDelayEffects(lane: MutableList<TimelineSlot>) {
        // Find all Delay orders and their positions
        val delayPositions = mutableListOf<Int>()
        for (i in lane.indices) {
            if (lane[i].order is Order.Delay) {
                delayPositions.add(i)
            }
        }

        // Apply delays from right to left to avoid index shifting issues
        delayPositions.sortedDescending().forEach { pos ->
            if (pos + 1 < lane.size) {
                // Swap the event with the next slot
                val currentEvent = lane[pos].event
                val nextEvent = lane[pos + 1].event

                lane[pos] = lane[pos].copy(event = nextEvent)
                lane[pos + 1] = lane[pos + 1].copy(event = currentEvent)

                // Update reveal status
                lane[pos].isRevealed = lane[pos].event !is Event.Fog
                lane[pos + 1].isRevealed = lane[pos + 1].event !is Event.Fog
            }
        }
    }

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
            Order.Fortify,
            Order.Convoy
        )
    }
}

