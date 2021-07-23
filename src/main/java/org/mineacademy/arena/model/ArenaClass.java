package org.mineacademy.arena.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.arena.util.Constants;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.YamlConfig;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents an arena class (a kit)
 */
public final class ArenaClass extends YamlConfig {

	/**
	 * The list of loaded classes
	 */
	private static volatile List<ArenaClass> loadedClasses = new ArrayList<>();

	/**
	 * The icon to display in the menu for this class
	 */
	private ItemStack icon;

	/**
	 * A list of applicable arenas where this class may be applied
	 */
	private List<String> applicableArenas;

	/**
	 * The permission to use this class
	 */
	@Getter
	private String permission;

	/**
	 * The tiers that players can upgrade into
	 */
	private List<ArenaClassTier> tiers;

	/**
	 * Creat a new arena class by the name
	 *
	 * @param name
	 */
	private ArenaClass(final String name) {

		loadConfiguration(NO_DEFAULT, "classes/" + name + ".yml");
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#onLoadFinish()
	 */
	@Override
	protected void onLoadFinish() {
		this.icon = get("Icon", ItemStack.class);
		this.tiers = getList("Tiers", ArenaClassTier.class, this);
		this.applicableArenas = getStringList("Applicable_Arenas");
		this.permission = getString("Permission");
	}

	/**
	 * Assign this class to the given player
	 *
	 * @param player
	 */
	public void assignTo(final Player player) {
		final ArenaPlayer cache = ArenaPlayer.getCache(player);
		Valid.checkBoolean(cache.hasArena() && cache.getMode() == ArenaJoinMode.PLAYING, "Classes may only be selected when playing an arena");

		final Arena arena = cache.getArena();
		Valid.checkBoolean(canAssign(player, arena), "Player " + player.getName() + " may not be assigned class " + getName());
		Valid.checkBoolean(arena.hasClasses(), "Arena " + arena.getName() + " does not support classes!");

		int tier = cache.getTier(this);
		ArenaClassTier classTier = getTier(tier);

		// Search for lower tiers if the one we want is not loaded/setup
		while (classTier == null && tier > 1) {
			tier--;

			classTier = getTier(tier);
		}

		Valid.checkNotNull(classTier, "Could not find tier of class " + getName() + " for player " + player.getName());

		classTier.applyFor(player, true);
		cache.setArenaClass(this);
	}

	/**
	 * Return if the player may get this class
	 *
	 * @param player
	 * @param arena
	 * @return
	 */
	public boolean canAssign(final Player player, final Arena arena) {
		if (!PlayerUtil.hasPerm(player, permission))
			return false;

		if (!applicableArenas.contains(arena.getName()) && !applicableArenas.isEmpty())
			return false;

		return true;
	}

	/**
	 * Set the class icon
	 *
	 * @param icon the icon to set
	 */
	public void setIcon(final ItemStack icon) {
		this.icon = icon;

		save();
	}

	/**
	 * Return the icon, or the default one if not set
	 *
	 * @return
	 */
	public ItemStack getIcon() {
		return icon != null && icon.getType() != Material.AIR ? icon : new ItemStack(CompMaterial.IRON_SWORD.getMaterial());
	}

	/**
	 * Adds or removes the given arena from applicable depending on if it was there previously
	 *
	 * @param arena
	 * @return true if the arena was added, false if removed
	 */
	public boolean toggleApplicableArena(final Arena arena) {
		final String arenaName = arena.getName();
		final boolean has = applicableArenas.contains(arenaName);

		if (has)
			applicableArenas.remove(arenaName);
		else
			applicableArenas.add(arenaName);

		save();
		return !has;
	}

	/**
	 * Add arena in which this class can be used
	 *
	 * @param arena
	 */
	public void addApplicableArena(final Arena arena) {
		Valid.checkBoolean(!applicableArenas.contains(arena.getName()), "Applicable arenas already contain " + arena);

		applicableArenas.add(arena.getName());
		save();
	}

	/**
	 * Remove arena in which this class can be used
	 *
	 * @param arena
	 */
	public void removeApplicableArena(final Arena arena) {
		Valid.checkBoolean(applicableArenas.contains(arena.getName()), "Applicable arenas do not contain " + arena);

		applicableArenas.remove(arena.getName());
		save();
	}

	/**
	 * Get the list of arenas this class can be used in
	 *
	 * @return the applicableArenas
	 */
	public List<String> getApplicableArenas() {
		return Collections.unmodifiableList(applicableArenas);
	}

	/**
	 * Set the permission to use this class, set to null to allow everyone
	 *
	 * @param permission the permission to set
	 */
	public void setPermission(final String permission) {
		this.permission = permission;

		save();
	}

	/**
	 * Return arena tier from the given tier number
	 *
	 * @param tier
	 * @return
	 */
	@Nullable
	public ArenaClassTier getTier(final int tier) {
		return hasTier(tier) ? tiers.get(tier - 1) : null;
	}

	/**
	 * Return if the given tier is contained within this class
	 *
	 * @param tier
	 * @return
	 */
	public boolean hasTier(final int tier) {
		return tiers.size() >= tier;
	}

	/**
	 * Add a new arena tier
	 *
	 * @return
	 */
	public ArenaClassTier addTier() {
		final ArenaClassTier tier = new ArenaClassTier(this);

		tiers.add(tier);
		save();

		return tiers.get(tiers.size() - 1);
	}

	/**
	 * Removes a tier from this arena
	 *
	 * @param tier the tier level
	 */
	public void removeTier(final int tier) {
		Valid.checkBoolean(getTiers() >= tier, "Cannot remove tier " + tier + " because the class " + getName() + " only has " + tiers.size() + " tiers");

		tiers.remove(tier - 1);
		save();
	}

	/**
	 * Return how many tiers this class have
	 *
	 * @return
	 */
	public int getTiers() {
		return tiers.size();
	}

	@Override
	public void save() {
		final SerializedMap map = SerializedMap.ofArray(
				"Applicable_Arenas", applicableArenas);
		// Enable null values
		map.asMap().put("Tiers", tiers);
		map.asMap().put("Icon", icon);
		map.asMap().put("Permission", permission);

		for (final Map.Entry<String, Object> entry : map.entrySet())
			setNoSave(entry.getKey(), entry.getValue());

		super.save();
	}

	@Override
	public boolean equals(final Object object) {
		return object instanceof ArenaClass && ((ArenaClass) object).getName().equals(this.getName());
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Load all classes in the plugin
	 */
	public static void loadClasses() {
		loadedClasses.clear();

		final File[] classFiles = FileUtil.getFiles("classes", "yml");

		for (final File classfile : classFiles) {
			final String name = FileUtil.getFileName(classfile);

			loadOrCreateClass(name);
		}
	}

	/**
	 * Load or creates a new class
	 *
	 * @param name
	 * @return
	 */
	public static ArenaClass loadOrCreateClass(final String name) {
		Valid.checkBoolean(!isClassLoaded(name), "Class " + name + " is already loaded: " + getClassNames());

		try {
			final ArenaClass arenaClass = new ArenaClass(name);
			loadedClasses.add(arenaClass);

			Common.log("[+] Loaded class " + arenaClass.getName());
			return arenaClass;

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to load class " + name);
		}

		return null;
	}

	/**
	 * Permanently delete a class
	 *
	 * @param arenaClass
	 */
	public static void removeClass(@NonNull final ArenaClass arenaClass) {
		Valid.checkBoolean(isClassLoaded(arenaClass.getName()), "Class " + arenaClass.getName() + " not loaded. Available: " + getClassNames());

		arenaClass.delete();

		loadedClasses.remove(arenaClass);
	}

	/**
	 * Return true if the given class exists
	 *
	 * @param name
	 * @return
	 */
	public static boolean isClassLoaded(final String name) {
		return findClass(name) != null;
	}

	/**
	 * Find a class by name
	 *
	 * @param name
	 * @return
	 */
	public static ArenaClass findClass(@NonNull final String name) {
		for (final ArenaClass arenaClass : loadedClasses)
			if (arenaClass.getName().equalsIgnoreCase(name))
				return arenaClass;

		return null;
	}

	/**
	 * Get all loaded classes
	 *
	 * @return
	 */
	public static List<ArenaClass> getClasses() {
		return Collections.unmodifiableList(loadedClasses);
	}

	/**
	 * Get all loaded class names
	 *
	 * @return
	 */
	public static List<String> getClassNames() {
		return Common.convert(loadedClasses, ArenaClass::getName);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Classes
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Represents the tier data class
	 */
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public final static class ArenaClassTier implements ConfigSerializable {

		/**
		 * The arena for which this tier is set
		 */
		private final ArenaClass arenaClass;

		/**
		 * Price to obtain this tier
		 */
		@Getter
		private double price;

		/**
		 * The inventory content of this tier
		 */
		@Getter
		private ItemStack[] content = new ItemStack[36];

		/**
		 * The armor content of this tier
		 */
		@Getter
		private ItemStack[] armorContent = new ItemStack[4];

		/**
		 * The potion effects of this tier name:amplifier
		 */
		private SerializedMap potionEffects = new SerializedMap();

		/**
		 * Set the price for this tier
		 *
		 * @param price the price to set
		 */
		public void setPrice(final double price) {
			this.price = price;

			arenaClass.save();
		}

		/**
		 * Set both the content and armor content
		 *
		 * @param content
		 * @param armor
		 */
		public void setAllContent(final ItemStack[] content, final ItemStack[] armor) {
			this.content = content;
			this.armorContent = armor;

			arenaClass.save();
		}

		/**
		 * Set inventory content
		 *
		 * @param content the content to set
		 */
		public void setContent(final ItemStack[] content) {
			this.content = content;

			arenaClass.save();
		}

		/**
		 * Set armor content
		 *
		 * @param armorContent the armorContent to set
		 */
		public void setArmorContent(final ItemStack[] armorContent) {
			this.armorContent = armorContent;

			arenaClass.save();
		}

		/**
		 * Get the potion effect or 0 if not set
		 *
		 * @param type
		 * @return
		 */
		public int getPotionEffect(final PotionEffectType type) {
			return potionEffects.getInteger(type.getName(), 0);
		}

		/**
		 * Return true if the tier is completely empty
		 *
		 * @return
		 */
		public boolean isEmpty() {
			return Valid.isNullOrEmpty(content) && Valid.isNullOrEmpty(armorContent) && potionEffects.isEmpty();
		}

		/**
		 * Set potion effects
		 *
		 * @param type the potionEffects to set
		 */
		public void setPotionEffect(final PotionEffectType type, final int level) {
			final String name = type.getName();

			if (level == 0 && potionEffects.containsKey(name))
				potionEffects.asMap().remove(name);
			else
				potionEffects.override(type.getName(), level);

			arenaClass.save();
		}

		/**
		 * Apply the tier for the given player
		 *
		 * @param player
		 * @param giveInventory
		 */
		public void applyFor(final Player player, boolean giveInventory) {
			if (giveInventory) {
				final PlayerInventory inventory = player.getInventory();

				inventory.setContents(content);
				inventory.setArmorContents(armorContent);
			}

			for (final Entry<String, Object> effect : potionEffects.asMap().entrySet())
				player.addPotionEffect(new PotionEffect(PotionEffectType.getByName(effect.getKey()), Integer.MAX_VALUE, (int) effect.getValue() - 1), true);
		}

		/**
		 * Create a new tier from saved config section
		 *
		 * @param map
		 * @param arenaClass
		 * @return
		 */
		public static ArenaClassTier deserialize(final SerializedMap map, final ArenaClass arenaClass) {
			final ArenaClassTier tier = new ArenaClassTier(arenaClass);

			final List<ItemStack> content = map.getList("Content", ItemStack.class);
			final List<ItemStack> armor = map.getList("Armor_Content", ItemStack.class);

			tier.price = map.getDouble("Price", Constants.Defaults.TIER_UPGRADE_PRICE);
			tier.content = content.toArray(new ItemStack[content.size()]);
			tier.armorContent = armor.toArray(new ItemStack[armor.size()]);

			tier.potionEffects = map.getMap("Potion_Effects");

			return tier;
		}

		/**
		 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
		 */
		@Override
		public SerializedMap serialize() {
			return SerializedMap.ofArray(
					"Price", price,
					"Content", content,
					"Armor_Content", armorContent,
					"Potion_Effects", potionEffects);
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Tier {content= " + Common.join(content, ", ", item -> item.getType().toString()) + "; armor=" + Common.join(armorContent, ", ", item -> item.getType().toString()) + "}";
		}
	}
}
