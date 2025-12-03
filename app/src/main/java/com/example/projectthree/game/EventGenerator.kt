package com.example.projectthree.game

import com.example.projectthree.model.*
import kotlin.random.Random

/**
 * Generates random events for the timeline with difficulty ramping.
 */
object EventGenerator {
    private val random = Random.Default
    
    /**
     * Generate a random event based on wave difficulty.
     * Excludes EnemyAttack (handled separately for difficulty ramping).
     */
    private fun generateNonAttackEvent(wave: Int, eventWeights: EventWeights): Event {
        val totalWeight = eventWeights.supplyDrop +
                eventWeights.fog +
                eventWeights.delayField +
                eventWeights.fieldHospital
        
        val roll = random.nextInt(totalWeight)
        
        return when {
            roll < eventWeights.supplyDrop -> {
                Event.SupplyDrop(GameConfig.SUPPLY_DROP_BASE)
            }
            roll < eventWeights.supplyDrop + eventWeights.fog -> {
                // Fog contains a random hidden event (but not another fog)
                val hiddenEvent = when (random.nextInt(3)) {
                    0 -> Event.EnemyAttack(
                        (GameConfig.DAMAGE_SMALL_RAID * DifficultyConfig.getDamageMultiplier(Difficulty.NORMAL)).toInt(),
                        Event.AttackStrength.SMALL
                    )
                    1 -> Event.SupplyDrop(GameConfig.SUPPLY_DROP_BASE)
                    else -> Event.FieldHospital(GameConfig.FIELD_HOSPITAL_HEAL)
                }
                Event.Fog(hiddenEvent)
            }
            roll < eventWeights.supplyDrop + eventWeights.fog + eventWeights.delayField -> {
                Event.DelayField(1)
            }
            else -> {
                Event.FieldHospital(GameConfig.FIELD_HOSPITAL_HEAL)
            }
        }
    }
    
    /**
     * Generate an enemy attack event.
     */
    private fun generateEnemyAttack(wave: Int, difficulty: Difficulty): Event.EnemyAttack {
        val strength = GameConfig.getAttackStrengthForWave(wave)
        val baseDamage = when (strength) {
            Event.AttackStrength.SMALL -> GameConfig.DAMAGE_SMALL_RAID
            Event.AttackStrength.MEDIUM -> GameConfig.DAMAGE_MEDIUM_RAID
            Event.AttackStrength.LARGE -> GameConfig.DAMAGE_LARGE_RAID
        }
        val damage = (baseDamage * DifficultyConfig.getDamageMultiplier(difficulty)).toInt()
        return Event.EnemyAttack(damage, strength)
    }
    
    /**
     * Generate a Boss Raid event (rare, telegraphed)
     */
    private fun generateBossRaid(wave: Int, difficulty: Difficulty): Event.BossRaid {
        val baseDamage = GameConfig.DAMAGE_BOSS_RAID + (wave * 2)  // Scales with wave
        val damage = (baseDamage * DifficultyConfig.getDamageMultiplier(difficulty)).toInt()
        return Event.BossRaid(damage)
    }
    
    /**
     * Generate multiple events for timeline with difficulty ramping.
     * Ensures max attacks per wave based on difficulty.
     */
    fun generateTimelineEvents(
        wave: Int,
        count: Int,
        difficulty: Difficulty,
        eventWeights: EventWeights
    ): List<Event> {
        val maxAttacks = DifficultyConfig.getMaxAttacksForWave(difficulty, wave)
        val events = mutableListOf<Event>()
        
        // Boss Raid appears rarely (10% chance per wave, but only if wave >= 3)
        val hasBossRaid = wave >= 3 && random.nextFloat() < 0.1f
        
        // First, determine how many regular attacks we'll have
        val numRegularAttacks = if (hasBossRaid) {
            random.nextInt(maxAttacks)  // One less slot for Boss Raid
        } else {
            random.nextInt(maxAttacks + 1)
        }
        
        // Generate regular attack events
        repeat(numRegularAttacks) {
            events.add(generateEnemyAttack(wave, difficulty))
        }
        
        // Add Boss Raid if applicable
        if (hasBossRaid) {
            events.add(generateBossRaid(wave, difficulty))
        }
        
        // Generate non-attack events to fill remaining slots
        repeat(count - events.size) {
            events.add(generateNonAttackEvent(wave, eventWeights))
        }
        
        // Shuffle to randomize positions
        return events.shuffled(random)
    }
}



