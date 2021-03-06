package org.ctp.enchantmentsolution.utils.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.ctp.enchantmentsolution.enchantments.CustomEnchantment;
import org.ctp.enchantmentsolution.enchantments.RegisterEnchantments;
import org.ctp.enchantmentsolution.enchantments.helper.EnchantmentLevel;
import org.ctp.enchantmentsolution.enums.EnchantmentLocation;
import org.ctp.enchantmentsolution.enums.ItemData;
import org.ctp.enchantmentsolution.utils.ChatUtils;
import org.ctp.enchantmentsolution.utils.GenerateUtils;
import org.ctp.enchantmentsolution.utils.MiscUtils;
import org.ctp.enchantmentsolution.utils.config.ConfigString;
import org.ctp.enchantmentsolution.utils.items.ItemUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class EnchantCommandUtils {

	public static boolean enchant(CommandSender sender, ESCommand details, String[] args, boolean unsafe) {
		Player player = null;
		Player givePlayer = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			givePlayer = player;
		}

		if (sender.hasPermission(details.getPermission())) {
			if (args.length > 1) {
				String enchantmentName = args[1];
				for(CustomEnchantment enchant: RegisterEnchantments.getRegisteredEnchantments())
					if (enchant.getName().equalsIgnoreCase(enchantmentName)) {
						if (!enchant.isEnabled()) {
							HashMap<String, Object> codes = ChatUtils.getCodes();
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.enchant-disabled"), Level.WARNING);
							return true;
						}
						int level = 1;
						if (args.length > 2) try {
							level = Integer.parseInt(args[2]);
							if (level < 1) {
								HashMap<String, Object> codes = ChatUtils.getCodes();
								codes.put("%level%", level);
								ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.level-too-low"), Level.WARNING);
								level = 1;
							}
						} catch (NumberFormatException ex) {
							HashMap<String, Object> codes = ChatUtils.getCodes();
							codes.put("%level%", args[2]);
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-level"), Level.WARNING);
						}
						if (level > enchant.getMaxLevel() && !unsafe) {
							HashMap<String, Object> codes = ChatUtils.getCodes();
							codes.put("%level%", level);
							codes.put("%maxLevel%", enchant.getMaxLevel());
							codes.put("%enchant%", enchant.getDisplayName());
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.level-too-high"), Level.WARNING);
							level = enchant.getMaxLevel();
						}
						if (args.length > 3) {
							String arg = args[3];
							if (!arg.equals("@p")) {
								givePlayer = Bukkit.getPlayer(arg);
								if (givePlayer == null) {
									HashMap<String, Object> codes = ChatUtils.getCodes();
									codes.put("%invalid_player%", args[3]);
									ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-player"), Level.WARNING);
									return false;
								} else if (givePlayer != null && !givePlayer.equals(sender) && !sender.hasPermission(details.getPermission() + ".others")) {
									ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.no-permission-other"), Level.WARNING);
									return false;
								}
							}
						}
						if (givePlayer == null) {
							HashMap<String, Object> codes = ChatUtils.getCodes();
							codes.put("%player%", ChatUtils.getMessage(codes, "commands.invalid.console"));
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-player"), Level.WARNING);
							return true;
						}
						int slot = givePlayer.getInventory().getHeldItemSlot();
						if (args.length > 4) try {
							slot = Integer.parseInt(args[4]);
							if (slot < 0) {
								HashMap<String, Object> codes = ChatUtils.getCodes();
								codes.put("%slot%", slot);
								ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.slot-too-low"), Level.WARNING);
								slot = givePlayer.getInventory().getHeldItemSlot();
							} else if (slot >= 45) {
								HashMap<String, Object> codes = ChatUtils.getCodes();
								codes.put("%slot%", slot);
								ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.slot-too-high"), Level.WARNING);
								slot = givePlayer.getInventory().getHeldItemSlot();
							}
						} catch (NumberFormatException ex) {
							HashMap<String, Object> codes = ChatUtils.getCodes();
							codes.put("%slot%", args[4]);
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-slot"), Level.WARNING);
							slot = givePlayer.getInventory().getHeldItemSlot();
						}
						ItemStack itemToEnchant = givePlayer.getInventory().getItem(slot);
						if (itemToEnchant != null) {
							if (!unsafe) {
								int maxEnchants = ConfigString.MAX_ENCHANTMENTS.getInt();
								int itemEnchants = ItemUtils.getTotalEnchantments(itemToEnchant);
								if (maxEnchants <= 0 || itemEnchants < maxEnchants && maxEnchants > 0) {
									if (!(enchant.canAnvilItem(new ItemData(itemToEnchant)) || enchant.canEnchantItem(new ItemData(itemToEnchant)))) {
										HashMap<String, Object> codes = ChatUtils.getCodes();
										codes.put("%enchant%", enchant.getDisplayName());
										ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.cannot-enchant-item"), Level.WARNING);
										return true;
									}
								} else {
									HashMap<String, Object> codes = ChatUtils.getCodes();
									codes.put("%maxEnchants%", maxEnchants);
									ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.too-many-enchants"), Level.WARNING);
									return true;
								}
							}
							boolean useBooks = ConfigString.USE_ENCHANTED_BOOKS.getBoolean();
							if (itemToEnchant.getType() == Material.BOOK && useBooks) itemToEnchant = ItemUtils.convertToEnchantedBook(itemToEnchant);
							else if (itemToEnchant.getType() == Material.ENCHANTED_BOOK && !useBooks) itemToEnchant = ItemUtils.convertToRegularBook(itemToEnchant);
							itemToEnchant = ItemUtils.addEnchantmentToItem(itemToEnchant, enchant, level);

							givePlayer.getInventory().setItem(slot, itemToEnchant);
							MiscUtils.updateAbilities(givePlayer);
							HashMap<String, Object> codes = ChatUtils.getCodes();
							codes.put("%level%", level);
							codes.put("%slot%", slot);
							codes.put("%enchant%", enchant.getDisplayName());
							if (player != null) codes.put("%player%", player.getDisplayName());
							else
								codes.put("%player%", ChatUtils.getMessage(codes, "commands.console-player"));
							codes.put("%give_player%", givePlayer.getName());
							if (givePlayer.equals(player)) ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.add-enchant"), Level.INFO);
							else {
								ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.add-enchant-other"), Level.INFO);
								ChatUtils.sendMessage(sender, givePlayer, ChatUtils.getMessage(codes, "commands.other-added-enchant"), Level.INFO);
							}
							return true;
						} else
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.enchant-fail"), Level.WARNING);
						return false;
					}
				HashMap<String, Object> codes = ChatUtils.getCodes();
				codes.put("%enchant%", enchantmentName);
				ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.enchant-not-found"), Level.WARNING);
				return true;
			} else
				ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.enchant-not-specified"), Level.WARNING);
		} else
			ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.no-permission"), Level.WARNING);
		return false;
	}

	public static boolean removeEnchant(CommandSender sender, ESCommand details, String[] args) {
		Player player = null;
		Player removePlayer = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			removePlayer = player;
		}
		if (sender.hasPermission(details.getPermission())) {
			if (args.length > 1) {
				String enchantmentName = args[1];
				boolean all = false;
				boolean includeCurse = false;
				for(CustomEnchantment enchant: RegisterEnchantments.getRegisteredEnchantments()) {
					if (enchantmentName.equals("All")) all = true;
					if (all || enchant.getName().equalsIgnoreCase(enchantmentName)) {
						if (args.length > 2) {
							String arg = args[2];
							if (all) includeCurse = Boolean.valueOf(arg);
							else if (!arg.equals("@p")) {
								removePlayer = Bukkit.getPlayer(arg);
								if (removePlayer == null) {
									HashMap<String, Object> codes = ChatUtils.getCodes();
									codes.put("%invalid_player%", args[2]);
									ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-player"), Level.WARNING);
									return true;
								} else if (removePlayer != null && !removePlayer.equals(sender) && !sender.hasPermission("enchantmentsolution.command.enchantremove.others")) {
									ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.no-permission-other"), Level.WARNING);
									return true;
								}
							}
						}
						if (removePlayer == null) {
							HashMap<String, Object> codes = ChatUtils.getCodes();
							codes.put("%player%", ChatUtils.getMessage(codes, "commands.invalid.console"));
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-player"), Level.WARNING);
							return true;
						}
						int slot = removePlayer.getInventory().getHeldItemSlot();
						if (args.length > 3) try {
							slot = Integer.parseInt(args[3]);
							if (slot < 0) {
								HashMap<String, Object> codes = ChatUtils.getCodes();
								codes.put("%slot%", slot);
								ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.slot-too-low"), Level.WARNING);
								slot = removePlayer.getInventory().getHeldItemSlot();
							} else if (slot >= 45) {
								HashMap<String, Object> codes = ChatUtils.getCodes();
								codes.put("%slot%", slot);
								ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.slot-too-high"), Level.WARNING);
								slot = removePlayer.getInventory().getHeldItemSlot();
							}
						} catch (NumberFormatException ex) {
							HashMap<String, Object> codes = ChatUtils.getCodes();
							codes.put("%slot%", args[3]);
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-slot"), Level.WARNING);
							slot = removePlayer.getInventory().getHeldItemSlot();
						}
						ItemStack itemToEnchant = removePlayer.getInventory().getItem(slot);
						if (itemToEnchant != null) {
							boolean useBooks = ConfigString.USE_ENCHANTED_BOOKS.getBoolean();
							if (itemToEnchant.getType() == Material.BOOK && useBooks) itemToEnchant = ItemUtils.convertToEnchantedBook(itemToEnchant);
							else if (itemToEnchant.getType() == Material.ENCHANTED_BOOK && !useBooks) itemToEnchant = ItemUtils.convertToRegularBook(itemToEnchant);
							HashMap<String, Object> codes = ChatUtils.getCodes();
							if (all) {
								itemToEnchant = ItemUtils.removeAllEnchantments(itemToEnchant, includeCurse);
								codes.put("%enchant%", "All");
							} else {
								itemToEnchant = ItemUtils.removeEnchantmentFromItem(itemToEnchant, enchant);
								codes.put("%enchant%", enchant.getDisplayName());
							}
							if (itemToEnchant.getType() == Material.ENCHANTED_BOOK && !((EnchantmentStorageMeta) itemToEnchant.getItemMeta()).hasStoredEnchants()) itemToEnchant.setType(Material.BOOK);

							removePlayer.getInventory().setItem(slot, itemToEnchant);
							codes.put("%player%", sender.getName());
							if (player != null) codes.put("%player%", player.getDisplayName());
							else
								codes.put("%player%", ChatUtils.getMessage(codes, "commands.console-player"));
							codes.put("%slot%", slot);
							codes.put("%remove_player%", removePlayer.getName());
							if (removePlayer.equals(player)) ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.enchant-removed"), Level.INFO);
							else {
								ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.enchant-removed-other"), Level.INFO);
								ChatUtils.sendMessage(sender, removePlayer, ChatUtils.getMessage(codes, "commands.other-removed-enchant"), Level.INFO);
							}
						} else
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.enchant-remove-from-item"), Level.WARNING);
						return true;
					}
				}
				HashMap<String, Object> codes = ChatUtils.getCodes();
				codes.put("%enchant%", enchantmentName);
				ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.enchant-not-found"), Level.WARNING);
				return true;
			} else
				ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.enchant-not-specified"), Level.WARNING);
		} else
			ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.no-permission"), Level.WARNING);
		return false;
	}

	public static boolean book(CommandSender sender, ESCommand details, String[] args) {
		Player player = null;
		if (sender instanceof Player) player = (Player) sender;
		if (!sender.hasPermission(details.getPermission())) {
			ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.no-permission"), Level.WARNING);
			return false;
		}
		List<EnchantmentLevel> levels = new ArrayList<EnchantmentLevel>();
		Player givePlayer = player;

		if (args.length > 1) {
			String arg = args[1];
			if (!arg.equals("@p")) {
				givePlayer = Bukkit.getPlayer(arg);
				if (givePlayer == null) {
					HashMap<String, Object> codes = ChatUtils.getCodes();
					codes.put("%player%", arg);
					ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-player"), Level.WARNING);
					return true;
				} else if (givePlayer != null && !givePlayer.equals(player) && !sender.hasPermission("enchantmentsolution.command.book.others")) {
					ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.no-permission-other"), Level.WARNING);
					return true;
				}
			} else if (player == null) {
				HashMap<String, Object> codes = ChatUtils.getCodes();
				codes.put("%player%", ChatUtils.getMessage(codes, "commands.invalid.console"));
				ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-player"), Level.WARNING);
				return false;
			}
		} else {
			HashMap<String, Object> codes = ChatUtils.getCodes();
			codes.put("%player%", ChatUtils.getMessage(codes, "commands.invalid.null-player"));
			ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-player"), Level.WARNING);
			return false;
		}

		if (args.length > 2) {
			String arg = args[2];
			if (arg.equals("RandomEnchant") || arg.equals("RandomMultiEnchant")) {
				boolean ignoreLimits = false;
				if (args.length > 3) ignoreLimits = Boolean.valueOf(args[3]);
				levels = GenerateUtils.generateBookLoot(givePlayer, new ItemStack(Material.BOOK), ignoreLimits ? EnchantmentLocation.NONE : EnchantmentLocation.CHEST_LOOT);
				if (arg.equals("RandomEnchant")) for(int i = levels.size() - 1; i > 0; i--)
					levels.remove(i);
			} else
				for(CustomEnchantment enchant: RegisterEnchantments.getRegisteredEnchantments())
					if (enchant.getName().equalsIgnoreCase(args[2])) {
						if (!enchant.isEnabled()) {
							HashMap<String, Object> codes = ChatUtils.getCodes();
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.enchant-disabled"), Level.WARNING);
							return true;
						}
						int level = 1;
						if (args.length > 3) try {
							level = Integer.parseInt(args[3]);
							if (level < 1) {
								HashMap<String, Object> codes = ChatUtils.getCodes();
								codes.put("%level%", level);
								ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.level-too-low"), Level.WARNING);
								level = 1;
							}
						} catch (NumberFormatException ex) {
							HashMap<String, Object> codes = ChatUtils.getCodes();
							codes.put("%level%", args[3]);
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-level"), Level.WARNING);
						}
						if (level > enchant.getMaxLevel()) {
							HashMap<String, Object> codes = ChatUtils.getCodes();
							codes.put("%level%", level);
							codes.put("%maxLevel%", enchant.getMaxLevel());
							codes.put("%enchant%", enchant.getDisplayName());
							ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.level-too-high"), Level.WARNING);
							level = enchant.getMaxLevel();
						}
						levels.add(new EnchantmentLevel(enchant, level));
						break;
					}
			if (levels.size() == 0) {
				HashMap<String, Object> codes = ChatUtils.getCodes();
				codes.put("%enchant%", arg);
				ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.enchant-not-found"), Level.WARNING);
				return true;
			}
		} else {
			ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.enchant-not-specified"), Level.WARNING);
			return false;
		}

		int amount = 1;
		if (args.length > 4) try {
			amount = Integer.parseInt(args[4]);
		} catch (NumberFormatException ex) {
			HashMap<String, Object> codes = ChatUtils.getCodes();
			codes.put("%amount%", args[4]);
			ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.invalid-amount"), Level.WARNING);
		}

		boolean useBooks = ConfigString.USE_ENCHANTED_BOOKS.getBoolean();
		ItemStack book = new ItemStack(Material.BOOK);
		if (useBooks) book = new ItemStack(Material.ENCHANTED_BOOK);
		book = ItemUtils.addEnchantmentsToItem(book, levels);

		for(int i = 0; i < amount; i++)
			ItemUtils.giveItemToPlayer(givePlayer, book, givePlayer.getLocation(), false);

		HashMap<String, Object> codes = ChatUtils.getCodes();
		if (player != null) codes.put("%player%", player.getDisplayName());
		else
			codes.put("%player%", ChatUtils.getMessage(codes, "commands.console-player"));
		codes.put("%give_player%", givePlayer.getDisplayName());

		if (amount == 1) {
			if (sender.equals(givePlayer)) ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.give-book"), Level.INFO);
			else {
				ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.give-book-other"), Level.INFO);
				ChatUtils.sendMessage(sender, givePlayer, ChatUtils.getMessage(codes, "commands.receive-book"), Level.INFO);
			}
			return true;
		} else {
			codes.put("%amount%", amount);
			if (sender.equals(givePlayer)) ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.give-book-multiple"), Level.INFO);
			else {
				ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.give-book-other-multiple"), Level.INFO);
				ChatUtils.sendMessage(sender, givePlayer, ChatUtils.getMessage(codes, "commands.receive-book-multiple"), Level.INFO);
			}
			return true;
		}
	}

	public static boolean enchantInfo(CommandSender sender, ESCommand details, String[] args) {
		Player player = null;
		if (sender instanceof Player) player = (Player) sender;
		if (!sender.hasPermission(details.getPermission())) {
			ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(ChatUtils.getCodes(), "commands.no-permission"), Level.WARNING);
			return true;
		}
		if (args.length == 1) sendEnchantInfo(sender, 1);
		else {
			try {
				int page = Integer.parseInt(args[1]);
				sendEnchantInfo(sender, page);
				return true;
			} catch (NumberFormatException e) {

			}
			CustomEnchantment enchantment = null;
			for(CustomEnchantment enchant: RegisterEnchantments.getRegisteredEnchantments())
				if (enchant.getName().equalsIgnoreCase(args[1])) {
					enchantment = enchant;
					break;
				}
			if (enchantment == null) {
				HashMap<String, Object> codes = ChatUtils.getCodes();
				codes.put("%enchant%", args[1]);
				ChatUtils.sendMessage(sender, player, ChatUtils.getMessage(codes, "commands.enchant-not-found"), Level.WARNING);
			} else {
				String page = enchantment.getDetails();
				ChatUtils.sendMessage(sender, player, page, Level.INFO);
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private static void sendEnchantInfo(CommandSender sender, int page) {
		Player player = null;
		if (sender instanceof Player) player = (Player) sender;
		if (page < 1) page = 1;
		List<CustomEnchantment> registered = RegisterEnchantments.getRegisteredEnchantments();
		while (registered.size() < (page - 1) * 10) {
			if (page == 1) break;
			page -= 1;
		}
		if (player != null) {
			JSONArray json = new JSONArray();
			JSONObject first = new JSONObject();
			first.put("text", "\n" + ChatColor.DARK_BLUE + "******");
			JSONObject second = new JSONObject();
			if (page > 1) {
				second.put("text", ChatColor.GREEN + "<<<");
				HashMap<Object, Object> action = new HashMap<Object, Object>();
				action.put("action", "run_command");
				action.put("value", "/enchantinfo " + (page - 1));
				second.put("clickEvent", action);
			} else
				second.put("text", ChatColor.DARK_BLUE + "***");
			JSONObject third = new JSONObject();
			third.put("text", ChatColor.DARK_BLUE + "******" + ChatColor.AQUA + " Enchantments Page " + page + ChatColor.DARK_BLUE + " ******");
			JSONObject fourth = new JSONObject();
			if (registered.size() > page * 10) {
				fourth.put("text", ChatColor.GREEN + ">>>");
				HashMap<Object, Object> action = new HashMap<Object, Object>();
				action.put("action", "run_command");
				action.put("value", "/enchantinfo " + (page + 1));
				fourth.put("clickEvent", action);
			} else
				fourth.put("text", ChatColor.DARK_BLUE + "***");
			JSONObject fifth = new JSONObject();
			fifth.put("text", ChatColor.DARK_BLUE + "******" + "\n");
			json.add(first);
			json.add(second);
			json.add(third);
			json.add(fourth);
			json.add(fifth);
			List<CustomEnchantment> alphabetical = RegisterEnchantments.getRegisteredEnchantmentsAlphabetical();
			for(int i = 0; i < 10; i++) {
				int num = i + (page - 1) * 10;
				if (num >= registered.size()) break;
				CustomEnchantment enchant = alphabetical.get(num);
				JSONObject name = new JSONObject();
				JSONObject desc = new JSONObject();
				JSONObject action = new JSONObject();
				action.put("action", "run_command");
				action.put("value", "/enchantinfo " + enchant.getName());
				name.put("text", shrink(ChatColor.GOLD + enchant.getDisplayName()));
				name.put("clickEvent", action);
				json.add(name);
				desc.put("text", shrink(ChatColor.GOLD + enchant.getDisplayName() + ChatColor.WHITE + ": " + ChatColor.WHITE + enchant.getDescription()).substring((ChatColor.GOLD + enchant.getDisplayName()).length()) + "\n");
				json.add(desc);
			}
			json.add(first);
			json.add(second);
			json.add(third);
			json.add(fourth);
			json.add(fifth);
			ChatUtils.sendRawMessage(player, json.toJSONString());
		} else {
			String message = "\n" + ChatColor.DARK_BLUE + "******" + (page > 1 ? "<<<" : "***") + "******" + ChatColor.AQUA + " Enchantments Page " + page + ChatColor.DARK_BLUE + " ******" + (registered.size() < (page - 1) * 10 ? ">>>" : "***") + "******" + "\n";
			for(int i = 0; i < 10; i++) {
				int num = i + (page - 1) * 10;
				if (num >= registered.size()) break;
				CustomEnchantment enchant = registered.get(num);
				message += shrink(ChatColor.GOLD + enchant.getDisplayName() + ": " + ChatColor.WHITE + enchant.getDescription()) + "\n";
			}
			message += "\n" + ChatColor.DARK_BLUE + "******" + (page > 1 ? "<<<" : "***") + "******" + ChatColor.AQUA + " Enchantments Page " + page + ChatColor.DARK_BLUE + " ******" + (registered.size() < (page - 1) * 10 ? ">>>" : "***") + "******" + "\n";
			ChatUtils.sendToConsole(Level.INFO, message);
		}
	}

	private static String shrink(String s) {
		if (s.length() > 60) return s.substring(0, 58) + "...";
		return s;
	}
}