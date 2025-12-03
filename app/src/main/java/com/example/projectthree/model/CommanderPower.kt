package com.example.projectthree.model

/**
 * Commander Powers - RTS-style abilities with cooldowns
 */
sealed class CommanderPower {
    abstract val name: String
    abstract val icon: String
    abstract val cooldown: Int  // Cooldown in waves
    var currentCooldown: Int = 0
    
    /**
     * Tactical Pause - slows time during planning phase
     */
    object TacticalPause : CommanderPower() {
        override val name: String = "Tactical Pause"
        override val icon: String = "⏸️"
        override val cooldown: Int = 3  // 3 wave cooldown
    }
    
    /**
     * Airstrike - instantly cancels one Enemy Attack event
     */
    object Airstrike : CommanderPower() {
        override val name: String = "Airstrike"
        override val icon: String = "✈️"
        override val cooldown: Int = 3  // 3 wave cooldown
    }
    
    fun isReady(): Boolean = currentCooldown <= 0
    
    fun use() {
        currentCooldown = cooldown
    }
    
    fun reduceCooldown() {
        if (currentCooldown > 0) {
            currentCooldown--
        }
    }
}

