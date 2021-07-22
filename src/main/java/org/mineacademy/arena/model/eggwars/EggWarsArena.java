package org.mineacademy.arena.model.eggwars;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Cancellable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.mineacademy.arena.menu.EggWarsVillagerMenu;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaHeartbeat;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaLeaveReason;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.arena.model.ArenaScoreboard;
import org.mineacademy.arena.util.Constants;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.RandomNoRepeatPicker;
import org.mineacademy.fo.remain.CompProperty;
import org.mineacademy.fo.remain.CompSound;

/**
 * Eggwars arena is a game where players spawn on their
 * islands where they collect items spawned on the ground
 * in exchange for items.
 *
 * The point is to destroy one another's crystal (egg) then kill
 * the other player. Once your crystal is destroyed you stop respawning.
 * Last man standing wins.
 */
public class EggWarsArena extends Arena {

	/**
	 * The arena unique identification type
	 */
	public static final String TYPE = "eggwars";

	/**
	 * Create a new arena
	 *
	 * @param name
	 */
	public EggWarsArena(final String name) {
		super(TYPE, name);
	}

	/**
	 * Create new arena settings
	 */
	@Override
	protected EggWarsSettings createSettings() {
		return new EggWarsSettings(this);
	}

	/**
	 * Create new arena heartbeat
	 */
	@Override
	protected ArenaHeartbeat createHeartbeat() {
		return new EggWarsHeartbeat(this);
	}

	/**
	 * Return a custom scoreboard for this arena
	 */
	@Override
	protected ArenaScoreboard createScoreboard() {
		return new EggWarsScoreboard(this);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Game logic
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Teleport players and spawn eggs/villagers
	 */
	@Override
	protected void onStart() {
		super.onStart();

		if (isEdited())
			return;

		// Use no repeat picker to ensure no 2 players will spawn at one spot
		final RandomNoRepeatPicker<Location> locationPicker = RandomNoRepeatPicker.newPicker(Location.class);
		locationPicker.setItems(getSettings().getEntrances());

		final List<Location> eggLocations = Common.toList(getSettings().getEggs());

		for (final ArenaPlayer arenaPlayer : getArenaPlayers(ArenaJoinMode.PLAYING)) {
			final Player player = arenaPlayer.getPlayer();
			final Location location = locationPicker.pickRandom(player);

			// Teleport to arena
			teleport(player, location);

			// Save their location for respawns
			setPlayerTag(arenaPlayer, Constants.Tag.ENTRANCE_LOCATION, location);

			// Spawn crystal
			final Location closestEgg = BlockUtil.findClosestLocation(player.getLocation(), eggLocations);
			eggLocations.remove(closestEgg);

			final EnderCrystal crystal = closestEgg.getWorld().spawn(closestEgg.clone().add(0.5, 1, 0.5), EnderCrystal.class);
			setEntityTag(crystal, Constants.Tag.TEAM_CRYSTAL, player.getName());
		}

		// Spawn villagers
		for (final Location villagerLocation : getSettings().getVillagers()) {
			final Villager villager = villagerLocation.getWorld().spawn(villagerLocation.clone().add(0.5, 1, 0.5), Villager.class);

			CompProperty.INVULNERABLE.apply(villager, true);
		}
	}

	/**
	 * Remove crystals on stop
	 */
	@Override
	protected void onStop() {
		super.onStop();

		removeCrystals();
	}

	/*
	 * Remove crystals that are left when the arena stops
	 */
	private void removeCrystals() {
		final World world = getSettings().getRegion().getWorld();

		for (final EnderCrystal crystal : world.getEntitiesByClass(EnderCrystal.class))
			if (hasEntityTag(crystal, Constants.Tag.TEAM_CRYSTAL))
				crystal.remove();
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#getRespawnLocation(org.bukkit.entity.Player)
	 */
	@Override
	protected Location getRespawnLocation(final Player player) {
		final ArenaPlayer cache = ArenaPlayer.getCache(player);
		final Location location = getPlayerTag(cache, Constants.Tag.ENTRANCE_LOCATION);
		Valid.checkNotNull(location, "Player " + player.getName() + " is missing entrance location!");

		return location;
	}

	/**
	 * Kick players without a crystal
	 */
	@Override
	protected void onPlayerRespawn(Player player, ArenaPlayer cache) {
		super.onPlayerRespawn(player, cache);

		if (!hasCrystal(player)) {
			leavePlayer(player, ArenaLeaveReason.CRYSTAL_DESTROYED);

			returnHandled();
		}
	}

	/*
	 * Return true if there is a crystal with the player tag associated spawned in the arena
	 */
	private boolean hasCrystal(Player player) {
		for (final Entity entity : getSettings().getRegion().getEntities())
			if (entity instanceof EnderCrystal && player.getName().equals(getEntityTag(entity, Constants.Tag.TEAM_CRYSTAL)))
				return true;

		return false;
	}

	/**
	 * Handle clicking on villagers
	 *
	 * @see org.mineacademy.arena.model.Arena#onEntityClick(org.bukkit.entity.Player, org.bukkit.entity.Entity, org.bukkit.event.player.PlayerInteractAtEntityEvent)
	 */
	@Override
	protected void onEntityClick(Player player, Entity clicked, PlayerInteractAtEntityEvent event) {
		super.onEntityClick(player, clicked, event);

		if (clicked instanceof Villager)
			EggWarsVillagerMenu.openPurchaseMenu(this, player);
	}

	/**
	 * Prevent crystal going BOOM
	 *
	 * @see org.mineacademy.arena.model.Arena#onDamage(org.bukkit.entity.Entity, org.bukkit.entity.Entity, org.bukkit.event.entity.EntityDamageByEntityEvent)
	 */
	@Override
	protected void onDamage(Entity attacker, Entity victim, EntityDamageByEntityEvent event) {
		super.onDamage(attacker, victim, event);

		if (victim instanceof EnderCrystal)
			event.setCancelled(true);
	}

	/**
	 * Handle crystal damage
	 *
	 * @see org.mineacademy.arena.model.Arena#onPlayerDamage(org.bukkit.entity.Player, org.bukkit.entity.Entity, org.bukkit.event.entity.EntityDamageByEntityEvent)
	 */
	@Override
	protected void onPlayerDamage(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
		super.onPlayerDamage(attacker, victim, event);

		if (!(victim instanceof EnderCrystal) || !hasEntityTag(victim, Constants.Tag.TEAM_CRYSTAL))
			return;

		final ArenaPlayer attackerCache = ArenaPlayer.getCache(attacker);

		final Player crystalOwner = Bukkit.getPlayer(getEntityTag(victim, Constants.Tag.TEAM_CRYSTAL));
		final ArenaPlayer crystalOwnerCache = findPlayer(crystalOwner);

		if (attackerCache.equals(crystalOwnerCache)) {
			Messenger.error(attacker, "You cannot damage your own crystal!");

		} else {
			int damage = getNumericEntityTag(victim, Constants.Tag.CRYSTAL_DAMAGE, 0);
			final int threshold = 10;

			if (++damage >= threshold) {
				victim.remove();

				broadcastWarn(crystalOwner.getName() + "'s egg got destroyed!");

				event.setCancelled(true);
				returnHandled();
			}

			// Broadcast every second hit only
			if (damage % 2 == 0) {
				for (final Player otherPlayer : getPlayersInAllModes()) {
					if (otherPlayer.equals(attacker)) {
						Messenger.info(otherPlayer, Common.format("Damaged %s's egg! (%s/%s)", crystalOwner.getName(), damage, threshold));
						CompSound.ANVIL_LAND.play(otherPlayer);

						continue;
					}

					if (otherPlayer.equals(crystalOwner)) {
						Messenger.info(otherPlayer, Common.format("&eYour egg got damaged! (%s/%s)", damage, threshold));

						CompSound.SUCCESSFUL_HIT.play(otherPlayer);

					} else
						Messenger.info(otherPlayer, Common.format("%s's crystal just got damaged! (%s/%s)", crystalOwner.getName(), damage, threshold));
				}
			}

			setNumericEntityTag(victim, Constants.Tag.CRYSTAL_DAMAGE, damage);
		}

		event.setCancelled(true);
		returnHandled();
	}

	/**
	 * Prevent explosions from damaging safezone blocks
	 *
	 * @see org.mineacademy.arena.model.Arena#onExplosion(org.bukkit.Location, java.util.List, org.bukkit.event.Cancellable)
	 */
	@Override
	protected void onExplosion(Location centerLocation, List<Block> blocks, Cancellable event) {
		super.onExplosion(centerLocation, blocks, event);

		final EggWarsSettings settings = getSettings();
		final List<Location> locations = Common.joinArrays(
				settings.getIron(),
				settings.getGold(),
				settings.getDiamonds());

		for (final Entity entity : settings.getRegion().getEntities())
			if (entity instanceof EnderCrystal || entity instanceof Villager)
				locations.add(entity.getLocation());

		for (final Iterator<Block> it = blocks.iterator(); it.hasNext();) {
			final Block block = it.next();
			final Location blockLocation = block.getLocation();
			final Location closestSafezone = BlockUtil.findClosestLocation(blockLocation, locations);

			if (closestSafezone.distance(blockLocation) < 3)
				it.remove();
		}
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#onLeave(org.bukkit.entity.Player, org.mineacademy.arena.model.ArenaLeaveReason)
	 */
	@Override
	protected void onLeave(final Player player, final ArenaLeaveReason reason) {
		super.onLeave(player, reason);

		checkLastStanding();
	}

	/**
	 * Run the last standing check when a player enters spectate mode, stop
	 * arena if only 1 player is left playing
	 */
	@Override
	protected void onSpectateStart(final Player player, final ArenaLeaveReason reason) {
		super.onSpectateStart(player, reason);

		checkLastStanding();
	}

	/*
	 * Check if there is only one last player in the playing mode then stop the arena
	 * and announce winner
	 */
	private void checkLastStanding() {
		if (getPlayers(ArenaJoinMode.PLAYING).size() == 1 && !isStopping()) {
			final Player winner = getPlayers(ArenaJoinMode.PLAYING).get(0);

			leavePlayer(winner, ArenaLeaveReason.LAST_STANDING);
		}
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#canSpectateOnLeave(org.bukkit.entity.Player)
	 */
	@Override
	protected boolean canSpectateOnLeave(final Player player) {
		return getPlayers(ArenaJoinMode.PLAYING).size() > 1;
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Pluggable
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * @see org.mineacademy.arena.model.Arena#hasPvP()
	 */
	@Override
	protected boolean hasPvP() {
		return true;
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Overrides
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * @see org.mineacademy.arena.model.Arena#getSettings()
	 */
	@Override
	public EggWarsSettings getSettings() {
		return (EggWarsSettings) super.getSettings();
	}
}
