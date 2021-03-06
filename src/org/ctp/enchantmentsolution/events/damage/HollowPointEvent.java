package org.ctp.enchantmentsolution.events.damage;

import org.bukkit.entity.LivingEntity;
import org.ctp.enchantmentsolution.enchantments.CERegister;
import org.ctp.enchantmentsolution.enchantments.helper.EnchantmentLevel;
import org.ctp.enchantmentsolution.events.entity.ESEntityDamageEntityEvent;

public class HollowPointEvent extends ESEntityDamageEntityEvent {

	public HollowPointEvent(LivingEntity damaged, LivingEntity damager, double damage, double newDamage) {
		super(damaged, new EnchantmentLevel(CERegister.HOLLOW_POINT, 1), damager, damage, newDamage);
	}

}
