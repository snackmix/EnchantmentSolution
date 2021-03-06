package org.ctp.enchantmentsolution.events.damage;

import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.ctp.enchantmentsolution.enchantments.CERegister;
import org.ctp.enchantmentsolution.enchantments.helper.EnchantmentLevel;
import org.ctp.enchantmentsolution.events.entity.ESDamageEntityEvent;

public class IronDefenseEvent extends ESDamageEntityEvent {

	private int shieldDamage;
	private final ItemStack shield;

	public IronDefenseEvent(LivingEntity damaged, int level, double damage, double newDamage, ItemStack shield,
	int shieldDamage) {
		super(damaged, new EnchantmentLevel(CERegister.IRON_DEFENSE, level), damage, newDamage);
		this.shield = shield;
		setShieldDamage(shieldDamage);
	}

	@Override
	public void setNewDamage(double newDamage) {
		if (newDamage > getDamage()) super.setNewDamage(getDamage());
		else
			super.setNewDamage(newDamage);
	}

	public int getShieldDamage() {
		return shieldDamage;
	}

	public void setShieldDamage(int shieldDamage) {
		this.shieldDamage = shieldDamage;
	}

	public ItemStack getShield() {
		return shield;
	}

}
