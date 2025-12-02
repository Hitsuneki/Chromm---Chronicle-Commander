package com.example.projectthree.model

/**
 * Represents the current game state.
 */
data class GameState(
    var hp: Int = 100,
    var supplies: Int = 10,
    var intel: Int = 5,
    var wave: Int = 1,
    var maxOrdersPerRound: Int = 3,
    var ordersPlacedThisRound: Int = 0
) {
    fun isGameOver(): Boolean = hp <= 0
    
    fun canAffordOrder(order: Order): Boolean {
        return supplies >= order.suppliesCost && intel >= order.intelCost
    }
    
    fun spendOrderCosts(order: Order) {
        supplies -= order.suppliesCost
        intel -= order.intelCost
    }
    
    fun resetRound() {
        ordersPlacedThisRound = 0
    }
}

