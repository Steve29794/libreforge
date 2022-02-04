package com.willfp.libreforge.effects

import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableList
import com.willfp.eco.core.config.interfaces.Config
import com.willfp.libreforge.ConfigViolation
import com.willfp.libreforge.LibReforgePlugin
import com.willfp.libreforge.conditions.Conditions
import com.willfp.libreforge.effects.effects.*
import com.willfp.libreforge.filters.ConfiguredFilter
import com.willfp.libreforge.filters.EmptyFilter
import com.willfp.libreforge.triggers.Trigger
import com.willfp.libreforge.triggers.Triggers
import java.util.*

@Suppress("UNUSED")
object Effects {
    private val BY_ID = HashBiMap.create<String, Effect>()

    val DAMAGE_MULTIPLIER: Effect = EffectDamageMultiplier()
    val CRIT_MULTIPLIER: Effect = EffectCritMultiplier()
    val KNOCKBACK_MULTIPLIER: Effect = EffectKnockbackMultiplier()
    val GIVE_MONEY: Effect = EffectGiveMoney()
    val ATTACK_SPEED_MULTIPLIER: Effect = EffectAttackSpeedMultiplier()
    val MOVEMENT_SPEED_MULTIPLIER: Effect = EffectMovementSpeedMultiplier()
    val BONUS_HEALTH: Effect = EffectBonusHealth()
    val RUN_COMMAND: Effect = EffectRunCommand()
    val STRIKE_LIGHTNING: Effect = EffectStrikeLightning()
    val SPAWN_MOBS: Effect = EffectSpawnMobs()
    val HUNGER_MULTIPLIER: Effect = EffectHungerMultiplier()
    val REGEN_MULTIPLIER: Effect = EffectRegenMultiplier()
    val PERMANENT_POTION_EFFECT: Effect = EffectPermanentPotionEffect()
    val POTION_EFFECT: Effect = EffectPotionEffect()
    val ARMOR: Effect = EffectArmor()
    val ARMOR_TOUGHNESS: Effect = EffectArmorToughness()
    val GIVE_XP: Effect = EffectGiveXp()
    val XP_MULTIPLIER: Effect = EffectXpMultiplier()
    val BLEED: Effect = EffectBleed()
    val ARROW_RING: Effect = EffectArrowRing()
    val FOOD_MULTIPLIER: Effect = EffectFoodMultiplier()
    val AUTOSMELT: Effect = EffectAutosmelt()
    val TELEPORT: Effect = EffectTeleport()
    val CANCEL_EVENT: Effect = EffectCancelEvent()
    val SEND_MESSAGE: Effect = EffectSendMessage()
    val GIVE_FOOD: Effect = EffectGiveFood()
    val GIVE_HEALTH: Effect = EffectGiveHealth()
    val BREAK_BLOCK: Effect = EffectBreakBlock()
    val REMOVE_POTION_EFFECT: Effect = EffectRemovePotionEffect()
    val PLAY_SOUND: Effect = EffectPlaySound()
    val IGNITE: Effect = EffectIgnite()
    val FEATHER_STEP: Effect = EffectFeatherStep()
    val MINE_RADIUS: Effect = EffectMineRadius()
    val GIVE_POINTS: Effect = EffectGivePoints()
    val SET_POINTS: Effect = EffectSetPoints()
    val MULTIPLY_POINTS: Effect = EffectMultiplyPoints()
    val MULTIPLY_DROPS: Effect = EffectMultiplyDrops()
    val SPAWN_PARTICLE: Effect = EffectSpawnParticle()
    val PULL_TO_LOCATION: Effect = EffectPullToLocation()
    val DAMAGE_ARMOR: Effect = EffectDamageArmor()
    val EXTINGUISH: Effect = EffectExtinguish()
    val GIVE_OXYGEN: Effect = EffectGiveOxygen()
    val RUN_PLAYER_COMMAND: Effect = EffectRunPlayerCommand()
    val DRILL: Effect = EffectDrill()
    val DAMAGE_NEARBY_ENTITIES: Effect = EffectDamageNearbyEntities()
    val SEND_TITLE: Effect = EffectDamageNearbyEntities()

    /**
     * Get effect matching id.
     *
     * @param id The id to query.
     * @return The matching effect, or null if not found.
     */
    fun getByID(id: String): Effect? {
        return BY_ID[id]
    }

    /**
     * List of all registered effects.
     *
     * @return The effects.
     */
    fun values(): List<Effect> {
        return ImmutableList.copyOf(BY_ID.values)
    }

    /**
     * Add new effect.
     *
     * @param effect The effect to add.
     */
    fun addNewEffect(effect: Effect) {
        BY_ID.remove(effect.id)
        BY_ID[effect.id] = effect
    }

    /**
     * Compile an effect.
     *
     * @param config The config for the effect.
     * @param context The context to log violations for.
     *
     * @return The configured effect, or null if invalid.
     */
    @JvmStatic
    fun compile(config: Config, context: String): ConfiguredEffect? {
        val effect = config.getString("id").let {
            val found = getByID(it)
            if (found == null) {
                LibReforgePlugin.instance.logViolation(
                    it,
                    context,
                    ConfigViolation("id", "Invalid effect ID specified!")
                )
            }

            found
        } ?: return null

        val args = config.getSubsection("args")
        if (effect.checkConfig(args, context)) {
            return null
        }

        val filter = config.getSubsectionOrNull("filters").let {
            if (!effect.supportsFilters && it != null) {
                LibReforgePlugin.instance.logViolation(
                    effect.id,
                    context,
                    ConfigViolation("filters", "Specified effect does not support filters")
                )

                return@let null
            }

            if (it == null) EmptyFilter() else ConfiguredFilter(it)
        } ?: return null

        val triggers = config.getStrings("triggers").let {
            val triggers = mutableListOf<Trigger>()

            if (it.isNotEmpty() && effect.applicableTriggers.isEmpty()) {
                LibReforgePlugin.instance.logViolation(
                    effect.id,
                    context,
                    ConfigViolation(
                        "triggers", "Specified effect does not support triggers"
                    )
                )

                return@let null
            }

            if (effect.applicableTriggers.isNotEmpty() && it.isEmpty()) {
                LibReforgePlugin.instance.logViolation(
                    effect.id,
                    context,
                    ConfigViolation(
                        "triggers", "Specified effect requires at least 1 trigger"
                    )
                )

                return@let null
            }

            for (id in it) {
                val trigger = Triggers.getById(id)

                if (trigger == null) {
                    LibReforgePlugin.instance.logViolation(
                        effect.id,
                        context,
                        ConfigViolation(
                            "triggers", "Invalid trigger specified: $id"
                        )
                    )

                    return@let null
                }

                if (!effect.applicableTriggers.contains(trigger)) {
                    LibReforgePlugin.instance.logViolation(
                        effect.id,
                        context,
                        ConfigViolation(
                            "triggers", "Specified effect does not support trigger $id"
                        )
                    )
                }

                triggers.add(trigger)
            }

            triggers
        } ?: return null

        val conditions = config.getSubsections("conditions").mapNotNull {
            Conditions.compile(it, "$context (effect-specific conditions)")
        }

        return ConfiguredEffect(effect, args, filter, triggers, UUID.randomUUID(), conditions)
    }
}
