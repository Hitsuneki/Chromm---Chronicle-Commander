package com.example.projectthree.model

/**
 * Difficulty levels that affect game parameters.
 */
enum class Difficulty {
    EASY,
    NORMAL,
    HARD,
    RTS
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
            Difficulty.RTS -> 50
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
            Difficulty.RTS -> 5
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
            Difficulty.RTS -> 2
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
            Difficulty.RTS -> baseMax
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
            Difficulty.RTS -> EventWeights(
                enemyAttack = 30,
                supplyDrop = 35,
                fog = 15,
                delayField = 12,
                fieldHospital = 8
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
            Difficulty.RTS -> 1.0f
        }
    }
    
    /**
     * Check if difficulty uses RTS mode (dual lane layout)
     */
    fun isRTSMode(difficulty: Difficulty): Boolean {
        return difficulty == Difficulty.RTS
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

