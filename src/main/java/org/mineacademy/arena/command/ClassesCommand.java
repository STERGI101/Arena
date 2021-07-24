package org.mineacademy.arena.command;

import org.bukkit.entity.Player;
import org.mineacademy.arena.settings.Localization;
import org.mineacademy.arena.menu.ClassSelectionMenu;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.arena.model.ArenaState;

/**
 * Class selection menu during lobby
 */
public class ClassesCommand extends ArenaSubCommand {

	protected ClassesCommand() {
		super("classes|cl", Localization.Commands.CLASSES_DESCRIPTION);
	}

	@Override
	protected void onCommand() {
		checkConsole();

		final Player player = getPlayer();
		final ArenaPlayer cache = ArenaPlayer.getCache(player);
		final ArenaJoinMode mode = cache.getMode();

		if (cache.hasArena() && mode != ArenaJoinMode.EDITING) {
			checkBoolean(mode == ArenaJoinMode.PLAYING && cache.getArena().getState() == ArenaState.LOBBY, "You may only select classes in the lobby.");

			ClassSelectionMenu.openSelectMenu(getPlayer(), cache.getArena());

		} else {
			checkBoolean(player.isOp(), "You don't have permission to edit classes.");

			ClassSelectionMenu.openEditMenu(null, getPlayer());
		}
	}
}
