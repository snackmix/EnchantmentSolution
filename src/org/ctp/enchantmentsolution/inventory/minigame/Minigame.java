package org.ctp.enchantmentsolution.inventory.minigame;

import java.util.*;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ctp.enchantmentsolution.EnchantmentSolution;
import org.ctp.enchantmentsolution.enchantments.CustomEnchantment;
import org.ctp.enchantmentsolution.enchantments.RegisterEnchantments;
import org.ctp.enchantmentsolution.enchantments.generate.MinigameEnchantments;
import org.ctp.enchantmentsolution.enchantments.helper.EnchantmentLevel;
import org.ctp.enchantmentsolution.enchantments.helper.EnchantmentList;
import org.ctp.enchantmentsolution.enums.*;
import org.ctp.enchantmentsolution.inventory.InventoryData;
import org.ctp.enchantmentsolution.inventory.Pageable;
import org.ctp.enchantmentsolution.inventory.minigame.MinigameItem.MinigameItemType;
import org.ctp.enchantmentsolution.utils.*;
import org.ctp.enchantmentsolution.utils.config.ConfigString;
import org.ctp.enchantmentsolution.utils.items.ItemUtils;
import org.ctp.enchantmentsolution.utils.yaml.YamlConfig;

public class Minigame implements InventoryData, Pageable {

	private Player player;
	private Inventory inventory;
	private Block block;
	private boolean opening;
	private static List<MinigameItem> MINIGAME_ITEMS = null;
	private static Map<UUID, Map<MinigameItem, Integer>> TIMES_USED = new HashMap<UUID, Map<MinigameItem, Integer>>();
	private final List<Integer> fastLocations = Arrays.asList(10, 12, 14, 16, 19, 21, 23, 25, 28, 30, 32, 34, 37, 39, 41, 43);
	private final List<Integer> mondaysLocations = Arrays.asList(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43);
	private final List<Integer> customLocationsSeven = Arrays.asList(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43);
	private final List<Integer> customLocationsNine = Arrays.asList(9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44);
	private Screen screen;
	private int page = 1;

	public Minigame(Player player, Block block) {
		setPlayer(player);
		this.block = block;
	}

	@Override
	public void setInventory() {
		screen = Screen.FAST;
		try {
			screen = Screen.valueOf(ConfigString.MINIGAME_TYPE.getString().toUpperCase());
		} catch (Exception ex) {}
		try {
			if (screen == Screen.FAST) {
				Inventory inv = Bukkit.createInventory(null, 54, ChatUtils.getMessage(getCodes(), "minigame.name"));
				inv = open(inv);
				ItemStack mirror = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
				ItemMeta mirrorMeta = mirror.getItemMeta();
				mirrorMeta.setDisplayName(ChatUtils.getMessage(getCodes(), "minigame.mirror"));
				mirror.setItemMeta(mirrorMeta);
				for(int i = 0; i < 54; i++)
					inv.setItem(i, mirror);

				List<ItemType> types = ItemType.getUniqueEnchantableTypes();
				for(int i = 0; i < 16; i++) {
					ItemType type = types.get(i);
					int slot = fastLocations.get(i);
					ItemStack item = new ItemStack(getMaterial(type));
					ItemMeta itemMeta = item.getItemMeta();
					HashMap<String, Object> itemCodes = getCodes();
					itemCodes.put("%name%", type.getDisplayName());
					itemMeta.setDisplayName(ChatUtils.getMessage(itemCodes, "minigame.fast.item"));
					itemMeta.setLore(ChatUtils.getMessages(getCodes(), "minigame.fast.item_lore"));
					item.setItemMeta(itemMeta);

					inv.setItem(slot, item);
				}
			} else if (screen == Screen.MONDAYS) {
				Inventory inv = Bukkit.createInventory(null, 54, ChatUtils.getMessage(getCodes(), "minigame.name"));
				inv = open(inv);
				ItemStack mirror = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
				ItemMeta mirrorMeta = mirror.getItemMeta();
				mirrorMeta.setDisplayName(ChatUtils.getMessage(getCodes(), "minigame.mirror"));
				mirror.setItemMeta(mirrorMeta);
				for(int i = 0; i < 54; i++)
					inv.setItem(i, mirror);

				List<String> enchants = getEnchants();

				for(int i = 0; i < mondaysLocations.size(); i++) {
					if (enchants.size() <= i) break;
					int num = i + mondaysLocations.size() * (page - 1);
					String name = Configurations.getMinigames().getString("mondays.enchantments." + enchants.get(num) + ".enchantment");
					if (name == null) continue;
					CustomEnchantment enchant = RegisterEnchantments.getByName(name);
					int slot = mondaysLocations.get(i);
					ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
					ItemMeta itemMeta = item.getItemMeta();
					HashMap<String, Object> itemCodes = getCodes();
					itemCodes.put("%name%", enchant == null ? name.equalsIgnoreCase("random") ? Configurations.getLanguage().getString("enchantment.random") : name : enchant.getDisplayName());
					HashMap<String, Object> itemLoreCodes = getCodes();
					itemLoreCodes.put("%cost%", Configurations.getMinigames().getInt("mondays.enchantments." + enchants.get(num) + ".cost"));
					itemMeta.setDisplayName(ChatUtils.getMessage(itemCodes, "minigame.mondays.item"));
					itemMeta.setLore(ChatUtils.getMessages(itemLoreCodes, "minigame.mondays.item_lore"));
					item.setItemMeta(itemMeta);

					inv.setItem(slot, item);
				}
				if (enchants.size() > mondaysLocations.size() * page) inv.setItem(53, nextPage());
				if (page != 1) inv.setItem(45, previousPage());
			} else if (screen == Screen.CUSTOM) {
				Inventory inv = Bukkit.createInventory(null, 54, ChatUtils.getMessage(getCodes(), "minigame.name"));
				inv = open(inv);
				ItemStack mirror = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
				ItemMeta mirrorMeta = mirror.getItemMeta();
				mirrorMeta.setDisplayName(ChatUtils.getMessage(getCodes(), "minigame.mirror"));
				mirror.setItemMeta(mirrorMeta);
				for(int i = 0; i < 54; i++)
					inv.setItem(i, mirror);

				int paging = getPaging();
				List<MinigameItem> items = getMinigameCustomItems();
				for(int i = 0; i < paging; i++) {
					if (items.size() <= i) break;
					int num = i + paging * (page - 1);
					MinigameItem item = items.get(num);
					if (item == null) continue;
					int slot = paging % 9 == 0 ? customLocationsNine.get(i) : customLocationsSeven.get(i);
					ItemStack show = new ItemStack(item.getShow().hasMaterial() ? item.getShow().getMaterial() : Material.ENCHANTED_BOOK);
					ItemMeta showMeta = show.getItemMeta();
					HashMap<String, Object> codes = getCodes();
					codes.put("%material%", item.getEnchant().hasMaterial() ? item.getEnchant().getMaterial().name() : item.getEnchant().getMaterialName());
					showMeta.setDisplayName(ChatUtils.getMessage(codes, "minigame.custom.item.name"));
					int cost = item.getCost();
					if (item.willIncreaseCost()) {
						Map<MinigameItem, Integer> timesUsed = TIMES_USED.get(player.getUniqueId());
						if (timesUsed != null && timesUsed.get(item) != null) {
							int times = timesUsed.get(item);
							cost += times * item.getExtraCost();
						}
					}
					if (item.getMaxCost() > 0 && cost > item.getMaxCost()) cost = item.getMaxCost();
					if (item.getType() == MinigameItemType.ENCHANTMENT) {
						List<String> lore = new ArrayList<String>();
						for(EnchantmentLevel level: item.getLevels()) {
							HashMap<String, Object> loreCodes = getCodes();
							loreCodes.put("%enchantment%", level.getEnchant().getDisplayName());
							loreCodes.put("%level%", level.getLevel());
							lore.add(ChatUtils.getMessage(loreCodes, "minigame.custom.item.enchantment"));
						}
						HashMap<String, Object> loreCodes = getCodes();
						loreCodes.put("%cost%", cost);
						lore.add(ChatUtils.getMessage(loreCodes, "minigame.custom.item.enchantment_cost"));
						showMeta.setLore(lore);
					} else {
						HashMap<String, Object> loreCodes = getCodes();
						loreCodes.put("%multiple%", item.getType().isMultiple());
						loreCodes.put("%cost%", cost);
						showMeta.setLore(ChatUtils.getMessages(loreCodes, "minigame.custom.item.random"));
					}
					show.setItemMeta(showMeta);

					inv.setItem(slot, show);
				}
				if (items.size() > paging * page) inv.setItem(53, nextPage());
				if (page != 1) inv.setItem(45, previousPage());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

	@Override
	public Inventory getInventory() {
		return inventory;
	}

	@Override
	public Block getBlock() {
		return block;
	}

	@Override
	public void close(boolean external) {
		if (EnchantmentSolution.getPlugin().hasInventory(this)) {
			EnchantmentSolution.getPlugin().removeInventory(this);
			if (!external) player.getOpenInventory().close();
		}
	}

	@Override
	public void setInventory(List<ItemStack> items) {}

	@Override
	public void setItemName(String name) {}

	@Override
	public Inventory open(Inventory inv) {
		opening = true;
		if (inventory == null) {
			inventory = inv;
			player.openInventory(inv);
		} else if (inv.getSize() == inventory.getSize()) {
			inv = player.getOpenInventory().getTopInventory();
			inventory = inv;
		} else {
			inventory = inv;
			player.openInventory(inv);
		}
		for(int i = 0; i < inventory.getSize(); i++)
			inventory.setItem(i, new ItemStack(Material.AIR));
		if (opening) opening = false;
		return inv;
	}

	@Override
	public List<ItemStack> getItems() {
		return null;
	}

	public HashMap<String, Object> getCodes() {
		HashMap<String, Object> codes = new HashMap<String, Object>();
		codes.put("%player%", player.getName());
		return codes;
	}

	public enum Screen {
		FAST(), MONDAYS(), CUSTOM();
	}

	public Screen getScreen() {
		return screen;
	}

	public Material getMaterial(ItemType type) {
		Material m = Material.BOOK;
		for(ItemData data: type.getEnchantMaterials())
			if (data.getMaterial() != Material.BOOK && data.getMaterial() != Material.ENCHANTED_BOOK) {
				m = data.getMaterial();
				break;
			}
		return m;
	}

	public void addFastEnchantment(int slot) {
		int num = fastLocations.indexOf(slot);
		ItemType type = ItemType.getUniqueEnchantableTypes().get(num);

		ItemStack item = new ItemStack(getMaterial(type));
		MinigameEnchantments enchants = GenerateUtils.generateMinigameEnchants(player, item, block);
		EnchantmentList[] list = enchants.getList();
		List<EnchantmentList> lists = new ArrayList<EnchantmentList>();
		if (list != null) for(EnchantmentList l: list)
			if (l != null) lists.add(l);
		int random = (int) (Math.random() * lists.size());

		List<EnchantmentLevel> levels = lists.get(random).getEnchantments();

		int i = 0;

		while ((levels == null || levels.size() == 0) && lists.size() > i) {
			levels = lists.get(i).getEnchantments();
			random = i;
			i++;
		}
		if (levels == null || levels.size() == 0) {
			ChatUtils.sendWarning("Item couldn't find EnchantmentSolution enchantments.");
			return;
		}
		item.setType(Material.BOOK);
		item = ItemUtils.addEnchantmentsToItem(item, levels);

		boolean useBooks = ConfigString.USE_ENCHANTED_BOOKS.getBoolean();
		if (item.getType() == Material.BOOK && useBooks) item = ItemUtils.convertToEnchantedBook(item);
		int cost = MinigameUtils.getTableCost(random + 1);
		if (cost <= player.getLevel() || player.getGameMode() == GameMode.CREATIVE) {
			if (player.getGameMode() != GameMode.CREATIVE) player.setLevel(player.getLevel() - cost);
			ItemUtils.giveItemToPlayer(player, item, player.getLocation(), false);
			player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
		} else
			ChatUtils.sendMessage(player, ChatUtils.getMessage(getCodes(), "minigame.not_enough_levels"));
	}

	public void addMondaysEnchantment(int slot) {
		int num = mondaysLocations.indexOf(slot) + mondaysLocations.size() * (page - 1);
		List<String> enchants = getEnchants();

		String s = enchants.get(num);
		String name = Configurations.getMinigames().getString("mondays.enchantments." + s + ".enchantment");
		CustomEnchantment ench = RegisterEnchantments.getByName(name);

		if (ench == null && name.equalsIgnoreCase("random")) {
			List<EnchantmentLevel> levels = GenerateUtils.generateBookLoot(player, new ItemStack(Material.BOOK), EnchantmentLocation.NONE);
			int tries = 50;
			while (levels.size() == 0 && tries > 0) {
				tries--;
				levels = GenerateUtils.generateBookLoot(player, new ItemStack(Material.BOOK), EnchantmentLocation.NONE);
			}
			if (levels.size() > 0) ench = levels.get(0).getEnchant();
		}
		if (ench != null) {
			ItemStack item = new ItemStack(Material.BOOK);
			ItemUtils.addEnchantmentToItem(item, ench, 1);
			boolean useBooks = ConfigString.USE_ENCHANTED_BOOKS.getBoolean();
			if (item.getType() == Material.BOOK && useBooks) item = ItemUtils.convertToEnchantedBook(item);
			int cost = Configurations.getMinigames().getInt("mondays.enchantments." + s + ".cost");
			if (player.getLevel() >= cost || player.getGameMode() == GameMode.CREATIVE) {
				if (player.getGameMode() != GameMode.CREATIVE) player.setLevel(player.getLevel() - cost);
				ItemUtils.giveItemToPlayer(player, item, player.getLocation(), false);
				player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
			} else
				ChatUtils.sendMessage(player, ChatUtils.getMessage(getCodes(), "minigame.not_enough_levels"));
		} else if (name.equalsIgnoreCase("random")) ChatUtils.sendMessage(player, ChatUtils.getMessage(getCodes(), "minigame.invalid_random_enchant"));
		else {
			HashMap<String, Object> codes = getCodes();
			codes.put("%name%", name);
			ChatUtils.sendMessage(player, ChatUtils.getMessage(codes, "minigame.invalid_enchant_name"));
		}
	}

	public void addCustomEnchantment(int slot) {
		List<Integer> list = getPaging() % 9 == 0 ? customLocationsNine : customLocationsSeven;
		int num = list.indexOf(slot) + list.size() * (page - 1);
		MinigameItem item = getMinigameCustomItems().get(num);
		if (item != null) {
			ItemStack enchant = new ItemStack(item.getEnchant().getMaterial());
			boolean useBooks = ConfigString.USE_ENCHANTED_BOOKS.getBoolean();
			if (enchant.getType() == Material.BOOK && useBooks) enchant = ItemUtils.convertToEnchantedBook(enchant);
			int cost = item.getCost();
			if (item.willIncreaseCost()) {
				Map<MinigameItem, Integer> timesUsed = TIMES_USED.get(player.getUniqueId());
				if (timesUsed != null && timesUsed.get(item) != null) {
					int times = timesUsed.get(item);
					cost += times * item.getExtraCost();
				}
			}
			if (item.getMaxCost() > 0 && cost > item.getMaxCost()) cost = item.getMaxCost();
			if (item.getType() == MinigameItemType.ENCHANTMENT) enchant = ItemUtils.addEnchantmentsToItem(enchant, item.getLevels());
			else
				enchant = GenerateUtils.generateMinigameLoot(player, enchant, block, item);
			if (player.getLevel() >= cost || player.getGameMode() == GameMode.CREATIVE) {
				if (player.getGameMode() != GameMode.CREATIVE) player.setLevel(player.getLevel() - cost);
				ItemUtils.giveItemToPlayer(player, enchant, player.getLocation(), false);
				player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
				if (item.willIncreaseCost()) {
					Map<MinigameItem, Integer> itemHash = new HashMap<MinigameItem, Integer>();
					if (!TIMES_USED.containsKey(player.getUniqueId())) {
						itemHash.put(item, 1);
						TIMES_USED.put(player.getUniqueId(), itemHash);
					} else {
						itemHash = TIMES_USED.get(player.getUniqueId());
						if (itemHash == null) itemHash = new HashMap<MinigameItem, Integer>();
						int timesUsed = 0;
						if (itemHash.containsKey(item)) timesUsed = itemHash.get(item);
						itemHash.put(item, timesUsed + 1);
						TIMES_USED.put(player.getUniqueId(), itemHash);
					}
				}
			} else
				ChatUtils.sendMessage(player, ChatUtils.getMessage(getCodes(), "minigame.not_enough_levels"));
		} else {
			HashMap<String, Object> codes = getCodes();
			ChatUtils.sendMessage(player, ChatUtils.getMessage(codes, "minigame.invalid_item"));
		}
	}

	@Override
	public int getPage() {
		return page;
	}

	@Override
	public void setPage(int page) {
		this.page = page;
	}

	private List<String> getEnchants() {
		YamlConfig config = Configurations.getMinigames().getConfig();
		List<String> paths = config.getLevelEntryKeysAtLevel("mondays.enchantments");
		paths.sort((o1, o2) -> {
			Integer i1 = null, i2 = null;
			try {
				i1 = Integer.parseInt(o1);
			} catch (NumberFormatException ex1) {}
			try {
				i2 = Integer.parseInt(o2);
			} catch (NumberFormatException ex2) {}
			if (i1 != null && i2 == null) return 1;
			else if (i1 == null && i2 != null) return -1;
			else if (i1 != null && i2 != null) return i1.intValue() - i2.intValue();
			return o1.compareTo(o2);
		});
		return paths;
	}

	private int getPaging() {
		int paging = ConfigString.MINIGAME_CUSTOM_PAGING.getInt();
		if (Arrays.asList(7, 9, 14, 18, 21, 27, 28, 36).contains(paging)) return paging;
		return 28;
	}

	private List<MinigameItem> getMinigameCustomItems() {
		if (MINIGAME_ITEMS == null) {
			YamlConfig config = Configurations.getMinigames().getConfig();
			List<String> keys = config.getLevelEntryKeys("custom.items");
			List<MinigameItem> items = new ArrayList<MinigameItem>();
			for(String key: keys) {
				List<EnchantmentLevel> levels = new ArrayList<EnchantmentLevel>();
				for(String s: config.getStringList(key + ".enchantments")) {
					EnchantmentLevel level = new EnchantmentLevel(s);
					if (level != null) levels.add(level);
				}
				try {
					items.add(new MinigameItem(new MatData(config.getString(key + ".material.show")), new MatData(config.getString(key + ".material.enchant")), MinigameItemType.valueOf(config.getString(key + ".type").toUpperCase()), config.getInt(key + ".cost"), config.getInt(key + ".increase.extra_cost_per_use"), config.getInt(key + ".increase.max_cost"), config.getInt(key + ".books.min"), config.getInt(key + ".books.max"), config.getInt(key + ".levels.min"), config.getInt(key + ".levels.max"), config.getInt(key + ".slot"), config.getBoolean(key + ".increase.use"), levels));
				} catch (IllegalArgumentException ex) {
					ex.printStackTrace();
				}
			}

			MINIGAME_ITEMS = new ArrayList<MinigameItem>();
			for(MinigameItem item: items) {
				while (MINIGAME_ITEMS.size() <= item.getSlot())
					MINIGAME_ITEMS.add(null);
				MINIGAME_ITEMS.set(item.getSlot(), item);
			}
		}

		return MINIGAME_ITEMS;
	}

	public static void reset() {
		MINIGAME_ITEMS = null;
		TIMES_USED = new HashMap<UUID, Map<MinigameItem, Integer>>();

		EnchantmentSolution.getPlugin().closeInventories(Minigame.class);
	}

}
