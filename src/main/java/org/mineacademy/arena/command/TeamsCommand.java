package org.mineacademy.arena.command;

import org.bukkit.entity.Player;
import org.mineacademy.arena.menu.TeamSelectionMenu;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.arena.model.ArenaState;
import org.mineacademy.arena.model.team.TeamArena;

/**
 * Team selection or edit menu
 */
public class TeamsCommand extends ArenaSubCommand {

	protected TeamsCommand() {
		super("teams|team|tm", "Select or edit teams.");
	}

	@Override
	protected void onCommand() {
		checkConsole();

		final Player player = getPlayer();
		final ArenaPlayer cache = ArenaPlayer.getCache(player);
		final ArenaJoinMode mode = cache.getMode();

		if (cache.hasArena() && mode != ArenaJoinMode.EDITING) {
			final Arena arena = cache.getArena();

			checkBoolean(mode == ArenaJoinMode.PLAYING && arena.getState() == ArenaState.LOBBY, "You may only select teams in the lobby.");
			checkBoolean(arena instanceof TeamArena, "You cannot select teams in this arena.");

			TeamSelectionMenu.openSelectMenu(getPlayer(), (TeamArena) arena);

		} else {
			checkBoolean(player.isOp(), "You don't have permission to edit teams.");

			TeamSelectionMenu.openEditMenu(null, getPlayer());
		}
	}
}
