package org.ctp.enchantmentsolution.enchantments.generate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.ctp.enchantmentsolution.enchantments.CustomEnchantment;
import org.ctp.enchantmentsolution.enchantments.RegisterEnchantments;
import org.ctp.enchantmentsolution.enchantments.helper.EnchantmentLevel;
import org.ctp.enchantmentsolution.enums.ItemType;
import org.ctp.enchantmentsolution.utils.ConfigUtils;
import org.ctp.enchantmentsolution.utils.config.MainConfiguration;
import org.ctp.enchantmentsolution.utils.items.DamageUtils;
import org.ctp.enchantmentsolution.utils.items.ItemUtils;

public class GrindstoneEnchantments extends GenerateEnchantments {

	private int takeCost;
	private ItemStack itemTwo, combinedItem, takenItem;
	private boolean canCombine, takeEnchantments;

	private GrindstoneEnchantments(Player player, ItemStack item, ItemStack itemTwo, boolean takeEnchantments) {
		super(player, item, false);
		this.itemTwo = itemTwo;

		setCanCombine();
		if (canCombine) {
			combineItems();
		}
		if (takeEnchantments) {
			setTakeEnchantments();
			if (canTakeEnchantments()) {
				takeEnchantments();
			}
		}
	}

	public static GrindstoneEnchantments getGrindstoneEnchantments(Player player, ItemStack first, ItemStack second,
			boolean grindstoneTakeEnchantments) {
		return new GrindstoneEnchantments(player, first, second, grindstoneTakeEnchantments);
	}

	private void setCanCombine() {
		if (getItemTwo() != null && getItem().getType() == getItemTwo().getType()) {
			canCombine = true;
			return;
		} else if (getItem() != null) {
			canCombine = true;
			return;
		}
		canCombine = false;
	}

	public boolean canCombine() {
		return canCombine;
	}

	private void setTakeEnchantments() {
		ItemStack item = getItem();
		if (item.getType() != Material.BOOK && item.getType() != Material.ENCHANTED_BOOK && item.hasItemMeta()
				&& item.getItemMeta().hasEnchants()) {
			if (itemTwo.getType() == Material.BOOK && (itemTwo.hasItemMeta() && !itemTwo.getItemMeta().hasEnchants())
					|| !itemTwo.hasItemMeta()) {
				takeEnchantments = true;
			}
		}
		takeEnchantments = false;
	}

	public boolean canTakeEnchantments() {
		return takeEnchantments;
	}

	private void takeEnchantments() {
		takenItem = new ItemStack(itemTwo.getType());
		boolean book = ConfigUtils.getBoolean(MainConfiguration.class, "use_enchanted_books");
		if (takenItem.getType() == Material.BOOK && book) {
			takenItem = ItemUtils.convertToEnchantedBook(takenItem);
		} else if (takenItem.getType() == Material.ENCHANTED_BOOK && !book) {
			takenItem = ItemUtils.convertToRegularBook(takenItem);
		}

		List<EnchantmentLevel> enchantments = new ArrayList<EnchantmentLevel>();
		for(Iterator<java.util.Map.Entry<Enchantment, Integer>> it = getItem().getEnchantments().entrySet()
				.iterator(); it.hasNext();) {
			java.util.Map.Entry<Enchantment, Integer> e = it.next();
			for(CustomEnchantment ench: RegisterEnchantments.getEnchantments()) {
				if (ench.getRelativeEnchantment().equals(e.getKey())) {
					enchantments.add(new EnchantmentLevel(ench, e.getValue()));
				}
			}
		}

		takenItem = ItemUtils.addEnchantmentsToItem(takenItem, enchantments);

		setTakeCost();
	}

	private void setTakeCost() {
		int cost = 0;
		ItemStack item = getItem();
		ItemMeta itemMeta = item.clone().getItemMeta();
		Map<Enchantment, Integer> enchants = itemMeta.getEnchants();
		for(Iterator<java.util.Map.Entry<Enchantment, Integer>> it = enchants.entrySet().iterator(); it.hasNext();) {
			java.util.Map.Entry<Enchantment, Integer> e = it.next();
			Enchantment enchant = e.getKey();
			int level = e.getValue();
			for(CustomEnchantment customEnchant: RegisterEnchantments.getEnchantments()) {
				if (ConfigUtils.isRepairable(customEnchant) && customEnchant.getRelativeEnchantment().equals(enchant)) {
					cost += level * customEnchant.multiplier(item.getType());
				}
			}
		}
		takeCost = Math.max(cost / ConfigUtils.getInt(MainConfiguration.class, "anvil.get_level_divisor"), 1);
	}

	public int getExperience() {
		Random random = new Random();
		byte b0 = 0;
		int j = b0;

		if (getItem() != null) {
			j += getEnchantmentExperience(getItem());
		}
		if (itemTwo != null) {
			j += getEnchantmentExperience(itemTwo);
		}

		if (j > 0) {
			int k = (int) Math.ceil(j / 2.0D);
			return k + random.nextInt(k);
		} else {
			return 0;
		}
	}

	private int getEnchantmentExperience(ItemStack itemstack) {
		int j = 0;
		Map<Enchantment, Integer> map = itemstack.getItemMeta().getEnchants();
		if (itemstack.getType().equals(Material.ENCHANTED_BOOK)) {
			EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemstack.getItemMeta();
			map = meta.getStoredEnchants();
		}
		Iterator<Entry<Enchantment, Integer>> iterator = map.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<Enchantment, Integer> entry = iterator.next();
			Enchantment enchantment = entry.getKey();
			Integer integer = entry.getValue();

			CustomEnchantment custom = RegisterEnchantments.getCustomEnchantment(enchantment);

			if (!RegisterEnchantments.getCustomEnchantment(enchantment).isCurse()) {
				j += custom.enchantability(integer);
			}
		}

		return j;
	}

	private void combineItems() {
		ItemStack item = getItem();
		combinedItem = item.clone();
		if (item.getType().equals(Material.ENCHANTED_BOOK)) {
			combinedItem = new ItemStack(Material.BOOK);
		}
		combinedItem = ItemUtils.removeAllEnchantments(combinedItem);

		if (itemTwo != null) {
			if (item.getType() != Material.BOOK && item.getType() != Material.ENCHANTED_BOOK
					&& ItemType.hasItemType(item.getType())) {
				DamageUtils.setDamage(combinedItem, DamageUtils.getDamage(item.getItemMeta()));
				int extraDurability = itemTwo.getType().getMaxDurability()
						- DamageUtils.getDamage(itemTwo.getItemMeta())
						+ (int) (itemTwo.getType().getMaxDurability() * .05);
				DamageUtils.setDamage(combinedItem, DamageUtils.getDamage(item.getItemMeta()) - extraDurability);
				if (DamageUtils.getDamage(combinedItem.getItemMeta()) < 0) {
					DamageUtils.setDamage(combinedItem, 0);
				}
			} else {
				combinedItem.setAmount(1);
			}
		} else {
			DamageUtils.setDamage(combinedItem, DamageUtils.getDamage(item.getItemMeta()));
		}

		combinedItem = ItemUtils.addEnchantmentsToItem(combinedItem, combineEnchants());
	}

	private List<EnchantmentLevel> combineEnchants() {
		ItemStack item = getItem();
		ItemMeta itemMeta = item.clone().getItemMeta();
		Map<Enchantment, Integer> firstEnchants = itemMeta.getEnchants();
		if (item.getType().equals(Material.ENCHANTED_BOOK)) {
			EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemMeta;
			firstEnchants = meta.getStoredEnchants();
		}
		List<EnchantmentLevel> enchantments = new ArrayList<EnchantmentLevel>();

		Iterator<Entry<Enchantment, Integer>> firstIter = firstEnchants.entrySet().iterator();
		while (firstIter.hasNext()) {
			Entry<Enchantment, Integer> entry = firstIter.next();
			CustomEnchantment custom = RegisterEnchantments.getCustomEnchantment(entry.getKey());
			if (custom.isCurse()) {
				boolean contains = false;
				for(EnchantmentLevel enchantment: enchantments) {
					if (enchantment.getEnchant().getRelativeEnchantment().equals(entry.getKey())) {
						contains = true;
						break;
					}
				}
				if (!contains)
					enchantments.add(new EnchantmentLevel(custom, entry.getValue()));
			}
		}

		if (itemTwo != null) {
			ItemMeta itemTwoMeta = itemTwo.getItemMeta();
			Map<Enchantment, Integer> secondEnchants = itemTwoMeta.getEnchants();
			if (itemTwo.getType().equals(Material.ENCHANTED_BOOK)) {
				EnchantmentStorageMeta meta = (EnchantmentStorageMeta) itemTwoMeta;
				secondEnchants = meta.getStoredEnchants();
			}
			Iterator<Entry<Enchantment, Integer>> secondIter = secondEnchants.entrySet().iterator();
			while (secondIter.hasNext()) {
				Entry<Enchantment, Integer> entry = secondIter.next();
				CustomEnchantment custom = RegisterEnchantments.getCustomEnchantment(entry.getKey());
				if (custom.isCurse()) {
					boolean contains = false;
					for(EnchantmentLevel enchantment: enchantments) {
						if (enchantment.getEnchant().getRelativeEnchantment().equals(entry.getKey())) {
							contains = true;
							break;
						}
					}
					if (!contains)
						enchantments.add(new EnchantmentLevel(custom, entry.getValue()));
				}
			}
		}

		for(int i = enchantments.size() - 1; i >= 0; i--) {
			EnchantmentLevel enchant = enchantments.get(i);
			if (!enchant.getEnchant().canAnvil(getPlayer(), enchant.getLevel())) {
				int level = enchant.getEnchant().getAnvilLevel(getPlayer(), enchant.getLevel());
				if (level > 0) {
					enchantments.get(i).setLevel(level);
				} else {
					enchantments.remove(i);
				}
			}
		}

		return enchantments;
	}

	public int getTakeCost() {
		return takeCost;
	}

	public ItemStack getItemTwo() {
		return itemTwo;
	}

	public ItemStack getTakenItem() {
		return takenItem;
	}

	public ItemStack getCombinedItem() {
		return combinedItem;
	}

	public boolean isCanCombine() {
		return canCombine;
	}

	public boolean isTakeEnchantments() {
		return takeEnchantments;
	}

}
