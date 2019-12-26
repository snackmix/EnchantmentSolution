package org.ctp.enchantmentsolution.nms;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.ctp.enchantmentsolution.EnchantmentSolution;
import org.ctp.enchantmentsolution.nms.damage.*;

public class DamageEvent {
	
	public static void damageEntity(LivingEntity entity, String cause, float damage) {
		switch (EnchantmentSolution.getPlugin().getBukkitVersion().getVersionNumber()) {
			case 1:
				DamageEvent_v1_13_R1.damageEntity(entity, cause, damage);
				break;
			case 2:
			case 3:
				DamageEvent_v1_13_R2.damageEntity(entity, cause, damage);
				break;
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
				DamageEvent_v1_14_R1.damageEntity(entity, cause, damage);
				break;
			case 9:
			case 10:
				DamageEvent_v1_15_R1.damageEntity(entity, cause, damage);
				break;
		}
	}
	
}
