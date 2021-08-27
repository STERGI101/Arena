package org.mineacademy.arena.command;

import org.mineacademy.arena.model.ArenaManager;
import org.mineacademy.arena.settings.Localization;

import java.util.ArrayList;
import java.util.List;

/**
 * A command to automatically list all loaded arenas
 */
public class ListCommand extends ArenaSubCommand {

	protected ListCommand() {
		super("list", Localization.Commands.LIST_DESCRIPTION);
	}

	@Override
	protected void onCommand() {
		checkConsole();

		tellInfo(
				Localization.Commands.LIST_ERROR
				.find("Arenanames")
				.replace(ArenaManager.getArenaNames())
				.getReplacedMessageJoined());
	}

	@Override
	protected List<String> tabComplete() {
		return new ArrayList<>();
	}
}
