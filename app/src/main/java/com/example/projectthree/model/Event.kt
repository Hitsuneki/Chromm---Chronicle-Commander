package com.example.projectthree.model

/**
 * Represents an event that occurs on a timeline turn.
 */
sealed class Event {
    abstract val name: String
    abstract val icon: String // Simple text icon for now
    
    /**
     * Enemy Attack - deals damage to HP unless blocked
     */
    data class EnemyAttack(
        val damage: Int,
        val strength: AttackStrength = AttackStrength.SMALL
    ) : Event() {
        override val name: String = when (strength) {
            AttackStrength.SMALL -> "Enemy Attack"
            AttackStrength.MEDIUM -> "Enemy Attack"
            AttackStrength.LARGE -> "Enemy Attack"
        }
        override val icon: String = "💀"
    }
    
    /**
     * Boss Raid - stronger Enemy Attack that hits harder but appears less often
     * Always telegraphed with a special icon
     */
    data class BossRaid(
        val damage: Int
    ) : Event() {
        override val name: String = "Boss Raid"
        override val icon: String = "👹"
    }
    
    /**
     * Supply Drop - grants Supplies when resolved
     */
    data class SupplyDrop(
        val baseAmount: Int = 5
    ) : Event() {
        override val name: String = "Supply Drop"
        override val icon: String = "📦"
    }
    
    /**
     * Fog / Ambush - hides event details until revealed
     */
    data class Fog(
        val hiddenEvent: Event? = null // The actual event hidden inside
    ) : Event() {
        override val name: String = "Fog / Ambush"
        override val icon: String = "☁️"
    }
    
    /**
     * Delay Field - shifts later events down the timeline
     */
    data class DelayField(
        val shiftAmount: Int = 1
    ) : Event() {
        override val name: String = "Delay Field"
        override val icon: String = "⏱️"
    }
    
    /**
     * Field Hospital - heals HP when resolved
     */
    data class FieldHospital(
        val healAmount: Int = 7
    ) : Event() {
        override val name: String = "Field Hospital"
        override val icon: String = "🏥"
    }
    
    enum class AttackStrength {
        SMALL, MEDIUM, LARGE
    }
}

