package org.mineacademy.arena.command;

import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaManager;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.arena.settings.Localization;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.SimpleSubCommand;

import java.util.List;

/**
 * A helper class for /arena sub commands
 */
public abstract class ArenaSubCommand extends SimpleSubCommand {

	/**
	 * Create a new arena sub command with a description
	 *
	 * @param sublabel
	 * @param description
	 */
	protected ArenaSubCommand(String sublabel, final String description) {
		this(sublabel, 0, "", description);
	}

	/**
	 * Create a new arena sub command and automatically register its arguments
	 *
	 * @param sublabel
	 * @param minArguments
	 * @param usage
	 * @param description
	 */
	protected ArenaSubCommand(final String sublabel, final int minArguments, final String usage, final String description) {
		super(sublabel);

		setMinArguments(minArguments);
		setUsage(usage);
		setDescription(description);
	}

	/**
	 * Get an arena by its name, or send an error message to the player
	 * if no arena by the given name exists
	 *
	 * @param name
	 * @return
	 */
	protected final Arena findArena(final String name) {
		final Arena arena = ArenaManager.findArena(name);
		checkNotNull(arena, Localization.Commands.FIND_ARENA_ERROR
				.find("input", "Arenanames")
				.replace(name, ArenaManager.getArenaNames())
				.getReplacedMessageJoined());

		return arena;
	}

	/**
	 * Check if the command sender is a player that is not joined in any arena
	 */
	protected final void checkInArena() {
		checkConsole();

		final ArenaPlayer cache = ArenaPlayer.getCache(getPlayer());
		checkBoolean(cache.hasArena(), Localization.Commands.PLAYER_NOT_IN_ARENA);
	}

	/**
	 * Check if the command sender is a player that is not joined in any arena
	 */
	protected final void checkNotInArena() {
		checkConsole();

		final ArenaPlayer cache = ArenaPlayer.getCache(getPlayer());

		if (cache.hasArena()) {
			returnTell(Localization.Commands.PLAYER_IN_ARENAMODE_WHILE_EDITING
					.find("Arenamode", "Arenaname")
					.replace(cache.getMode().getLocalized(), cache.getArena().getName())
					.getReplacedMessage());
		}
	}

	/**
	 * Check if the given arena is loaded and return the command with an error message if not
	 *
	 * @param arenaName
	 */
	protected final void checkArenaLoaded(final String arenaName) {
		checkBoolean(ArenaManager.isArenaLoaded(arenaName), Common.format("Arena %s does not exist. Available: %s", arenaName, ArenaManager.getArenaNames()));
	}

	/**
	 * Check if the given arena is not loaded and return the command with an error message if it is
	 *
	 * @param arenaName
	 */
	protected final void checkArenaNotLoaded(final String arenaName) {
		checkBoolean(!ArenaManager.isArenaLoaded(arenaName), Common.format("Arena %s already exists.", arenaName));
	}

	/**
	 * For your convenience we already tab complete the last word with arena names
	 *
	 * @see org.mineacademy.fo.command.SimpleCommand#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return completeLastWord(ArenaManager.getArenaNames());
	}
}
