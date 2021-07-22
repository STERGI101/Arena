package org.mineacademy.arena.model.team.ctf;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.mineacademy.arena.model.ArenaLeaveReason;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.arena.model.ArenaSettings;
import org.mineacademy.arena.model.ArenaStopReason;
import org.mineacademy.arena.model.ArenaTeam;
import org.mineacademy.arena.model.team.TeamArena;
import org.mineacademy.arena.util.Constants;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.remain.CompSound;

/**
 * Represents a team arena where players can respawn infinitely and
 * the goal is to destroy opponents team
 */
public class CaptureTheFlagArena extends TeamArena {

	/**
	 * The arena unique identification type
	 */
	public static final String TYPE = "ctf";

	/**
	 * Create a new arena type
	 *
	 * @param name
	 */
	public CaptureTheFlagArena(final String name) {
		super(TYPE, name);
	}

	/**
	 * @see org.mineacademy.arena.model.team.TeamArena#createSettings()
	 */
	@Override
	protected ArenaSettings createSettings() {
		return new CaptureTheFlagSettings(this);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Game logic
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Spawns crystals when the game starts
	 *
	 * @see org.mineacademy.arena.model.team.TeamArena#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();

		// Spawn crystals to the arena
		for (final Location crystalLocation : getSettings().getCrystals()) {
			final ArenaTeam crystalTeam = getSettings().findCrystalTeam(crystalLocation);

			final EnderCrystal crystal = crystalLocation.getWorld().spawn(crystalLocation.clone().add(0, 1, 0), EnderCrystal.class);

			// Put the team name as an invisible flag to them so that we know
			// what team got their crystal (flag) destroyed
			setEntityTag(crystal, Constants.Tag.TEAM_CRYSTAL, crystalTeam.getName());

			// Set team data to have this crystal alive
			setTeamTag(crystalTeam, Constants.Tag.CRYSTAL_ALIVE, true);
		}
	}

	/**
	 * Remove crystals on game over
	 *
	 * @see org.mineacademy.arena.model.Arena#onStop()
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
	 * Prevent custom player kill logic
	 *
	 * @see org.mineacademy.arena.model.team.TeamArena#onPlayerKill(org.bukkit.entity.Player, org.bukkit.entity.LivingEntity)
	 */
	@Override
	protected void onPlayerKill(Player killer, LivingEntity victim) {
		// Prevent override to disable rewards for killing players
	}

	/**
	 * Handle crystal damage
	 *
	 * @see org.mineacademy.arena.model.Arena#onPlayerDamage(org.bukkit.entity.Player, org.bukkit.entity.Entity, org.bukkit.event.entity.EntityDamageByEntityEvent)
	 */
	@Override
	protected void onPlayerDamage(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
		if (victim instanceof EnderCrystal) {

			final ArenaTeam attackerTeam = ArenaPlayer.getCache(attacker).getArenaTeam();
			final ArenaTeam crystalTeam = ArenaTeam.findTeam(getEntityTag(victim, Constants.Tag.TEAM_CRYSTAL));

			if (attackerTeam.equals(crystalTeam)) {
				Messenger.error(attacker, "You cannot damage your own crystal!");

			} else {
				int damage = getNumericEntityTag(victim, Constants.Tag.CRYSTAL_DAMAGE, 0);
				final int threshold = getSettings().getCrystalHitThreshold();

				if (++damage >= threshold) {
					setTeamTag(crystalTeam, Constants.Tag.CRYSTAL_ALIVE, false);
					leaveTeamPlayers(crystalTeam, ArenaLeaveReason.CRYSTAL_DESTROYED);

					event.setCancelled(true);
					returnHandled();
				}

				// Broadcast every second hit only
				if (damage % 2 == 0) {
					for (final Player player : getPlayersInAllModes()) {
						if (player.equals(attacker)) {
							Messenger.info(player, Common.format("Damaged %s team's crystal! (%s/%s)", crystalTeam.getName(), damage, threshold));
							CompSound.ANVIL_LAND.play(player);

							continue;
						}

						final ArenaTeam playerTeam = ArenaPlayer.getCache(player).getArenaTeam();

						if (playerTeam != null && playerTeam.equals(crystalTeam)) {
							Messenger.info(player, Common.format("&eYour crystal got damaged! (%s/%s)", damage, threshold));

							CompSound.SUCCESSFUL_HIT.play(player);

						} else
							Messenger.info(player, Common.format("%s team's crystal just got damaged! (%s/%s)", crystalTeam.getName(), damage, threshold));
					}
				}

				setNumericEntityTag(victim, Constants.Tag.CRYSTAL_DAMAGE, damage);
			}

			event.setCancelled(true);
			returnHandled();
		}
	}

	/**
	 * Prevent any form of crystal damage from non players
	 *
	 * @see org.mineacademy.arena.model.Arena#onDamage(org.bukkit.entity.Entity, org.bukkit.entity.Entity, org.bukkit.event.entity.EntityDamageByEntityEvent)
	 */
	@Override
	protected void onDamage(Entity attacker, Entity victim, EntityDamageByEntityEvent event) {
		if (victim instanceof EnderCrystal) {
			event.setCancelled(true);

			returnHandled();
		}
	}

	/**
	 * @see org.mineacademy.arena.model.team.TeamArena#onLeave(org.bukkit.entity.Player, org.mineacademy.arena.model.ArenaLeaveReason)
	 */
	@Override
	protected void onLeave(Player player, ArenaLeaveReason reason) {
		super.onLeave(player, reason);

		checkStop(reason);
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#onSpectateStart(org.bukkit.entity.Player, org.mineacademy.arena.model.ArenaLeaveReason)
	 */
	@Override
	protected void onSpectateStart(Player player, ArenaLeaveReason reason) {
		super.onSpectateStart(player, reason);

		checkStop(reason);
	}

	/*
	 * Stop the arena if only 1 team is left
	 */
	private void checkStop(ArenaLeaveReason reason) {
		final ArenaTeam lastTeam = getLastTeamStanding();

		// Stop the arena if there is only 1 team left
		if (!isStopped() && !isStopping() && lastTeam != null) {
			boolean allOtherCrystalsDestroyed = true;

			for (final ArenaTeam team : getTeamTags().keySet()) {
				if (team.equals(lastTeam))
					continue;

				final boolean crystalAlive = getTeamTag(team, Constants.Tag.CRYSTAL_ALIVE);

				if (crystalAlive) {
					allOtherCrystalsDestroyed = false;

					break;
				}
			}

			leaveTeamPlayers(lastTeam, allOtherCrystalsDestroyed && reason == ArenaLeaveReason.CRYSTAL_DESTROYED ? ArenaLeaveReason.LAST_TEAM_STANDING : ArenaLeaveReason.OTHER_TEAMS_LEFT);

			if (!isStopped())
				stopArena(ArenaStopReason.PLUGIN);
		}
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Pluggables
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Players can respawn indefinitely
	 */
	@Override
	protected boolean hasLives() {
		return false;
	}

	/**
	 * Custom handling of teams leaving
	 */
	@Override
	protected boolean stopIfLastStanding() {
		return false;
	}

	/**
	 * Do not broadcast that the playerl left if their crystal got destroyed
	 *
	 * @see org.mineacademy.arena.model.Arena#canBroadcastLeave(org.mineacademy.arena.model.ArenaLeaveReason)
	 */
	@Override
	protected boolean canBroadcastLeave(ArenaLeaveReason reason) {
		return reason != ArenaLeaveReason.CRYSTAL_DESTROYED;
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Overrides
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * @see org.mineacademy.arena.model.team.TeamArena#getSettings()
	 */
	@Override
	public CaptureTheFlagSettings getSettings() {
		return (CaptureTheFlagSettings) super.getSettings();
	}
}
