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
    
    /**
     * Analyze - gains Intel when resolved
     * Also reveals Fog if placed on it
     */
    object Analyze : Order() {
        override val name: String = "Analyze"
        override val icon: String = "🔬"
        override val suppliesCost: Int = 1
        override val intelCost: Int = 0
        const val intelGain: Int = 3
    }
    
    /**
     * Forage - gains Supplies when resolved
     * Stacks with Supply Drop for bonus
     */
    object Forage : Order() {
        override val name: String = "Forage"
        override val icon: String = "🍄"
        override val suppliesCost: Int = 1
        override val intelCost: Int = 0
        const val suppliesGain: Int = 4
    }
    
    /**
     * Medkit - heals HP when resolved
     * No HP cap, can exceed starting HP
     */
    object Medkit : Order() {
        override val name: String = "Medkit"
        override val icon: String = "💊"
        override val suppliesCost: Int = 2
        override val intelCost: Int = 0
        const val healAmount: Int = 7
        const val bonusOnHospital: Int = 3  // Extra heal when combined with Field Hospital
    }
    
    /**
     * Fortify - grants armor for the next 2 turns
     * Buff over time instead of one turn
     */
    object Fortify : Order() {
        override val name: String = "Fortify"
        override val icon: String = "🏰"
        override val suppliesCost: Int = 3
        override val intelCost: Int = 0
        const val armorBonus: Int = 2  // Grants +2 armor for next 2 turns
        const val duration: Int = 2    // Lasts 2 turns
    }
    
    /**
     * Defend Top - lane-specific defend for top lane
     */
    object DefendTop : Order() {
        override val name: String = "Defend Top"
        override val icon: String = "🛡️↑"
        override val suppliesCost: Int = 2
        override val intelCost: Int = 0
        const val damageReduction: Int = 100
        const val armorBonus: Int = 1
    }
    
    /**
     * Defend Bottom - lane-specific defend for bottom lane
     */
    object DefendBottom : Order() {
        override val name: String = "Defend Bottom"
        override val icon: String = "🛡️↓"
        override val suppliesCost: Int = 2
        override val intelCost: Int = 0
        const val damageReduction: Int = 100
        const val armorBonus: Int = 1
    }
    
    /**
     * Convoy - routes resources between lanes
     */
    object Convoy : Order() {
        override val name: String = "Convoy"
        override val icon: String = "🚚"
        override val suppliesCost: Int = 1
        override val intelCost: Int = 0
        const val transferAmount: Int = 2  // Transfers 2 supplies to other lane
    }
}

