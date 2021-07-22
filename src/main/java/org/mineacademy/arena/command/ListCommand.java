package org.mineacademy.arena.command;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.arena.model.ArenaManager;
import org.mineacademy.fo.Common;

/**
 * A command to automatically list all loaded arenas
 */
public class ListCommand extends ArenaSubCommand {

	protected ListCommand() {
		super("list", "List available arenas.");
	}

	@Override
	protected void onCommand() {
		checkConsole();

		tellInfo(Common.format("Available arenas: %s", ArenaManager.getArenaNames()));
	}

	@Override
	protected List<String> tabComplete() {
		return new ArrayList<>();
	}
}
