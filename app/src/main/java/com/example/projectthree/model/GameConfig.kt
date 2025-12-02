package com.example.projectthree.model

/**
 * Configuration values for game tuning.
 */
object GameConfig {
    // Timeline settings
    const val INITIAL_SLOTS = 6
    const val MAX_SLOTS = 8
    
    // Starting resources
    const val STARTING_HP = 20
    const val STARTING_SUPPLIES = 5
    const val STARTING_INTEL = 2
    
    // Orders per round
    const val INITIAL_ORDERS_PER_ROUND = 3
    
    // Event generation weights (higher = more common)
    const val WEIGHT_ENEMY_ATTACK = 40
    const val WEIGHT_SUPPLY_DROP = 30
    const val WEIGHT_FOG = 20
    const val WEIGHT_DELAY_FIELD = 10
    
    // Damage values
    const val DAMAGE_SMALL_RAID = 5
    const val DAMAGE_MEDIUM_RAID = 15
    const val DAMAGE_LARGE_RAID = 30
    
    // Supply drop amounts
    const val SUPPLY_DROP_BASE = 3
    
    // Difficulty scaling
    fun getSlotsForWave(wave: Int): Int {
        return when {
            wave <= 2 -> 4
            wave <= 5 -> 5
            wave <= 10 -> 6
            else -> 7
        }.coerceAtMost(MAX_SLOTS)
    }
    
    fun getAttackStrengthForWave(wave: Int): Event.AttackStrength {
        return when {
            wave <= 3 -> Event.AttackStrength.SMALL
            wave <= 7 -> Event.AttackStrength.MEDIUM
            else -> Event.AttackStrength.LARGE
        }
    }
}

