package org.mineacademy.arena.command;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaManager;
import org.mineacademy.fo.Common;

/**
 * The command to create new arenas in game
 */
public class NewCommand extends ArenaSubCommand {

	protected NewCommand() {
		super("new|n", 2, "<type> <arena>", "Create a new arena.");
	}

	@Override
	protected void onCommand() {
		checkConsole();
		checkNotInArena();

		final String type = args[0];
		checkBoolean(ArenaManager.hasArenaType(type), Common.format("Invalid arena type. Available: %s", ArenaManager.getArenaTypes()));

		final String name = args[1];
		checkArenaNotLoaded(name);

		final Arena arena = ArenaManager.loadOrCreateArena(name, type);

		// Automatically put player into edit mode after he creates a new arena
		arena.joinPlayer(getPlayer(), ArenaJoinMode.EDITING);

		tellSuccess("Created " + type + " arena " + arena.getName() + ". Use /{label} tools to edit it now.");
	}

	@Override
	protected List<String> tabComplete() {

		if (args.length == 1)
			return completeLastWord(ArenaManager.getArenaTypes());

		return new ArrayList<>();
	}
}
