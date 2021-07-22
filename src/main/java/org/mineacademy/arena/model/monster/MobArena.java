package org.mineacademy.arena.model.monster;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaHeartbeat;
import org.mineacademy.arena.model.ArenaLeaveReason;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.arena.model.ArenaScoreboard;
import org.mineacademy.arena.model.ArenaSettings;
import org.mineacademy.arena.model.ArenaState;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.RandomUtil;

/**
 * A simple monster arena
 */
public class MobArena extends Arena {

	/**
	 * The arena unique identification type
	 */
	public static final String TYPE = "monster";

	/**
	 * Create a new monster arena
	 *
	 * @param name
	 */
	public MobArena(final String name) {
		super(TYPE, name);
	}

	/**
	 * Create new arena settings
	 */
	@Override
	protected ArenaSettings createSettings() {
		return new MobArenaSettings(this);
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#createHeartbeat()
	 */
	@Override
	protected ArenaHeartbeat createHeartbeat() {
		return new MobArenaHeartbeat(this);
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#createScoreboard()
	 */
	@Override
	protected ArenaScoreboard createScoreboard() {
		return new MobArenaScoreboard(this);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Game logic
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * @see org.mineacademy.arena.model.Arena#onStart()
	 */
	@Override
	protected void onStart() {
		super.onStart();

		if (getState() != ArenaState.EDITED)
			forEachInAllModes((player) -> teleport(player, getSettings().getEntranceLocation()));
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#getRespawnLocation(org.bukkit.entity.Player)
	 */
	@Override
	protected Location getRespawnLocation(final Player player) {
		return getSettings().getEntranceLocation();
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#onPlayerKill(org.bukkit.entity.Player, org.bukkit.entity.LivingEntity)
	 */
	@Override
	protected void onPlayerKill(final Player killer, final LivingEntity victim) {
		super.onPlayerKill(killer, victim);

		if (victim instanceof Monster) {
			final ArenaPlayer cache = ArenaPlayer.getCache(killer);
			final double points = MathUtil.formatTwoDigitsD(RandomUtil.nextBetween(10, 20) + Math.random());

			cache.giveArenaPoints(killer, points);
			Messenger.warn(killer, "You received " + points + " points for killing " + Common.article(ItemUtil.bountifyCapitalized(victim.getType()))
					+ " and now have " + cache.getArenaPoints() + " points!");
		}
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#onSpectateStart(org.bukkit.entity.Player, org.mineacademy.arena.model.ArenaLeaveReason)
	 */
	@Override
	protected void onSpectateStart(final Player player, final ArenaLeaveReason reason) {
		super.onSpectateStart(player, reason);

		final ArenaPlayer cache = ArenaPlayer.getCache(player);

		if (!hasPlayerComeLater(player) && cache.getPlayedToWave() == 0)
			cache.setPlayedToWave(getHeartbeat().getWave());
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#onReward(org.bukkit.entity.Player, org.mineacademy.arena.model.ArenaPlayer)
	 */
	@Override
	protected void onReward(final Player player, final ArenaPlayer cache) {
		//super.onReward(player, cache); Disable the generic points message

		final int wave = cache.getPlayedToWave() != 0 ? cache.getPlayedToWave() : getHeartbeat().getWave();

		// Only give points if survived 5 waves or more
		// If survived 20 or more waves, multiply by 2, otherwise multiply by 2.5
		final double points = wave >= 20 ? wave * 2 : wave >= 5 ? wave * 2.5 : 0;
		final double totalArenaPoints = points + cache.getArenaPoints();

		if (totalArenaPoints > 0) {
			cache.giveArenaPoints(player, points);

			Messenger.warn(player, "You received " + MathUtil.formatTwoDigits(points) + " points for surviving " + Common.plural(wave, "wave")
					+ ". Points gained in arena: " + totalArenaPoints + " and overall: " + cache.getTotalPoints() + " points.");
		}
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Pluggable
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * @see org.mineacademy.arena.model.Arena#hasLives()
	 */
	@Override
	protected boolean hasLives() {
		return true;
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#hasClasses()
	 */
	@Override
	protected boolean hasClasses() {
		return true;
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Misc
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * @see org.mineacademy.arena.model.Arena#getHeartbeat()
	 */
	@Override
	public MobArenaHeartbeat getHeartbeat() {
		return (MobArenaHeartbeat) super.getHeartbeat();
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#getSettings()
	 */
	@Override
	public MobArenaSettings getSettings() {
		return (MobArenaSettings) super.getSettings();
	}

	/**
	 * @see org.mineacademy.arena.model.Arena#getScoreboard()
	 */
	@Override
	public MobArenaScoreboard getScoreboard() {
		return (MobArenaScoreboard) super.getScoreboard();
	}
}
