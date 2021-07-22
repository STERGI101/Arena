package org.mineacademy.arena.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.MetadataValue;
import org.mineacademy.arena.menu.ClassSelectionMenu;
import org.mineacademy.arena.menu.TeamSelectionMenu;
import org.mineacademy.arena.model.team.TeamArena;
import org.mineacademy.arena.settings.Settings;
import org.mineacademy.arena.tool.ArenaTool;
import org.mineacademy.arena.tool.ToolSpectatePlayers;
import org.mineacademy.arena.util.Constants;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.collection.StrictSet;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.menu.tool.ToolRegistry;
import org.mineacademy.fo.model.HookManager;
import org.mineacademy.fo.model.RandomNoRepeatPicker;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;

import lombok.AccessLevel;
import lombok.Getter;

public abstract class Arena {

	/**
	 * The name of the arena
	 */
	private final String name;

	/**
	 * The type of this arena
	 */
	private final String type;

	/**
	 * The arena settings
	 */
	@Getter
	private final ArenaSettings settings;

	/**
	 * A list of players currently involved with this arena
	 */
	private final List<ArenaPlayer> players = new ArrayList<>();

	/**
	 * The countdown to start the arena
	 */
	@Getter
	private final ArenaCountdownStart startCountdown;

	/**
	 * The heartbeat that controls arena logic such as waves or arena end
	 */
	@Getter
	private final ArenaHeartbeat heartbeat;

	/**
	 * A simple scoreboard to show info during arena play
	 */
	@Getter
	private final ArenaScoreboard scoreboard;

	/**
	 * The mode this arena is currently in
	 */
	private ArenaState state = ArenaState.STOPPED;

	/**
	 * Private flag indicating whether the arena is stopping right now
	 */
	private boolean stopping = false;

	/**
	 * Private flag indicating whether the arena is starting right now
	 */
	private boolean starting = false;

	/**
	 * The players when the lobby countdown hit zero
	 */
	private List<Player> playersAtTheStart = new ArrayList<>();

	/**
	 * Holds data for each player, removed when arena stops
	 */
	@Getter(value = AccessLevel.PROTECTED)
	private final StrictMap<ArenaPlayer, StrictMap<String, Object>> playerTags = new StrictMap<>();

	/**
	 * Create a new arena. If the arena settings do not yet exist,
	 * they are created automatically.
	 *
	 * @param name
	 */
	protected Arena(final String type, final String name) {
		this.type = type;
		this.name = name;
		this.settings = createSettings();
		this.startCountdown = new ArenaCountdownStart(this);
		this.heartbeat = createHeartbeat();
		this.scoreboard = createScoreboard();

		this.settings.setArenaType(type);
	}

	/**
	 * Create new {@link ArenaSettings}. Used in the constructor, override to implement
	 * own settings
	 *
	 * @return
	 */
	protected ArenaSettings createSettings() {
		return new ArenaSettings(this);
	}

	/**
	 * Create new {@link ArenaHeartbeat}. Used in the constructor, override to implement
	 * own settings
	 *
	 * @return
	 */
	protected ArenaHeartbeat createHeartbeat() {
		return new ArenaHeartbeat(this);
	}

	/**
	 * Create new {@link ArenaScoreboard}. Used in the constructor, override to implement
	 * own settings
	 *
	 * @return
	 */
	protected ArenaScoreboard createScoreboard() {
		return new ArenaScoreboard(this);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Player related stuff
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Joins the player in the arena in the given mode
	 *
	 * @param player
	 * @param joinMode
	 */
	public final boolean joinPlayer(final Player player, final ArenaJoinMode joinMode) {
		final ArenaPlayer cache = ArenaPlayer.getCache(player);

		if (!canJoin(player, joinMode)) {
			return false;
		}

		if (joinMode != ArenaJoinMode.EDITING) {
			cache.storeSnapshot(player);

			PlayerUtil.normalize(player, true);

		} else {
			// Call edit start for all registered arena tools
			for (final Tool tool : ToolRegistry.getTools())
				if (tool instanceof ArenaTool && ((ArenaTool<?>) tool).isApplicable(this))
					((ArenaTool<Arena>) tool).onEditStart(player, this);
		}

		try {
			onJoin(player, joinMode);

		} catch (final Throwable t) {
			Common.error(t, "Failed to properly handle " + player.getName() + " joining to arena " + name + ", aborting");

			return false;
		}

		players.add(cache);
		cache.markArenaJoin(player, this, joinMode);

		if (joinMode != ArenaJoinMode.EDITING)
			teleport(player, settings.getLobbyLocation());

		if (joinMode == ArenaJoinMode.SPECTATING)
			transformToSpectate(player);

		// Start countdown and change arena mode
		if (state == ArenaState.STOPPED)
			if (joinMode == ArenaJoinMode.EDITING) {
				state = ArenaState.EDITED;

				onEditStart();
			} else {
				state = ArenaState.LOBBY;

				onLobbyStart();
			}

		checkIntegrity();
		return true;
	}

	/**
	 * Check if the player is eligible for joining this arena
	 *
	 * @param player
	 * @param joinMode
	 * @return
	 */
	protected boolean canJoin(final Player player, final ArenaJoinMode joinMode) {
		final ArenaPlayer cache = ArenaPlayer.getCache(player);

		if (cache.hasArena()) {
			Messenger.error(player, "You are already " + cache.getMode().getLocalized() + " the arena " + cache.getArena().getName() + ".");

			return false;
		}

		if (state == ArenaState.EDITED && joinMode != ArenaJoinMode.EDITING) {
			Messenger.error(player, "Arena " + getName() + " is being edited right now.");

			return false;
		}

		if (state != ArenaState.EDITED && state != ArenaState.STOPPED && joinMode == ArenaJoinMode.EDITING) {
			Messenger.error(player, "Arena " + getName() + " cannot be edited while it's being played.");

			return false;
		}

		if (state != ArenaState.PLAYED && joinMode == ArenaJoinMode.SPECTATING) {
			Messenger.error(player, "Only arenas that are being played may be spectated.");

			return false;
		}

		if (state == ArenaState.PLAYED && joinMode == ArenaJoinMode.PLAYING) {
			Messenger.error(player, "This arena is being played right now. Type '/arena spectate " + getName() + "' to observe the game.");

			return false;
		}

		if (!isReady() && joinMode != ArenaJoinMode.EDITING) {
			Messenger.error(player, "Arena " + getName() + " is not yet configured. If you are an admin, run '/arena edit " + name + "' to see what's missing.");

			return false;
		}

		if (joinMode == ArenaJoinMode.PLAYING && players.size() >= settings.getMaxPlayers()) {
			Messenger.error(player, "Arena " + getName() + " is full (" + settings.getMaxPlayers() + " players)!");

			return false;
		}

		final Region region = settings.getRegion();

		if (region != null && region.isWhole() && ArenaWorldManager.isWorldBeingProcessed(region.getWorld())) {
			Messenger.error(player, "Arena " + getName() + " is being restored right now!");

			return false;
		}

		return true;
	}

	/**
	 * Called when the player joins this arena
	 *
	 * @param player
	 * @param joinMode
	 */
	protected void onJoin(final Player player, final ArenaJoinMode joinMode) {

		if (joinMode != ArenaJoinMode.EDITING) {
			if (joinMode == ArenaJoinMode.PLAYING) {
				Messenger.success(player, "Welcome to " + getName() + "! Arena starts in " + startCountdown.getTimeLeft() + " seconds!");

				// Tell others that the player joined
				broadcast("&8[&2+&8] &7" + player.getName() + " joined the arena! (" + (players.size() + 1) + "/" + settings.getMaxPlayers() + ")");

				// Open team menu if any
				if (hasTeams() && !ArenaTeam.getTeams().isEmpty()) {
					Valid.checkBoolean(this instanceof TeamArena, "Only TeamArena has support for teams! Remove hasTeams() from " + getClass() + " now");

					TeamSelectionMenu.openSelectMenu(player, (TeamArena) this);

					// Open his menu if any
				} else if (hasClasses() && !ArenaClass.getClasses().isEmpty())
					ClassSelectionMenu.openSelectMenu(player, this);

			} else
				// If spectating, only show message
				Messenger.success(player, "Welcome to " + getName() + "! Arena ends in " + heartbeat.getTimeLeft() + " seconds!");
		}

		scoreboard.onPlayerJoin(player, joinMode);
	}

	/**
	 * Remove the player from this arena
	 *
	 * @param player
	 */
	public final void leavePlayer(final Player player, final ArenaLeaveReason reason) {
		final ArenaPlayer arenaPlayer = ArenaPlayer.getCache(player);

		Valid.checkBoolean(!isStopped(), "Cannot leave player " + player.getName() + " from stopped arena!");
		Valid.checkBoolean(arenaPlayer.hasArena() && arenaPlayer.getArena().equals(this), "Player " + player.getName() + " is not joined in " + getName());

		arenaPlayer.setLeavingArena(true);

		if (getPlayers(ArenaJoinMode.PLAYING).size() > 0 && (Settings.Rotate.ENABLED || reason.canSpectate()) && canSpectateOnLeave(player) && !arenaPlayer.isLeavingServer()) {
			onSpectateStart(player, reason);

		} else {
			scoreboard.onPlayerLeave(player);

			players.remove(arenaPlayer);

			try {
				onLeave(player, reason);
			} catch (final Throwable t) {
				Common.error(t, "Failed to properly handle " + player.getName() + " leaving arena " + name + ", stopping for safety");

				if (!isStopped() && !isStopping()) {
					stopArena(ArenaStopReason.ERROR);

					return;
				}
			}

			arenaPlayer.markArenaLeft();

			if (!isEdited()) {
				PlayerUtil.normalize(player, true);

				arenaPlayer.restoreSnapshot(player);

			} else {
				// Call edit stop for all registered arena tools
				for (final Tool tool : ToolRegistry.getTools())
					if (tool instanceof ArenaTool && ((ArenaTool<?>) tool).isApplicable(this))
						((ArenaTool<Arena>) tool).onEditStop(player, this);
			}

			// If we are not stopping, remove from the map automatically
			if (!stopping && getPlayers(ArenaJoinMode.PLAYING).isEmpty() && !isStopped())
				stopArena(ArenaStopReason.LAST_PLAYER_LEFT);

			else
				ArenaRotateManager.onArenaLeave(player);
		}

		arenaPlayer.setLeavingArena(false);
	}

	/**
	 * Called automatically when the player leaves
	 *
	 * @param player
	 */
	protected void onLeave(final Player player, final ArenaLeaveReason reason) {
		final ArenaPlayer arenaPlayer = ArenaPlayer.getCache(player);
		final ArenaJoinMode mode = arenaPlayer.getMode();

		if (!isEdited())
			teleport(player, arenaPlayer.getJoinLocation(), player.getWorld().getSpawnLocation());

		if (reason.getMessage() != null) {
			if (!isEdited())
				Remain.sendTitle(player, "&eGame Over", getState() == ArenaState.PLAYED ? "&fThank you for playing arena " + name : "Arena has ended prematurely.");

			sendLeaveMessage(player, reason, false);

			if (canBroadcastLeave(reason) && reason != ArenaLeaveReason.NO_LIVES_LEFT && mode != ArenaJoinMode.SPECTATING && !isStopping() && getPlayers(ArenaJoinMode.PLAYING).size() > 0)
				broadcast("&8[&4-&8] &7" + player.getName() + " has left the arena! " + Common.plural(getPlayers(ArenaJoinMode.PLAYING).size(), "player") + " left");
		}

		if (mode == ArenaJoinMode.PLAYING)
			giveRewards(player, reason);
	}

	/*
	 * Send the leave message from leave reason to the player
	 */
	private void sendLeaveMessage(final Player player, final ArenaLeaveReason reason, final boolean addSpectateSentence) {

		String message = Replacer.of(reason.getMessage()).replaceAll(
				"arena", name,
				"deaths", settings.getLives(),
				"lacking_players", Common.plural(settings.getMinPlayers() - playersAtTheStart.size(), "player"));

		if (addSpectateSentence && !isStopping())
			message += " You are now spectating the arena and been teleported to a random player.";

		Messenger.announce(player, message);
	}

	/**
	 * Called automatically when the player enters spectate mode after he died
	 *
	 * @param player
	 * @param reason
	 */
	protected void onSpectateStart(final Player player, final ArenaLeaveReason reason) {
		final ArenaPlayer cache = ArenaPlayer.getCache(player);

		cache.markSpectate();

		// We give rewards now instead of waiting till disconnect (easier handling)
		giveRewards(player, reason);

		// Invisibility and flying
		transformToSpectate(player);

		// Send leave message
		sendLeaveMessage(player, reason, true);
	}

	/*
	 * Make player invisible etc. and give him the compass
	 */
	private void transformToSpectate(final Player player) {
		// Normalize
		PlayerUtil.normalize(player, true);

		// Set invisibility
		forEachInAllModes(other -> other.hidePlayer(player));

		// Set flying
		player.setAllowFlight(true);
		player.setFlying(true);

		// Teleport to the first living player
		final List<ArenaPlayer> playing = getArenaPlayers(ArenaJoinMode.PLAYING);
		Valid.checkBoolean(!playing.isEmpty(), "Cannot spectate arena where there are no playing players! Found: " + playing);

		teleport(player, RandomUtil.nextItem(playing).getPlayer().getLocation().add(1, 0, 1));

		// Give a special compass that opens a menu to select players to teleport to
		final PlayerInventory inventory = player.getInventory();
		inventory.setItem(4, ToolSpectatePlayers.getInstance().getItem());
	}

	/**
	 * Return if the given player can spectate the arena
	 *
	 * @param player
	 * @return
	 */
	protected boolean canSpectateOnLeave(final Player player) {
		return getPlayers(ArenaJoinMode.PLAYING).size() > 0;
	}

	/**
	 * Return true if we can broadcast leaving
	 *
	 * @param reason
	 * @return
	 */
	protected boolean canBroadcastLeave(final ArenaLeaveReason reason) {
		return true;
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Rewards
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/*
	 * Check if the player is eligible for rewards and give it to them if so.
	 *
	 * Player is marked as having received the rewards so you can only
	 * call this method once
	 */
	private void giveRewards(final Player player, final ArenaLeaveReason reason) {
		final ArenaPlayer cache = ArenaPlayer.getCache(player);

		if (!reason.canReward() || cache.isRewarded() || hasPlayerComeLater(player))
			return;

		onReward(player, cache);

		final double arenaPoints = cache.getArenaPoints();

		if (arenaPoints > 0) {
			// Convert arena points to total points and clear up arena points
			cache.convertArenaPoints();

			// Convert arena points to your own server's currency
			HookManager.deposit(player, arenaPoints);
		}

		cache.markRewarded();
	}

	/**
	 * Called when the player leaves the arena in a way
	 * that enables him to get rewards, such as running out of lives etc.
	 *
	 * If player enters spectate mode after leaving, this is also fired
	 *
	 * @param player
	 * @param cache
	 */
	protected void onReward(final Player player, final ArenaPlayer cache) {
		if (cache.getArenaPoints() > 0)
			Messenger.warn(player, "You received " + MathUtil.formatTwoDigits(cache.getArenaPoints()) + " points in the arena. Your total balance is now " + cache.getTotalPoints() + " points.");
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Game logic
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Force start the arena
	 */
	public final void startArena() {
		Valid.checkBoolean(state == ArenaState.LOBBY, "Cannot start arena " + getName() + " while in the " + state + " mode");

		this.playersAtTheStart = new ArrayList<>(getPlayersInAllModes());

		state = ArenaState.PLAYED;

		try {
			starting = true;

			onPreStart();

			if (!canStart()) {
				if (!isStopped())
					stopArena(ArenaStopReason.PLUGIN);

				return;
			}

			heartbeat.launch();
			scoreboard.onStart();

			if (startCountdown.isRunning())
				startCountdown.cancel();

			try {
				onStart();
			} catch (final Throwable t) {
				Common.error(t, "Failed to properly handle start of arena " + name);
			}

		} finally {
			starting = false;
		}

		// Close all players inventories
		forEachInAllModes(Player::closeInventory);

		broadcastInfo("Arena " + getName() + " starts now! Players: " + players.size());
		Common.log("Started arena " + getName());
	}

	/**
	 * Called automatically before the arena starts
	 */
	protected void onPreStart() {
		playerTags.clear();

		// Kick players without class
		if (hasClasses()) {

			// Copy list to avoid concurrent errors
			final List<ArenaPlayer> players = new ArrayList<>(this.players);

			// Picker to pick a random class for players who did not select any
			final RandomNoRepeatPicker<ArenaClass> classPicker = RandomNoRepeatPicker.newPicker((player, clazz) -> clazz.canAssign(player, this));

			for (final ArenaPlayer cache : players) {
				// Stopped in the meanwhile
				if (isStopped())
					return;

				final Player player = cache.getPlayer();

				if (cache.getArenaClass() == null) {
					final ArenaClass picked = classPicker.pickFromFor(ArenaClass.getClasses(), player);

					if (picked != null)
						picked.assignTo(cache.getPlayer());

					else {
						leavePlayer(cache.getPlayer(), ArenaLeaveReason.NO_CLASS);

						continue;
					}
				}

				final ArenaClass playerClass = cache.getArenaClass();

				Messenger.info(player, "You are starting with the " + playerClass.getName() + " " + cache.getTier(playerClass) + " class!");
			}
		}

		// If we kicked all players do not continue
		if (isStopped())
			return;

		if (players.size() < settings.getMinPlayers())
			stopArena(ArenaStopReason.NOT_ENOUGH_PLAYERS);
	}

	/**
	 * Return true if the arena can start
	 *
	 * @return
	 */
	protected boolean canStart() {
		return !players.isEmpty();
	}

	/**
	 * Called when the lobby starts on first player join
	 */
	protected void onLobbyStart() {
		Valid.checkBoolean(!startCountdown.isRunning(), "Arena start countdown already running for " + getName());

		startCountdown.launch();
		scoreboard.onLobbyStart();

		// Save the map blocks
		if (settings.isMapResetEnabled())
			ArenaMapManager.saveRegion(this);

		else if (settings.isWorldResetEnabled()) {
			if (settings.getResetLocation() == null) {
				Common.log("Arena " + name + " will not reset world, please set reset location first! Disabling destruction for safety");

				settings.setDestructionEnabled(false);
			} else
				ArenaWorldManager.disableAutoSave(this);
		}
	}

	/**
	 * Called automatically when the first player stars to edit the arena
	 */
	protected void onEditStart() {
		Valid.checkBoolean(!startCountdown.isRunning(), "Arena start countdown already running for " + getName());

		scoreboard.onEditStart();
	}

	/**
	 * Called automatically on game start
	 */
	protected void onStart() {
	}

	/**
	 * Force stop the arena if running, ensure it is running by calling negative {@link #isStopped()}
	 */
	public final void stopArena(final ArenaStopReason reason) {
		Valid.checkBoolean(!stopping, "Already stopping arena " + getName());
		Valid.checkBoolean(state != ArenaState.STOPPED, "Cannot stop a stopped arena " + getName());

		// Wrap in a try-finally block to properly clean the arena and set it back to stopped even on error
		try {
			stopping = true;

			if (state != ArenaState.EDITED) {
				if (startCountdown.isRunning())
					startCountdown.cancel();

				if (heartbeat.isRunning())
					heartbeat.cancel();

				// Try to determine by matching the same enum name between these two reasons
				ArenaLeaveReason leaveReason = ReflectionUtil.lookupEnumSilent(ArenaLeaveReason.class, reason.toString());

				if (leaveReason == null)
					leaveReason = ArenaLeaveReason.ARENA_STOP;

				if (reason == ArenaStopReason.NOT_ENOUGH_PLAYERS)
					leaveReason = ArenaLeaveReason.NOT_ENOUGH_PLAYERS;

				for (final Player player : getPlayersInAllModes())
					leavePlayer(player, leaveReason);
			}

			scoreboard.onStop();
			cleanEntities();

			try {
				onStop();
			} catch (final Throwable t) {
				Common.error(t, "Failed to properly handle stopping arena " + name);
			}

			playerTags.clear();

			// Load the map back to where it was originally
			if (state == ArenaState.PLAYED)
				if (settings.isMapResetEnabled())
					ArenaMapManager.restoreRegion(this);

				else if (settings.isWorldResetEnabled() && settings.getResetLocation() != null)
					ArenaWorldManager.restoreWorld(this);

		} finally {
			state = ArenaState.STOPPED;
			players.clear();

			stopping = false;
			playersAtTheStart.clear();

			Common.log("Stopped arena " + getName());

			ArenaRotateManager.onArenaStop();
		}
	}

	/**
	 * Called automatically on arena stop
	 */
	protected void onStop() {
	}

	/*
	 * Clean up all entities in the arena except players
	 */
	private void cleanEntities() {
		final List<Entity> entities = getSettings().getRegion() != null ? getSettings().getRegion().getEntities() : new ArrayList<>();
		final StrictSet<String> ignoredEntities = new StrictSet<>("PLAYER", "ITEM_FRAME", "PAINTING", "ARMOR_STAND", "LEASH_HITCH");

		for (final Entity entity : entities)
			if (!ignoredEntities.contains(entity.getType().toString()))
				entity.remove();
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Events called from Arena Listener
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Called automatically when the player is being respawned
	 *
	 * @param player
	 * @param cache
	 */
	protected void onPlayerRespawn(final Player player, final ArenaPlayer cache) {
		if (hasLives() && cache.getMode() == ArenaJoinMode.PLAYING) {
			cache.increaseRespawns();

			final int respawns = cache.getRespawns();

			if (respawns >= settings.getLives()) {
				leavePlayer(player, ArenaLeaveReason.NO_LIVES_LEFT);

				returnHandled();
			}

			Messenger.announce(player, "You have died and have " + Common.pluralEs(settings.getLives() - respawns, "life") + " left.");
		}

		if (cache.getMode() == ArenaJoinMode.SPECTATING)
			transformToSpectate(player);

		else {
			if (hasClasses() && cache.getArenaClass() != null) {
				final ArenaClass arenaClass = cache.getArenaClass();

				arenaClass.getTier(cache.getTier(arenaClass)).applyFor(player, false);
			}
		}
	}

	/**
	 * Return a respawn location for the player, used in the respawn event in listener
	 *
	 * @param player
	 * @return
	 */
	protected Location getRespawnLocation(final Player player) {
		return null;
	}

	/**
	 * Called automatically when a player dies
	 *
	 * @param player
	 * @param event
	 */
	protected void onPlayerDeath(final Player player, final PlayerDeathEvent event) {
		final ArenaPlayer cache = ArenaPlayer.getCache(player);

		if (hasDeathMessages() && cache.getMode() == ArenaJoinMode.PLAYING)
			broadcastExcept(player, "&8[&4&l-&8] &7Player " + player.getName() + " has died");

		event.setDroppedExp(0);
		event.getDrops().clear();
	}

	/**
	 * Called automatically when the player kills someone/something
	 *
	 * @param killer
	 * @param victim
	 */
	protected void onPlayerKill(final Player killer, final LivingEntity victim) {
	}

	/**
	 * Called automatically when players attack one another
	 *
	 * @param attacker
	 * @param victim
	 * @param event
	 */
	protected void onPvP(final Player attacker, final Player victim, final EntityDamageByEntityEvent event) {
	}

	/**
	 * Called automatically when player damages something that is not a player
	 *
	 * @param attacker
	 * @param victim
	 * @param event
	 */
	protected void onPlayerDamage(final Player attacker, final Entity victim, final EntityDamageByEntityEvent event) {
	}

	/**
	 * Called when an unknown entity damages an unknown entity (both are in this arena)
	 *
	 * @param attacker
	 * @param victim
	 * @param event
	 */
	protected void onDamage(final Entity attacker, final Entity victim, final EntityDamageByEntityEvent event) {
	}

	/**
	 * Called when an unknown entity gets hit by something
	 *
	 * @param victim
	 * @param event
	 */
	protected void onDamage(final Entity victim, final EntityDamageEvent event) {
	}

	/**
	 * Called when a block is placed
	 *
	 * @param player
	 * @param block
	 * @param event
	 */
	protected void onBlockPlace(Player player, Block block, BlockPlaceEvent event) {
		handleBlockInteraction(player, block, event);
	}

	/**
	 * Called when a block is broken
	 *
	 * @param player
	 * @param block
	 * @param event
	 */
	protected void onBlockBreak(Player player, Block block, BlockBreakEvent event) {
		handleBlockInteraction(player, block, event);
	}

	private void handleBlockInteraction(Player player, Block block, Cancellable event) {
		if (settings.getDestructionWhitelist().contains(CompMaterial.fromBlock(block)))
			returnHandled();

		event.setCancelled(true);
		player.updateInventory();
	}

	/**
	 * Called when a player clicks something
	 *
	 * @param player
	 * @param action
	 * @param event
	 */
	protected void onClick(Player player, Action action, PlayerInteractEvent event) {
	}

	/**
	 * Called when a player right clicks an entity
	 *
	 * @param player
	 * @param clicked
	 * @param event
	 */
	protected void onEntityClick(Player player, Entity clicked, PlayerInteractAtEntityEvent event) {
	}

	/**
	 * Called when an explosion should damage the blocks
	 *
	 * @param centerLocation
	 * @param blocks
	 * @param event
	 */
	protected void onExplosion(Location centerLocation, List<Block> blocks, Cancellable event) {
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Messaging
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Sends a message to all players in the arena
	 *
	 * @param message
	 */
	public final void broadcastInfo(final String message) {
		checkIntegrity();

		forEachInAllModes(player -> Messenger.info(player, message));
	}

	/**
	 * Sends a warning message to all players in the arena
	 *
	 * @param message
	 */
	public final void broadcastWarn(final String message) {
		checkIntegrity();

		forEachInAllModes(player -> Messenger.warn(player, message));
	}

	/**
	 * Sends a generic no prefix message to all players
	 *
	 * @param message
	 */
	public final void broadcast(final String message) {
		checkIntegrity();

		forEachInAllModes(player -> Common.tellNoPrefix(player, message));
	}

	/**
	 * Send a message to all players except the given one
	 *
	 * @param exception
	 * @param message
	 */
	public final void broadcastExcept(final Player exception, final String message) {
		for (final Player player : getPlayersInAllModes())
			if (!player.getName().equals(exception.getName()))
				Common.tellNoPrefix(player, message);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Arena configuration
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Return true if this arena supports the lives system
	 *
	 * @return false by default
	 */
	protected boolean hasLives() {
		return false;
	}

	/**
	 * Return true if this arena support classes
	 *
	 * @return
	 */
	protected boolean hasClasses() {
		return false;
	}

	/**
	 * Return true if this arena support teams
	 *
	 * @return
	 */
	protected boolean hasTeams() {
		return false;
	}

	/**
	 * Can players kill one another in the arena?
	 *
	 * @return
	 */
	protected boolean hasPvP() {
		return false;
	}

	/**
	 * Should we broadcast messages that players died?
	 *
	 * @return
	 */
	protected boolean hasDeathMessages() {
		return true;
	}

	/**
	 * Throws {@link ArenaPipelineEndException} exception to mark end in the pipeline
	 *
	 * @see ArenaPipelineEndException
	 */
	protected final void returnHandled() {
		throw new ArenaPipelineEndException();
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Getters
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Get the arena name
	 *
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * Get the arena type
	 *
	 * @return the type
	 */
	public final String getType() {
		return type;
	}

	/**
	 * Get the arena state
	 *
	 * @return the state
	 */
	public final ArenaState getState() {
		return state;
	}

	/**
	 * Return true if the arena is ready to be played
	 *
	 * @return
	 */
	public boolean isReady() {
		return settings.isSetup();
	}

	/**
	 * Return true if the arena is stopped right now
	 * <p>
	 * Arenas that are played or edited are not stopped, see {@link #getState()}
	 *
	 * @return
	 */
	public final boolean isStopped() {
		return state == ArenaState.STOPPED;
	}

	/**
	 * Return true if the arena state is edited
	 *
	 * @return
	 */
	public final boolean isEdited() {
		return state == ArenaState.EDITED;
	}

	/**
	 * Return true if the arena state is played
	 *
	 * @return
	 */
	public final boolean isPlayed() {
		return state == ArenaState.PLAYED;
	}

	/**
	 * Return if the arena is stopping
	 *
	 * @return
	 */
	public final boolean isStopping() {
		return stopping;
	}

	/**
	 * Return if the arena is starting
	 *
	 * @return
	 */
	public final boolean isStarting() {
		return starting;
	}

	/**
	 * Get all players currently joined in all modes
	 *
	 * @return
	 */
	public final List<Player> getPlayersInAllModes() {
		return getPlayers(null);
	}

	/**
	 * Get a list of players in the given mode
	 *
	 * @param mode
	 * @return
	 */
	public final List<Player> getPlayers(final ArenaJoinMode mode) {
		return Common.convert(getArenaPlayers(mode), ArenaPlayer::getPlayer);
	}

	/**
	 * Get arena players currently joined in all modes
	 *
	 * @return
	 */
	public final List<ArenaPlayer> getArenaPlayersInAllModes() {
		return Collections.unmodifiableList(players);
	}

	/**
	 * Get arena players in the given mode
	 *
	 * @param mode
	 * @return
	 */
	public final List<ArenaPlayer> getArenaPlayers(final ArenaJoinMode mode) {
		final List<ArenaPlayer> list = new ArrayList<>();

		for (final ArenaPlayer arenaPlayer : players)
			if (mode == null || arenaPlayer.getMode() == mode) {
				if (arenaPlayer.getMode() == ArenaJoinMode.PLAYING && arenaPlayer.isLeavingArena())
					continue;

				list.add(arenaPlayer);
			}

		return list;
	}

	/**
	 * Return the arena player, or null if he is not in this arena
	 *
	 * @param player
	 * @return
	 */
	public final ArenaPlayer findPlayer(final Player player) {
		checkIntegrity();

		for (final ArenaPlayer arenaPlayer : players)
			if (arenaPlayer.hasArena() && arenaPlayer.getArena().equals(this) && arenaPlayer.getId().equals(player.getUniqueId()))
				return arenaPlayer;

		return null;
	}

	/**
	 * Return if the player has joined after the arena has begun
	 *
	 * @param player
	 * @return
	 */
	protected final boolean hasPlayerComeLater(final Player player) {
		return !playersAtTheStart.contains(player);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Player data
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Return true if the given player has data at the given key
	 *
	 * @param player
	 * @param key
	 * @return
	 */
	protected final boolean hasPlayerTag(final ArenaPlayer player, final String key) {
		return getPlayerTag(player, key) != null;
	}

	/**
	 * Return the a value at the given key for the player, null if not set
	 *
	 * @param <T>
	 * @param player
	 * @param key
	 * @return
	 */
	protected final <T> T getPlayerTag(final ArenaPlayer player, final String key) {
		if (playerTags.contains(player)) {
			final Object value = playerTags.get(player).get(key);

			return value != null ? (T) value : null;
		}

		return null;
	}

	/**
	 * Sets the player a key-value data pair that is persistent until the arena finishes
	 * even if the player gets kicked out
	 *
	 * @param player
	 * @param key
	 * @param value
	 */
	protected final void setPlayerTag(final ArenaPlayer player, final String key, final Object value) {
		final StrictMap<String, Object> playerData = playerTags.getOrDefault(player, new StrictMap<>());

		playerData.override(key, value);
		playerTags.override(player, playerData);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Tags
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Return if the given entity has a tag
	 *
	 * @param entity
	 * @param key
	 * @return
	 */
	protected final boolean hasEntityTag(final Entity entity, final String key) {
		return getEntityTag(entity, key) != null;
	}

	/**
	 * Get a numeric tag from the entity or default if it does not have one
	 *
	 * @param entity
	 * @param key
	 * @param def
	 * @return
	 */
	protected final int getNumericEntityTag(final Entity entity, final String key, final int def) {
		final MetadataValue value = CompMetadata.getTempMetadata(entity, key);

		return value != null ? value.asInt() : def;
	}

	/**
	 * Get a String tag from the entity or null if it does not have one
	 *
	 * @param entity
	 * @param key
	 * @return
	 */
	protected final String getEntityTag(final Entity entity, final String key) {
		final MetadataValue value = CompMetadata.getTempMetadata(entity, key);

		return value != null ? value.asString() : null;
	}

	/**
	 * Set a numeric tag for an entity, overriding the old one if existed
	 *
	 * @param entity
	 * @param key
	 * @param value
	 */
	protected final void setNumericEntityTag(final Entity entity, final String key, final int value) {
		setEntityTag(entity, key, String.valueOf(value));
	}

	/**
	 * Set a boolean tag for an entity, overriding the old one if existed
	 *
	 * @param entity
	 * @param key
	 * @param value
	 */
	protected final void setBooleanEntityTag(final Entity entity, final String key, final boolean value) {
		setEntityTag(entity, key, String.valueOf(value));
	}

	/**
	 * Set a string tag for an entity, overriding the old one if existed
	 *
	 * @param entity
	 * @param key
	 * @param value
	 */
	protected final void setEntityTag(final Entity entity, final String key, final String value) {
		Valid.checkBoolean(!(entity instanceof Player), "To set tags for players use the setPlayerTag method!");

		CompMetadata.setTempMetadata(entity, key, value);
	}

	/**
	 * Remove a tag from the entity
	 *
	 * @param entity
	 * @param key
	 */
	protected final void removeEntityTag(final Entity entity, final String key) {
		CompMetadata.removeTempMetadata(entity, key);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Utils
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Run a function for all players in the arena regardless of their mode
	 *
	 * @param consumer
	 */
	protected final void forEachInAllModes(final Consumer<Player> consumer) {
		forEach(consumer, null);
	}

	/**
	 * Run a function for each players having the given mode
	 *
	 * @param consumer
	 * @param mode
	 */
	protected final void forEach(final Consumer<Player> consumer, final ArenaJoinMode mode) {
		for (final Player player : getPlayers(mode))
			consumer.accept(player);
	}

	/**
	 * Teleports the player to the given location
	 *
	 * @param player
	 * @param location
	 */
	protected void teleport(final Player player, final Location location) {
		teleport(player, location, null);
	}

	/**
	 * Teleport the player to the given location, or to the fallback location if failed
	 *
	 * @param player
	 * @param location
	 * @param fallbackLocation
	 */
	protected void teleport(final Player player, final Location location, final Location fallbackLocation) {
		Valid.checkBoolean(player != null && player.isOnline(), "Cannot teleport offline players!");
		Valid.checkBoolean(!player.isDead(), "Cannot teleport dead player " + player.getName());

		if (location == null)
			Valid.checkNotNull(fallbackLocation, "Cannot teleport " + player.getName() + " to a null location and fallback location!");

		final Location topOfTheBlock = location.getBlock().getLocation().add(0.5, 1, 0.5);

		// Since we prevent players escaping the arena, add a special invisible tag
		// that we use to check if we can actually enable the teleportation
		CompMetadata.setTempMetadata(player, Constants.Tag.TELEPORT_EXEMPTION);

		boolean success = player.teleport(topOfTheBlock, PlayerTeleportEvent.TeleportCause.PLUGIN);

		try {
			if (!success) {
				Common.log("Failed to teleport " + player.getName() + " to " + location + ", trying fallback location " + fallbackLocation);

				success = fallbackLocation != null ? player.teleport(fallbackLocation.add(0.5, 1, 0.5), PlayerTeleportEvent.TeleportCause.PLUGIN) : false;
				Valid.checkBoolean(success, "Failed to teleport " + player.getName() + " to both primary and fallback location, they may get stuck in the arena!");
			}
		} finally {

			// Remove the tag after the teleport. Also remove in case of failure to clear up
			CompMetadata.removeTempMetadata(player, Constants.Tag.TELEPORT_EXEMPTION);
		}
	}

	/*
	 * Runs a few security checks to prevent accidental programming errors
	 */
	private void checkIntegrity() {
		int playing = 0, editing = 0, spectating = 0;

		for (final ArenaPlayer arenaPlayer : players) {
			final Player player = arenaPlayer.getPlayer();
			final ArenaJoinMode mode = arenaPlayer.getMode();

			Valid.checkBoolean(player != null && player.isOnline(), "Found a disconnected player " + player + " in arena " + getName());

			if (mode == ArenaJoinMode.PLAYING)
				playing++;

			else if (mode == ArenaJoinMode.EDITING)
				editing++;

			else if (mode == ArenaJoinMode.SPECTATING)
				spectating++;
		}

		if (state == ArenaState.STOPPED)
			Valid.checkBoolean(players.isEmpty(), "Found players in a stopped " + getName() + " arena: " + players);

		if (editing > 0) {
			Valid.checkBoolean(state == ArenaState.EDITED, "Arena " + getName() + " must be in EDIT mode not " + state + " while there are " + editing + " editing players!");
			Valid.checkBoolean(playing == 0 && spectating == 0, "Found " + playing + " and " + spectating + " players in edited arena " + getName());
		}
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(final Object obj) {
		return obj instanceof Arena && ((Arena) obj).getName().equals(this.name);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public final String toString() {
		return "Arena{name=" + getName() + "}";
	}
}
