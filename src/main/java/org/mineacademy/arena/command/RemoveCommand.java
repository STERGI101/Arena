package org.mineacademy.arena.command;

import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaManager;
import org.mineacademy.arena.settings.Localization;

/*
 * A command to permanently remove arenas
 */
public class RemoveCommand extends ArenaSubCommand {

	protected RemoveCommand() {
		super("remove|rm", 1, "<arena>", Localization.Commands.REMOVE_DESCRIPTION);
	}

	@Override
	protected void onCommand() {
		final String name = args[0];
		final Arena arena = findArena(name);

		ArenaManager.removeArena(arena);
		tellSuccess(
				Localization.Commands.REMOVE_SUCCESS
				.find("Arenaname")
				.replace(name)
				.getReplacedMessageJoined());
	}
}
