package com.example.examplemod.compat.logic;


import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static dev.xkmc.pandora.init.Pandora.MODID;

public enum GTLang {
	TOOLTIP_FILL("tooltip.fill_crystal", "Progress: %s / %s", 2),
	TOOLTIP_BLOCK("tooltip.consume_block", "Right click %s to absorb essence", 1),
	TOOLTIP_VINE("tooltip.nearby_grow", "Grows when adjacent crops grow to maturity naturally", 0),
	TOOLTIP_STRUCK("tooltip.struck_log", "Ignite to summon lightning", 0),
	TOOLTIP_AFFINITY("tooltip.affinity", "[%s] affinity: %s", 2),
	TOOLTIP_SPELL("tooltip.spell", "%s - [%s]", 2),
	TOOLTIP_COST("tooltip.cost", "Spell cost: %s", 1),
	TOOLTIP_COST_CONT("tooltip.cost_cont", "%s per tick", 1),
	TOOLTIP_COST_CAPPED("tooltip.cost_capped", "%s per tick, at most %s", 2),
	TOOLTIP_FOCUS("tooltip.focus", "Focus cost: %s", 1),
	TOOLTIP_MOB_USE("tooltip.mob_use", "Mobs can use this spell as well", 0),
	TOOLTIP_RITUAL_FORM("tooltip.ritual_form", "Links to [%s] within 3 blocks", 1),
	TOOLTIP_RITUAL_START("tooltip.ritual_start", "Right click with wand to start ritual", 0),
	TOOLTIP_SELECTED("tooltip.selected", "Selected: %s", 1),
	CHANCE_EFFECT("tooltip.chance", "%1$s with %2$s%% chance", 2),
	CHANCE_EXPLODE("tooltip.explode", "%s%% to explode on consumption", 1),
	PLANTABLE("tooltip.plantable", "Plantable on: ", 0),
	PLANTABLE_WATER("tooltip.plantable_water", "Plantable under water on: ", 0),
	SPAWN("tooltip.spawn", "Naturally spawned and grows faster in %s", 1),
	BIOME_PLAINS("tooltip.biome.plains", "Plains Biomes", 0),
	BIOME_FLOWERY("tooltip.biome.flowery", "Flowery Biomes", 0),
	BIOME_OCEAN("tooltip.biome.ocean", "Ocean Biomes", 0),
	BIOME_CORRECT("tooltip.biome.correct", "It grows faster here", 0),
	BIOME_WRONG("tooltip.biome.wrong", "It grows slowly here", 0),

	TOOLTIP_MAGIC("item.glove_magic", "Convert all spell damage to magic damage", 0),
	TOOLTIP_ABYSS("item.glove_abyss", "Infuse all spell damage with abyss damage", 0),
	TOOLTIP_COOLDOWN("item.glove_cooldown", "[%s] spell damage will bypass damage cooldown", 1),

	OVERLAY_DESTROY("overlay.destroy", "Consume target block", 0),
	OVERLAY_MANA("overlay.mana", "Mana: %s / %s", 2),
	OVERLAY_FOCUS("overlay.focus", "Focus: %s / %s", 2),

	DESC_DAMAGE("desc.damage", "Damage", 0),
	DESC_HEAL("desc.heal", "Restore %s every second", 1),
	HP("desc.hp", "HP", 0),
	DESC_FIRE("desc.fire", "Fire", 0),
	DESC_EXPLOSION("desc.explosion", "Explosion", 0),
	DESC_FREEZING("desc.freezing", "Freezing", 0),
	DESC_PROJECTILE("desc.projectile", "Projectile", 0),
	DESC_LIGHTNING("desc.lightning", "Lightning", 0),
	DESC_MAGIC("desc.magic", "Magic", 0),
	DESC_FALLING_BLOCK("desc.falling_block", "Falling Block", 0),
	DESC_SPACE("desc.space", " ", 0),
	DESC_DMG("desc.damage_number", "%s", 1),
	DESC_GROUND("desc.ground", "Only Resolvable on ground", 0),
	DESC_SHIFT("desc.shift", "Press [Shift] for research info", 0),

	JEI_STRIKE_ITEM("jei.strike_item", "Lightning Strikes Item", 0),
	JEI_STRIKE_BLOCK("jei.strike_block", "Lightning Strikes Block", 0),
	JEI_TRANSFORM("jei.transform", "Item Special Transformation", 0),
	JEI_RITUAL("jei.ritual", "Ritual", 0),

	HEX_STATUS("hex.status", "Research status: %s", 1),
	HEX_COST("hex.cost", "Cost: %s", 1),
	HEX_BONUS("hex.bonus", "Research Bonus:", 0),
	HEX_BONUS_MANA("hex.bonus_mana", "- Cost <= %s: -%s%% mana cost", 2),
	HEX_BONUS_FOCUS("hex.bonus_focus", "- Cost <= %s: -%s%% focus cost", 2),
	HEX_BONUS_BOTH("hex.bonus_both", "- Cost <= %s: -%s%% mana cost, -%s%% focus cost", 3),
	;

	final String id, def;
	final int count;

	GTLang(String id, String def, int count) {
		this.id = MODID + "." + id;
		this.def = def;
		this.count = count;
	}

	public MutableComponent get(Object... objs) {
		if (objs.length != this.count) {
			throw new IllegalArgumentException("for " + this.name() + ": expect " + this.count + " parameters, got " + objs.length);
		} else {
			return Component.translatable(this.id, objs);
		}
	}

	public interface EnumLang {

		String name();

		String defText();


		default MutableComponent getDesc() {
			return null;
		}

	}


}
