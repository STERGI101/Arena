package org.mineacademy.arena.model;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.YamlConfig;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * The arena settings
 */
@Getter
public class ArenaSettings extends YamlConfig {

	/**
	 * The arena these settings are related to
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private final Arena arena;

	/**
	 * The minimum amount of players to start the arena
	 */
	private int minPlayers;

	/**
	 * The maximum amount of players in the arena
	 */
	private int maxPlayers;

	/**
	 * The maximum amount of monsters/animals in the arena
	 */
	private int maxCreatures;

	/**
	 * How long is the game taking?
	 */
	private SimpleTime gameDuration;

	/**
	 * How long are players in lobby till the arena starts?
	 */
	private SimpleTime lobbyDuration;

	/**
	 * The arena region
	 */
	private VisualizedRegion region;

	/**
	 * The lobby location for this arena
	 */
	private Location lobbyLocation;

	/**
	 * Where to teleport players after the world is reset? Must
	 * be on a different world
	 */
	private Location resetLocation;

	/**
	 * How many times to respawn players before kicking them out
	 */
	private int lives;

	/**
	 * Can the arena map be destroyed?
	 */
	private boolean destructionEnabled;

	/**
	 * What blocks may be placed/broken if the destruction is enabled?
	 */
	private List<CompMaterial> destructionWhitelist;

	/**
	 * Should we save and restore the arena region after play?
	 * Also enables arena destruction
	 */
	private boolean mapResetEnabled;

	/**
	 * Shall we reset the whole arena world after play?
	 */
	private boolean worldResetEnabled;

	/**
	 * Create new arena settings
	 *
	 * @param arena
	 */
	public ArenaSettings(final Arena arena) {
		this.arena = arena;

		setHeader(Common.configLine(),
				" Welcome to the main configuration for " + arena.getName(),
				Common.configLine());

		loadConfiguration(NO_DEFAULT, "arenas/" + arena.getName() + ".yml");
	}

	/**
	 * Load and save all settings
	 *
	 * @see org.mineacademy.fo.settings.YamlConfig#onLoadFinish()
	 */
	@Override
	protected void onLoadFinish() {
		this.minPlayers = getInteger("Min_Players", 2);
		this.maxPlayers = getInteger("Max_Players", 20);
		this.maxCreatures = getInteger("Max_Creatures", 50);
		this.gameDuration = getTime("Game_Duration", "10 seconds");
		this.lobbyDuration = getTime("Lobby_Duration", "5 seconds");
		this.region = get("Region", VisualizedRegion.class);
		this.lobbyLocation = getLocation("Lobby_Location");
		this.resetLocation = getLocation("Reset_Location");
		this.lives = arena.hasLives() ? getInteger("Lives", 2) : -1;
		this.destructionEnabled = getBoolean("Destruction", false);
		this.destructionWhitelist = getList("Destruction_Whitelist", CompMaterial.class);
		this.mapResetEnabled = getBoolean("Map_Reset", false);
		this.worldResetEnabled = getBoolean("World_Reset", false);

		checkDestructionAndRestore();
	}

	@Override
	public void save() {
		final SerializedMap map = serialize();

		for (final Map.Entry<String, Object> entry : map.entrySet())
			setNoSave(entry.getKey(), entry.getValue());

		super.save();
	}

	/*
	 * Check if destruction and restore settings are correctly enabled
	 */
	private void checkDestructionAndRestore() {
		if (mapResetEnabled && (!HookManager.isWorldEditLoaded() || MinecraftVersion.olderThan(V.v1_13))) {
			Common.logFramed(
					"Map_Restore feature requires WorldEdit 7",
					"and Minecraft 1.13 or greater. Disabled",
					"Map_Restore for arena " + getFileName());

			setMapResetEnabled(false);
		}

		if (mapResetEnabled && worldResetEnabled)
			throw new FoException("Cannot have both map and world reset enabled, one is enough");
	}

	/**
	 * @param minPlayers the minPlayers to set
	 */
	public void setMinPlayers(final int minPlayers) {
		this.minPlayers = minPlayers;

		save();
	}

	/**
	 * Set the maximum amount of players this arena can have
	 *
	 * @param maxPlayers
	 */
	public final void setMaxPlayers(final int maxPlayers) {
		this.maxPlayers = maxPlayers;

		save();
	}

	/**
	 * Set the max amount of animals + monsters in the arena
	 *
	 * @param maxCreatures the maxCreatures to set
	 */
	public void setMaxCreatures(int maxCreatures) {
		this.maxCreatures = maxCreatures;

		save();
	}

	/**
	 * Set the game duration
	 *
	 * @param gameDuration the gameDuration to set
	 */
	public final void setGameDuration(final SimpleTime gameDuration) {
		this.gameDuration = gameDuration;

		save();
	}

	/**
	 * Set the lobby duration
	 *
	 * @param lobbyDuration the lobbyDuration to set
	 */
	public final void setLobbyDuration(final SimpleTime lobbyDuration) {
		this.lobbyDuration = lobbyDuration;

		save();
	}

	/**
	 * Sets or updates the region point
	 *
	 * @param primary
	 * @param secondary
	 */
	public final void setRegion(final Location primary, final Location secondary) {
		// Update region points
		if (this.region != null)
			this.region.updateLocationsWeak(primary, secondary);

		else
			this.region = new VisualizedRegion(primary, secondary);

		save();
	}

	/**
	 * Set the lobby location for this arena
	 *
	 * @param location
	 */
	public final void setLobbyLocation(final Location location) {
		this.lobbyLocation = location;

		save();
	}

	/**
	 * Set where to move players after world is reset
	 *
	 * @param resetLocation the resetLocation to set
	 */
	public void setResetLocation(Location resetLocation) {
		if (lobbyLocation != null)
			Valid.checkBoolean(!resetLocation.getWorld().getName().equals(lobbyLocation.getWorld().getName()), "Reset location must be on another world than the arena!");

		this.resetLocation = resetLocation;

		save();
	}

	/**
	 * Set arena lives (means respawns)
	 *
	 * @param lives the lives to set
	 */
	public void setLives(final int lives) {
		Valid.checkBoolean(arena.hasLives(), "Arena " + arena + " does not support lives system");

		this.lives = lives;
		save();
	}

	/**
	 * Set if the arena may be destroyed
	 *
	 * @param destructionEnabled the destructionEnabled to set
	 */
	public void setDestructionEnabled(boolean destructionEnabled) {
		this.destructionEnabled = destructionEnabled;

		save();
	}

	/**
	 * Set what blocks can players place/break in the arena
	 *
	 * @param destructionWhitelist the destructionWhitelist to set
	 */
	public void setDestructionWhitelist(List<CompMaterial> destructionWhitelist) {
		this.destructionWhitelist = destructionWhitelist;

		save();
	}

	/**
	 * Set if we should reset the arena map
	 *
	 * @param mapResetEnabled the mapResetEnabled to set
	 */
	public void setMapResetEnabled(boolean mapResetEnabled) {
		this.mapResetEnabled = mapResetEnabled;

		save();
	}

	/**
	 * Set if we should reset the arena world
	 *
	 * @param worldResetEnabled the worldResetEnabled to set
	 */
	public void setWorldResetEnabled(boolean worldResetEnabled) {
		this.worldResetEnabled = worldResetEnabled;

		save();
	}

	/**
	 * Return true if the settings are configured properly
	 *
	 * @return
	 */
	public boolean isSetup() {
		return region != null && region.isWhole() && lobbyLocation != null;
	}

	/**
	 * Set the arena type if it has not yet been set
	 *
	 * @param type
	 */
	protected final void setArenaType(final String type) {
		if (!isSet("Type"))
			save("Type", type);
	}

	/**
	 * Invoked automatically on {@link #save()}
	 *
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	public SerializedMap serialize() {
		return SerializedMap.ofArray(
				"Min_Players", minPlayers,
				"Max_Players", maxPlayers,
				"Max_Creatures", maxCreatures,
				"Game_Duration", gameDuration,
				"Lobby_Duration", lobbyDuration,
				"Region", region,
				"Lobby_Location", lobbyLocation,
				"Reset_Location", resetLocation,
				"Lives", lives,
				"Destruction", destructionEnabled,
				"Destruction_Whitelist", destructionWhitelist,
				"Map_Reset", mapResetEnabled,
				"World_Reset", worldResetEnabled);
	}
}