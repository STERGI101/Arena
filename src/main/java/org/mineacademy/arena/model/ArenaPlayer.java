package org.mineacademy.arena.model;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.remain.CompAttribute;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.remain.Remain;
import org.mineacademy.fo.settings.YamlSectionConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The cache for players in the plugin
 */
public final class ArenaPlayer extends YamlSectionConfig {

	/**
	 * The map holding all loading player caches
	 */
	private static final Map<UUID, ArenaPlayer> cacheMap = new HashMap<>();

	/**
	 * The unique ID of this player
	 */
	@Getter
	private final UUID id;

	/**
	 * The arena player is currently in
	 * <p>
	 * Null if not joined any arena
	 */
	private String arena;

	/**
	 * The mode players is currently in
	 * <p>
	 * Null if not joined any arena
	 */
	@Getter
	private ArenaJoinMode mode;

	/**
	 * The location before the player joined an arena
	 */
	@Getter
	private Location joinLocation;

	/**
	 * How many times the player has died and respawned. Used for the lives system
	 * 1 by default
	 */
	@Getter
	private int respawns;

	/**
	 * The inventory saved snapshot
	 */
	private SerializedMap savedSnapshot;

	/**
	 * The arena class for player
	 */
	@Getter
	@Setter
	private ArenaClass arenaClass;

	/**
	 * The team the player has in his arena
	 */
	@Getter
	@Setter
	private ArenaTeam arenaTeam;

	/**
	 * The arena points gained for each arena and cleared after the game ends
	 */
	@Getter
	private double arenaPoints;

	/**
	 * Is the player leaving an arena right now?
	 */
	@Getter
	@Setter
	private boolean leavingArena;

	/**
	 * Is the player leaving the server? Stored here because the {@link PlayerQuitEvent}
	 * returns true for the isOnline check so we need to mark the player for leave in our own way
	 */
	@Getter
	@Setter
	private boolean leavingServer;

	/**
	 * Has the player been rewarded yet?
	 */
	@Getter
	private boolean rewarded;

	/**
	 * When players enter spectate mode, we will stop counting how
	 * many waves they survived so that they wont receive more reward
	 * points for waves they just spectated, but not played
	 */
	@Getter
	private int playedToWave;

	// --------------------------------------------------------------------------------------------------------------
	// Fields saved to data.db file
	// --------------------------------------------------------------------------------------------------------------

	/**
	 * The points gained in arenas (total)
	 */
	@Getter
	private double totalPoints = 0;

	/**
	 * Stored class tiers by class name and tier level player obtained
	 */
	private SerializedMap classTiers = new SerializedMap();

	/**
	 * Create a new player cache
	 */
	private ArenaPlayer(final UUID id) {
		super("Players." + id.toString());

		this.id = id;

		loadConfiguration(NO_DEFAULT, "data.db");
	}

	/**
	 * Load his data from data.db
	 */
	@Override
	protected void onLoadFinish() {
		this.totalPoints = getDouble("Points", 0D);
		this.classTiers = getMap("Class_Tiers");
	}

	/**
	 * Load the data from MySQL, or preserve the ones from file if MySQL does not have them
	 * But if database has them, override the ones from our data.db file
	 *
	 * @param map
	 */
	public void loadFromMySQL(final SerializedMap map) {
		this.totalPoints = map.getDouble("Points", this.totalPoints);
		this.classTiers = map.containsKey("Class_Tiers") ? map.getMap("Class_Tiers") : this.classTiers;

		save();
	}

	@Override
	public void save() {
		final SerializedMap map = serialize();

		for (final Map.Entry<String, Object> entry : map.entrySet())
			setNoSave(entry.getKey(), entry.getValue());

		super.save();
	}

	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"Points", Double.parseDouble(totalPoints + "000" + String.valueOf(Math.random() + Math.random()).replace(".", "")),
				"Class_Tiers", classTiers);
	}

	// --------------------------------------------------------------------------------------------------------------
	// Data saved to data.db file
	// --------------------------------------------------------------------------------------------------------------

	/**
	 * Give and show points to the player
	 *
	 * @param points the points to set
	 */
	public void giveArenaPoints(final Player player, double points) {
		Valid.checkBoolean(points >= 0, "Points must not be negative");

		if (points == 0)
			return;

		points = formatPoints(points);

		arenaPoints += points;
		arenaPoints = formatPoints(arenaPoints);

		// We divide the given points into the whole part and the fraction part
		// Such as giving 5.25 points will give 5 levels and 0.25 of experience
		final String[] number = String.valueOf(arenaPoints).split("\\.");
		final int wholeNumber = Integer.parseInt(number[0]);
		final double rest = Double.parseDouble("0." + number[1]);

		player.setLevel(wholeNumber);
		player.setExp((float) rest);

		CompSound.LEVEL_UP.play(player);

		save();
	}

	/*
	 * Converts arena points to total points and removes arena points
	 */
	public void convertArenaPoints() {
		totalPoints += arenaPoints;
		totalPoints = formatPoints(totalPoints);

		arenaPoints = 0;

		save();
	}

	/*
	 * Formats the given points to have two decimal places only
	 */
	private double formatPoints(final double points) {
		return MathUtil.formatTwoDigitsD(points);
	}

	/**
	 * Set total points that are saved in the file (NOT arena points - arena points
	 * are only given when playing in arena)
	 *
	 * @param totalPoints the totalPoints to set
	 */
	public void setTotalPoints(final double totalPoints) {
		this.totalPoints = totalPoints;

		save();
	}

	/**
	 * Get the tier for this player from a class
	 *
	 * @param arenaClass
	 * @return
	 */
	public int getTier(final ArenaClass arenaClass) {
		return classTiers.getInteger(arenaClass.getName(), 1);
	}

	/**
	 * Save a tier for the given class to the data file
	 *
	 * @param arenaClass
	 * @param tier
	 */
	public void saveTier(final ArenaClass arenaClass, final int tier) {
		classTiers.override(arenaClass.getName(), tier);

		save();
	}

	// --------------------------------------------------------------------------------------------------------------
	// Data stored locally trashed on each reload
	// --------------------------------------------------------------------------------------------------------------

	/**
	 * Get the joined arena or null if none
	 *
	 * @return the arena
	 * @throws FoException if the player is not in an arena. Use {@link ArenaManager#findArena(Player)} to avoid throwing errors
	 */
	public Arena getArena() throws FoException {
		Valid.checkBoolean(hasArena(), "Player " + getPlayer().getName() + " has no associated arena!");

		return ArenaManager.findArena(arena);
	}

	/**
	 * Called when the player joins an arena
	 *
	 * @param player
	 * @param arena
	 * @param joinMode
	 */
	public void markArenaJoin(final Player player, @NonNull final Arena arena, final ArenaJoinMode joinMode) {
		Valid.checkBoolean(!hasArena(), "Player " + getPlayer().getName() + " already has an arena: " + this.arena);

		this.arena = arena.getName();
		this.mode = joinMode;
		this.joinLocation = player.getLocation();
		this.respawns = 0;
		this.arenaPoints = 0;
		this.arenaClass = null;
		this.arenaTeam = null;
		this.rewarded = false;
		this.playedToWave = 0;
		this.leavingArena = false;
	}

	/**
	 * Called when the player leaves their arena
	 */
	public void markArenaLeft() {
		Valid.checkBoolean(hasArena(), "Player " + getPlayer().getName() + " does not have any arena!");

		this.arena = null;
		this.mode = null;
		this.joinLocation = null;
		this.respawns = 0;
		this.arenaPoints = 0;
		this.arenaClass = null;
		this.arenaTeam = null;
		this.rewarded = false;
		this.playedToWave = 0;
		this.leavingArena = false;
	}

	/**
	 * Called when the player starts to spectate the arena
	 */
	public void markSpectate() {
		Valid.checkBoolean(hasArena(), "Player " + getPlayer().getName() + " does not have any arena!");

		this.mode = ArenaJoinMode.SPECTATING;
		this.respawns = 0;
		this.arenaClass = null;
		this.arenaTeam = null;
	}

	/**
	 * Mark the player as being rewarded for playing in the arena
	 */
	public void markRewarded() {
		Valid.checkBoolean(hasArena(), "Player " + getPlayer().getName() + " lacks an arena to be rewarded in!");
		Valid.checkBoolean(!rewarded, "Player " + getPlayer().getName() + " was already rewarded!");

		this.rewarded = true;
	}

	/**
	 * Get if the player has an active associated arena
	 *
	 * @return true if the player has associated arena
	 */
	public boolean hasArena() {
		return arena != null;
	}

	/**
	 * Set the players snapshot to be stored locally in the cache
	 *
	 * @param player
	 */
	public void storeSnapshot(final Player player) {
		Valid.checkBoolean(!hasStoredSnapshot(), "Player " + player.getName() + " already has stored inventory!");

		final SerializedMap snapshot = SerializedMap.ofArray(
				"gameMode", player.getGameMode(),
				"content", player.getInventory().getContents(),
				"armorContent", player.getInventory().getArmorContents(),
				"healthScaled", player.isHealthScaled(),
				"remainingAir", player.getRemainingAir(),
				"maximumAir", player.getMaximumAir(),
				"fallDistance", player.getFallDistance(),
				"fireTicks", player.getFireTicks(),
				"totalExp", player.getTotalExperience(),
				"level", player.getLevel(),
				"exp", player.getExp(),
				"foodLevel", player.getFoodLevel(),
				"exhaustion", player.getExhaustion(),
				"saturation", player.getSaturation(),
				"flySpeed", player.getFlySpeed(),
				"walkSpeed", player.getWalkSpeed(),
				"potionEffects", player.getActivePotionEffects(),
				"health", Remain.getHealth(player));

		for (final CompAttribute attribute : CompAttribute.values()) {
			final Double value = attribute.get(player);

			snapshot.putIfExist(attribute.toString(), value);
		}

		// From now on we have to surround each method with try-catch since
		// those are not available in older MC versions

		try {
			snapshot.put("extraContent", player.getInventory().getExtraContents());
		} catch (final Throwable t) {
		}

		try {
			snapshot.put("invulnerable", player.isInvulnerable());
		} catch (final Throwable t) {
		}

		try {
			snapshot.put("silent", player.isSilent());
		} catch (final Throwable t) {
		}

		try {
			snapshot.put("glowing", player.isGlowing());
		} catch (final Throwable t) {
		}

		savedSnapshot = snapshot;
	}

	/**
	 * Restores the player inventory and properties
	 *
	 * @param player
	 */
	public void restoreSnapshot(final Player player) {
		Valid.checkBoolean(hasStoredSnapshot(), "Player " + player.getName() + " does not have stored inventory!");

		final SerializedMap snap = savedSnapshot;

		player.setGameMode(snap.get("gameMode", GameMode.class));
		player.getInventory().setContents((ItemStack[]) snap.getObject("content"));
		player.getInventory().setArmorContents((ItemStack[]) snap.getObject("armorContent"));
		player.setHealthScaled(snap.getBoolean("healthScaled"));
		player.setRemainingAir(snap.getInteger("remainingAir"));
		player.setMaximumAir(snap.getInteger("maximumAir"));
		player.setFallDistance(snap.getFloat("fallDistance"));
		player.setFireTicks(snap.getInteger("fireTicks"));
		player.setTotalExperience(snap.getInteger("totalExp"));
		player.setLevel(snap.getInteger("level"));
		player.setExp(snap.getFloat("exp"));
		player.setFoodLevel(snap.getInteger("foodLevel"));
		player.setExhaustion(snap.getFloat("exhaustion"));
		player.setSaturation(snap.getFloat("saturation"));
		player.setFlySpeed(snap.getFloat("flySpeed"));
		player.setWalkSpeed(snap.getFloat("walkSpeed"));
		player.setHealth(snap.getInteger("health"));

		for (final PotionEffect effect : player.getActivePotionEffects())
			player.removePotionEffect(effect.getType());

		for (final PotionEffect effect : snap.getList("potionEffects", PotionEffect.class))
			player.addPotionEffect(effect);

		for (final CompAttribute attribute : CompAttribute.values()) {
			final Double value = snap.getDouble(attribute.toString());

			if (value != null)
				attribute.set(player, value);
		}

		// From now on we have to surround each method with try-catch since
		// those are not available in older MC versions

		try {
			player.getInventory().setExtraContents((ItemStack[]) snap.getObject("extraContent"));
		} catch (final Throwable t) {
		}

		try {
			player.setInvulnerable(snap.getBoolean("invulnerable"));
		} catch (final Throwable t) {
		}

		try {
			player.setSilent(snap.getBoolean("silent"));
		} catch (final Throwable t) {
		}

		try {
			player.setGlowing(snap.getBoolean("glowing"));
		} catch (final Throwable t) {
		}

		savedSnapshot = null;
	}

	/**
	 * Return true if the player has a stored snapshot
	 *
	 * @return
	 */
	public boolean hasStoredSnapshot() {
		return savedSnapshot != null;
	}

	/**
	 * Increase player respawns
	 *
	 * respawns the respawns to set
	 */
	public void increaseRespawns() {
		respawns++;
	}

	/**
	 * Set the final wave before player went into spectate mode
	 *
	 * @param playedToWave the playedToWave to set
	 */
	public void setPlayedToWave(int playedToWave) {
		Valid.checkBoolean(hasArena(), "Player " + getPlayer().getName() + " lacks an arena to set his last wave to!");
		Valid.checkBoolean(this.playedToWave == 0, "Player " + getPlayer().getName() + " already has last wave = " + playedToWave);

		this.playedToWave = playedToWave;
	}

	// --------------------------------------------------------------------------------------------------------------
	// General getters
	// --------------------------------------------------------------------------------------------------------------

	/**
	 * Return the {@link Player} Bukkit instance
	 *
	 * @return
	 */
	public Player getPlayer() {
		final Player player = Remain.getPlayerByUUID(id);
		Valid.checkBoolean(player != null && player.isOnline(), "Player " + id + " has disconnected before we marked them as so!");

		return player;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj instanceof ArenaPlayer && ((ArenaPlayer) obj).id.equals(this.id);
	}

	// --------------------------------------------------------------------------------------------------------------
	// Static methods below
	// --------------------------------------------------------------------------------------------------------------

	/**
	 * Get the arena player by instance
	 *
	 * @param player
	 * @return
	 */
	public static ArenaPlayer getCache(final Player player) {
		return getCache(player.getUniqueId());
	}

	/**
	 * Get the arena player by uuid
	 *
	 * @param uuid
	 * @return
	 */
	private static ArenaPlayer getCache(final UUID uuid) {
		ArenaPlayer cache = cacheMap.get(uuid);

		if (cache == null) {
			cache = new ArenaPlayer(uuid);

			cacheMap.put(uuid, cache);
		}

		return cache;
	}

	/**
	 * Remove data for the given player
	 *
	 * @param player
	 */
	public static void clearDataFor(Player player) {
		cacheMap.remove(player.getUniqueId());
	}

	/**
	 * Remove all stored caches
	 */
	public static void clearAllData() {
		cacheMap.clear();
	}
}
