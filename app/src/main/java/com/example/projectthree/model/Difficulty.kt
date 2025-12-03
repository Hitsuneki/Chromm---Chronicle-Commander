package com.example.projectthree.model

/**
 * Difficulty levels that affect game parameters.
 */
enum class Difficulty {
    EASY,
    NORMAL,
    HARD
}

/**
 * Difficulty-specific configuration values.
 */
object DifficultyConfig {
    /**
     * Get starting HP based on difficulty.
     */
    fun getStartingHP(difficulty: Difficulty): Int {
        return when (difficulty) {
            Difficulty.EASY -> 60
            Difficulty.NORMAL -> 50
            Difficulty.HARD -> 40
        }
    }
    
    /**
     * Get starting Supplies based on difficulty.
     */
    fun getStartingSupplies(difficulty: Difficulty): Int {
        return when (difficulty) {
            Difficulty.EASY -> 8
            Difficulty.NORMAL -> 5
            Difficulty.HARD -> 3
        }
    }
    
    /**
     * Get starting Intel based on difficulty.
     */
    fun getStartingIntel(difficulty: Difficulty): Int {
        return when (difficulty) {
            Difficulty.EASY -> 4
            Difficulty.NORMAL -> 2
            Difficulty.HARD -> 1
        }
    }
    
    /**
     * Get max attacks per wave based on difficulty.
     */
    fun getMaxAttacksForWave(difficulty: Difficulty, wave: Int): Int {
        val baseMax = GameConfig.getMaxAttacksForWave(wave)
        return when (difficulty) {
            Difficulty.EASY -> (baseMax - 1).coerceAtLeast(1)  // Fewer attacks
            Difficulty.NORMAL -> baseMax
            Difficulty.HARD -> (baseMax + 1).coerceAtMost(5)    // More attacks (max 5)
        }
    }
    
    /**
     * Get event weights based on difficulty.
     */
    fun getEventWeights(difficulty: Difficulty): EventWeights {
        return when (difficulty) {
            Difficulty.EASY -> EventWeights(
                enemyAttack = 20,      // Fewer attacks
                supplyDrop = 40,       // More supplies
                fog = 12,
                delayField = 10,
                fieldHospital = 18      // More healing
            )
            Difficulty.NORMAL -> EventWeights(
                enemyAttack = 30,
                supplyDrop = 35,
                fog = 15,
                delayField = 12,
                fieldHospital = 8
            )
            Difficulty.HARD -> EventWeights(
                enemyAttack = 40,       // More attacks
                supplyDrop = 25,       // Fewer supplies
                fog = 18,
                delayField = 10,
                fieldHospital = 7      // Less healing
            )
        }
    }
    
    /**
     * Get damage multiplier based on difficulty.
     */
    fun getDamageMultiplier(difficulty: Difficulty): Float {
        return when (difficulty) {
            Difficulty.EASY -> 0.8f     // Less damage
            Difficulty.NORMAL -> 1.0f
            Difficulty.HARD -> 1.3f     // More damage
        }
    }
}

/**
 * Event generation weights for different difficulties.
 */
data class EventWeights(
    val enemyAttack: Int,
    val supplyDrop: Int,
    val fog: Int,
    val delayField: Int,
    val fieldHospital: Int
)

