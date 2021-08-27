package org.mineacademy.arena.command;

import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaManager;
import org.mineacademy.arena.settings.Localization;

import java.util.ArrayList;
import java.util.List;

/**
 * The command to create new arenas in game
 */
public class NewCommand extends ArenaSubCommand {

	protected NewCommand() {
		super("new|n", 2, "<type> <arena>", Localization.Commands.NEW_DESCRIPTION);
	}

	@Override
	protected void onCommand() {
		checkConsole();
		checkNotInArena();

		final String type = args[0];
		checkBoolean(ArenaManager.hasArenaType(type),
				Localization.Commands.NEW_ERROR
				.find("Arenatypes")
				.replace(ArenaManager.getArenaTypes())
				.getReplacedMessageJoined());

		final String name = args[1];
		checkArenaNotLoaded(name);

		final Arena arena = ArenaManager.loadOrCreateArena(name, type);

		// Automatically put player into edit mode after he creates a new arena
		arena.joinPlayer(getPlayer(), ArenaJoinMode.EDITING);

		tellSuccess(
				Localization.Commands.NEW_SUCCESS
				.find("Arenatype", "Arenaname")
				.replace(type, arena.getName())
				.getReplacedMessageJoined());
	}

	@Override
	protected List<String> tabComplete() {

		if (args.length == 1)
			return completeLastWord(ArenaManager.getArenaTypes());

		return new ArrayList<>();
	}
}
