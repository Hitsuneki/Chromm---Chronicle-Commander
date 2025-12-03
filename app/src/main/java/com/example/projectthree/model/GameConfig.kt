package com.example.projectthree.model

/**
 * Configuration values for game tuning.
 */
object GameConfig {
    // Timeline settings - always 7 slots per wave
    const val SLOTS_PER_WAVE = 7
    
    // Planning timer settings
    const val PLANNING_TIME_SECONDS = 15L
    const val TACTICAL_PAUSE_EXTENSION_SECONDS = 10L
    
    // Starting resources
    const val STARTING_HP = 50
    const val STARTING_SUPPLIES = 5
    const val STARTING_INTEL = 2
    
    // Orders per round
    const val INITIAL_ORDERS_PER_ROUND = 3
    
    // Event generation weights (target percentages: Enemy 25-35%, Supply 30-40%, Fog 10-20%, Delay 10-15%, Field Hospital ~15%)
    const val WEIGHT_ENEMY_ATTACK = 30  // 25-35% range
    const val WEIGHT_SUPPLY_DROP = 35   // 30-40% range
    const val WEIGHT_FOG = 15            // 10-20% range
    const val WEIGHT_DELAY_FIELD = 12    // 10-15% range
    const val WEIGHT_FIELD_HOSPITAL = 8  // ~15% (similar rarity to Supply Drop)
    
    // Damage values
    const val DAMAGE_SMALL_RAID = 5
    const val DAMAGE_MEDIUM_RAID = 15
    const val DAMAGE_LARGE_RAID = 30
    const val DAMAGE_BOSS_RAID = 40  // Boss Raid base damage
    
    // Supply drop amounts
    const val SUPPLY_DROP_BASE = 3
    
    // Healing amounts
    const val FIELD_HOSPITAL_HEAL = 7
    const val MEDKIT_HEAL = 7
    const val MEDKIT_BONUS_ON_HOSPITAL = 3  // Extra heal when Medkit + Field Hospital
    
    // Resource gain amounts
    const val ANALYZE_INTEL_GAIN = 3
    const val FORAGE_SUPPLIES_GAIN = 4
    
    // Difficulty scaling - always 7 slots
    fun getSlotsForWave(wave: Int): Int = SLOTS_PER_WAVE
    
    // Max enemy attacks per wave (difficulty ramping)
    fun getMaxAttacksForWave(wave: Int): Int {
        return when {
            wave <= 3 -> 2  // Early waves: max 2 attacks
            wave <= 7 -> 3  // Mid waves: max 3 attacks
            else -> 4       // Later waves: max 4 attacks (never all 7)
        }
    }
    
    fun getAttackStrengthForWave(wave: Int): Event.AttackStrength {
        return when {
            wave <= 3 -> Event.AttackStrength.SMALL
            wave <= 7 -> Event.AttackStrength.MEDIUM
            else -> Event.AttackStrength.LARGE
        }
    }
}

