package com.example.projectthree.game

import com.example.projectthree.model.Event
import com.example.projectthree.model.GameConfig
import kotlin.random.Random

/**
 * Generates random events for the timeline.
 */
object EventGenerator {
    private val random = Random.Default
    
    /**
     * Generate a random event based on wave difficulty.
     */
    fun generateEvent(wave: Int): Event {
        val totalWeight = GameConfig.WEIGHT_ENEMY_ATTACK +
                GameConfig.WEIGHT_SUPPLY_DROP +
                GameConfig.WEIGHT_FOG +
                GameConfig.WEIGHT_DELAY_FIELD
        
        val roll = random.nextInt(totalWeight)
        
        return when {
            roll < GameConfig.WEIGHT_ENEMY_ATTACK -> {
                val strength = GameConfig.getAttackStrengthForWave(wave)
                val damage = when (strength) {
                    Event.AttackStrength.SMALL -> GameConfig.DAMAGE_SMALL_RAID
                    Event.AttackStrength.MEDIUM -> GameConfig.DAMAGE_MEDIUM_RAID
                    Event.AttackStrength.LARGE -> GameConfig.DAMAGE_LARGE_RAID
                }
                Event.EnemyAttack(damage, strength)
            }
            roll < GameConfig.WEIGHT_ENEMY_ATTACK + GameConfig.WEIGHT_SUPPLY_DROP -> {
                Event.SupplyDrop(GameConfig.SUPPLY_DROP_BASE)
            }
            roll < GameConfig.WEIGHT_ENEMY_ATTACK + GameConfig.WEIGHT_SUPPLY_DROP + GameConfig.WEIGHT_FOG -> {
                // Fog contains a random hidden event
                val hiddenEvent = generateEvent(wave)
                Event.Fog(hiddenEvent)
            }
            else -> {
                Event.DelayField(1)
            }
        }
    }
    
    /**
     * Generate multiple events for initial timeline.
     */
    fun generateTimelineEvents(wave: Int, count: Int): List<Event> {
        return (1..count).map { generateEvent(wave) }
    }
}

