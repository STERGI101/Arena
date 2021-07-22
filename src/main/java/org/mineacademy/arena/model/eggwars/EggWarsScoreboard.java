package org.mineacademy.arena.model.eggwars;

import org.bukkit.entity.Player;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaScoreboard;
import org.mineacademy.fo.model.Replacer;

/**
 * Represents a scoreboard for eggwars arena
 */
public class EggWarsScoreboard extends ArenaScoreboard {

	/**
	 * Create a new scoreboard
	 *
	 * @param arena
	 */
	public EggWarsScoreboard(final Arena arena) {
		super(arena);
	}

	/**
	 * Replace variables on the table
	 */
	@Override
	protected String replaceVariablesLate(final Player player, String message) {
		final EggWarsSettings settings = getArena().getSettings();

		message = Replacer.of(message).replaceAll(
				"spawnpoints", settings.getEntrances().size(),
				"eggs", settings.getEggs().size(),
				"villagers", settings.getVillagers().size(),
				"iron", settings.getIron().size(),
				"gold", settings.getGold().size(),
				"diamonds", settings.getDiamonds().size());

		return message;
	}

	/**
	 * @see org.mineacademy.arena.model.ArenaScoreboard#addEditRows()
	 */
	@Override
	protected void addEditRows() {
		addRows(
				"Player spawnpoints: {spawnpoints}",
				"Egg locations: {eggs}",
				"Villagers: {villagers}",
				"Iron spawners: {iron}",
				"Gold spawners: {gold}",
				"Diamond spawners: {diamonds}");
	}

	/**
	 * @see org.mineacademy.arena.model.ArenaScoreboard#getArena()
	 */
	@Override
	public EggWarsArena getArena() {
		return (EggWarsArena) super.getArena();
	}
}
