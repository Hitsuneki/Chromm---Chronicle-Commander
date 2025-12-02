package com.example.projectthree.model

/**
 * Represents an order that can be placed on a timeline slot.
 */
sealed class Order {
    abstract val name: String
    abstract val icon: String
    abstract val suppliesCost: Int
    abstract val intelCost: Int
    
    /**
     * Defend - prevents or reduces damage from Enemy Attack
     * Also grants armor bonus for next turn
     */
    object Defend : Order() {
        override val name: String = "Defend"
        override val icon: String = "🛡️"
        override val suppliesCost: Int = 2
        override val intelCost: Int = 0
        const val damageReduction: Int = 100 // Percentage
        const val armorBonus: Int = 1 // Reduces next turn damage by this amount
    }
    
    /**
     * Harvest - increases Supplies gained from Supply Drop
     */
    object Harvest : Order() {
        override val name: String = "Harvest"
        override val icon: String = "🌾"
        override val suppliesCost: Int = 1
        override val intelCost: Int = 0
        const val bonusMultiplier: Float = 1.5f // 50% bonus
    }
    
    /**
     * Delay - pushes an event down by one turn
     */
    object Delay : Order() {
        override val name: String = "Delay"
        override val icon: String = "⏳"
        override val suppliesCost: Int = 0
        override val intelCost: Int = 2
        const val shiftAmount: Int = 1
    }
    
    /**
     * Scout - reveals Fog/Ambush events, costs Intel
     */
    object Scout : Order() {
        override val name: String = "Scout"
        override val icon: String = "🔍"
        override val suppliesCost: Int = 0
        override val intelCost: Int = 1
    }
}

