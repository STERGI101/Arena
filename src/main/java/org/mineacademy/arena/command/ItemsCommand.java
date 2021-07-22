package org.mineacademy.arena.command;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.arena.item.ExplosiveBow;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaPlayer;

/**
 * The command to obtain arena items
 */
public class ItemsCommand extends ArenaSubCommand {

	protected ItemsCommand() {
		super("items|i", "Goodies for playing arenas.");
	}

	@Override
	protected void onCommand() {
		checkConsole();

		final ArenaPlayer cache = ArenaPlayer.getCache(getPlayer());
		checkBoolean(!cache.hasArena() || cache.getMode() == ArenaJoinMode.EDITING, "You may only use this command while not playing, or editing an arena.");

		ExplosiveBow.getInstance().give(getPlayer());
		tell("You have been given some tools...");
	}

	@Override
	protected List<String> tabComplete() {
		return new ArrayList<>();
	}
}
