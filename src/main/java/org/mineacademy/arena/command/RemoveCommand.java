package org.mineacademy.arena.command;

import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaManager;

/*
 * A command to permanently remove arenas
 */
public class RemoveCommand extends ArenaSubCommand {

	protected RemoveCommand() {
		super("remove|rm", 1, "<arena>", "Remove an new arena.");
	}

	@Override
	protected void onCommand() {
		final String name = args[0];
		final Arena arena = findArena(name);

		ArenaManager.removeArena(arena);
		tellSuccess("Arena " + name + " has been removed successfully.");
	}
}
