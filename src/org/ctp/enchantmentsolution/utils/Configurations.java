package org.ctp.enchantmentsolution.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.ctp.enchantmentsolution.EnchantmentSolution;
import org.ctp.enchantmentsolution.enchantments.RegisterEnchantments;
import org.ctp.enchantmentsolution.enchantments.generate.TableEnchantments;
import org.ctp.enchantmentsolution.enums.Language;
import org.ctp.enchantmentsolution.utils.config.*;
import org.ctp.enchantmentsolution.utils.files.DataFile;
import org.ctp.enchantmentsolution.utils.files.LanguageFile;
import org.ctp.enchantmentsolution.utils.yaml.YamlConfig;
import org.ctp.enchantmentsolution.utils.yaml.YamlConfigBackup;

public class Configurations {

	private static MainConfiguration CONFIG;
	private static FishingConfiguration FISHING;
	private static LanguageConfiguration LANGUAGE;
	private static EnchantmentsConfiguration ENCHANTMENTS;
	private static AdvancementsConfiguration ADVANCEMENTS;
	private static RPGConfiguration RPG;
	private static MinigameConfiguration MINIGAME;
	private static HardModeConfiguration HARD_MODE;

	private static List<LanguageFile> LANGUAGE_FILES = new ArrayList<LanguageFile>();
	private static DataFile DATA_FILE;

	private Configurations() {

	}

	public static void onEnable() {
		File dataFolder = EnchantmentSolution.getPlugin().getDataFolder();

		try {
			if (!dataFolder.exists()) dataFolder.mkdirs();
		} catch (final Exception e) {
			e.printStackTrace();
		}

		CONFIG = new MainConfiguration(dataFolder);
		FISHING = new FishingConfiguration(dataFolder);
		ENCHANTMENTS = new EnchantmentsConfiguration(dataFolder);
		ADVANCEMENTS = new AdvancementsConfiguration(dataFolder);
		RPG = new RPGConfiguration(dataFolder);
		MINIGAME = new MinigameConfiguration(dataFolder);
		HARD_MODE = new HardModeConfiguration(dataFolder);

		String languageFile = CONFIG.getString("language_file");
		Language lang = Language.getLanguage(CONFIG.getString("language"));
		if (!lang.getLocale().equals(CONFIG.getString("language"))) CONFIG.updatePath("language", lang.getLocale());

		File languages = new File(dataFolder + "/language");

		if (!languages.exists()) languages.mkdirs();

		LANGUAGE_FILES.add(new LanguageFile(dataFolder, Language.US));
		LANGUAGE_FILES.add(new LanguageFile(dataFolder, Language.GERMAN));
		LANGUAGE_FILES.add(new LanguageFile(dataFolder, Language.CHINA_SIMPLE));

		for(LanguageFile file: LANGUAGE_FILES)
			if (file.getLanguage() == lang) LANGUAGE = new LanguageConfiguration(dataFolder, languageFile, file);

		if (LANGUAGE == null) LANGUAGE = new LanguageConfiguration(dataFolder, languageFile, LANGUAGE_FILES.get(0));

		File extras = new File(dataFolder + "/extras");

		if (!extras.exists()) extras.mkdirs();

		DATA_FILE = new DataFile(dataFolder, "data.yml");

		save();
	}

	public static void revert() {
		CONFIG.revert();
		FISHING.revert();
		LANGUAGE.revert();
		ENCHANTMENTS.revert();
		ADVANCEMENTS.revert();
		RPG.revert();
		MINIGAME.revert();
		HARD_MODE.revert();
	}

	public static void revert(Configuration config, int backup) {
		config.revert(backup);
	}

	public static void save() {
		CONFIG.setComments(ConfigString.USE_COMMENTS.getBoolean());
		FISHING.setComments(ConfigString.USE_COMMENTS.getBoolean());
		LANGUAGE.setComments(ConfigString.USE_COMMENTS.getBoolean());
		ENCHANTMENTS.setComments(ConfigString.USE_COMMENTS.getBoolean());
		ADVANCEMENTS.setComments(ConfigString.USE_COMMENTS.getBoolean());
		RPG.setComments(ConfigString.USE_COMMENTS.getBoolean());
		MINIGAME.setComments(ConfigString.USE_COMMENTS.getBoolean());
		HARD_MODE.setComments(ConfigString.USE_COMMENTS.getBoolean());

		CONFIG.save();
		FISHING.save();
		LANGUAGE.save();
		ENCHANTMENTS.save();
		ADVANCEMENTS.save();
		RPG.save();
		MINIGAME.save();
		HARD_MODE.save();

		if (ConfigString.RESET_ON_RELOAD.getBoolean()) TableEnchantments.removeAllTableEnchantments();
		RegisterEnchantments.setEnchantments();

		if (!EnchantmentSolution.getPlugin().isInitializing()) {
			EnchantmentSolution.getPlugin().setVersionCheck(ConfigString.LATEST_VERSION.getBoolean(), ConfigString.EXPERIMENTAL_VERSION.getBoolean());
			AdvancementUtils.createAdvancements();
			EnchantmentSolution.getPlugin().getWiki().resetRunner();
		}
	}

	public static void generateDebug() {
		File dataFolder = EnchantmentSolution.getPlugin().getDataFolder();
		String[] header = { "Enchantment Solution", "Plugin by", "crashtheparty" };
		YamlConfig backup = new YamlConfig(new File(dataFolder + "/debug.yml"), header);

		SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z Z");
		backup.set("time", format.format(new Date()));
		backup.set("version.bukkit", EnchantmentSolution.getPlugin().getBukkitVersion().getVersion());
		backup.set("version.bukkit_num", EnchantmentSolution.getPlugin().getBukkitVersion().getVersionNumber());
		backup.set("version.plugin", EnchantmentSolution.getPlugin().getPluginVersion().getCurrent());
		backup.set("plugins.jobs_reborn", EnchantmentSolution.getPlugin().isJobsEnabled());
		backup.set("plugins.mcmmo", EnchantmentSolution.getPlugin().getMcMMOType());
		backup.set("plugins.mcmmo_version", EnchantmentSolution.getPlugin().getMcMMOVersion());
		backup.set("plugins.mmo_items", EnchantmentSolution.getPlugin().getMMOItems());
		backup.set("plugins.vein_miner", EnchantmentSolution.getPlugin().getVeinMiner());

		YamlConfigBackup config = CONFIG.getConfig();
		YamlConfigBackup fishing = FISHING.getConfig();
		YamlConfigBackup language = LANGUAGE.getConfig();
		YamlConfigBackup enchantments = ENCHANTMENTS.getConfig();
		YamlConfigBackup advancements = ADVANCEMENTS.getConfig();
		YamlConfigBackup rpg = RPG.getConfig();
		YamlConfigBackup minigame = MINIGAME.getConfig();
		YamlConfigBackup hardMode = HARD_MODE.getConfig();

		for(String s: config.getAllEntryKeys())
			if (config.contains(s)) backup.set("config." + s, config.get(s));

		for(String s: fishing.getAllEntryKeys())
			if (fishing.contains(s)) backup.set("fishing." + s, fishing.get(s));

		for(String s: language.getAllEntryKeys())
			if (language.contains(s)) backup.set("language." + s, language.get(s));

		for(String s: advancements.getAllEntryKeys())
			if (advancements.contains(s)) backup.set("advancements." + s, advancements.get(s));

		for(String s: enchantments.getAllEntryKeys())
			if (enchantments.contains(s)) backup.set("enchantment." + s, enchantments.get(s));

		for(String s: rpg.getAllEntryKeys())
			if (rpg.contains(s)) backup.set("rpg." + s, rpg.get(s));

		for(String s: minigame.getAllEntryKeys())
			if (minigame.contains(s)) backup.set("minigame." + s, rpg.get(s));

		for(String s: hardMode.getAllEntryKeys())
			if (hardMode.contains(s)) backup.set("hard_mode." + s, rpg.get(s));

		backup.saveConfig();
	}

	public static void reload() {
		CONFIG.reload();
		FISHING.reload();
		LANGUAGE.reload();
		ENCHANTMENTS.reload();
		ADVANCEMENTS.reload();
		RPG.reload();
		MINIGAME.reload();
		HARD_MODE.reload();

		save();
	}

	public static MainConfiguration getConfig() {
		return CONFIG;
	}

	public static FishingConfiguration getFishing() {
		return FISHING;
	}

	public static LanguageConfiguration getLanguage() {
		return LANGUAGE;
	}

	public static EnchantmentsConfiguration getEnchantments() {
		return ENCHANTMENTS;
	}

	public static AdvancementsConfiguration getAdvancements() {
		return ADVANCEMENTS;
	}

	public static RPGConfiguration getRPG() {
		return RPG;
	}

	public static List<LanguageFile> getLanguageFiles() {
		return LANGUAGE_FILES;
	}

	public static DataFile getDataFile() {
		return DATA_FILE;
	}

	public static MinigameConfiguration getMinigames() {
		return MINIGAME;
	}

	public static HardModeConfiguration getHardMode() {
		return HARD_MODE;
	}
}
