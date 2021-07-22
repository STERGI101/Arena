package org.mineacademy.arena.command;

import org.bukkit.entity.Player;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaManager;
import org.mineacademy.fo.remain.Remain;

/**
 * The command to join existing arenas
 * The first player joining in play mode will start the countdown
 */
public class JoinCommand extends ArenaSubCommand {

	protected JoinCommand() {
		super("join|j", "Go play an arena.");

		setUsage("[arena]");
	}

	@Override
	protected void onCommand() {
		checkConsole();

		Arena arena = null;

		// Automatically find arena by players location if he only types /arena join
		if (args.length == 0) {
			arena = ArenaManager.findArena(getPlayer().getLocation());

			checkNotNull(arena, "Could not find an arena to join, please specify its name.");
		} else {
			arena = findArena(args[0]);

			// Testing command: use /arena join <arena> all to join all players to an arena
			if (args.length == 2 && "all".equals(args[1])) {
				for (final Player player : Remain.getOnlinePlayers())
					arena.joinPlayer(player, ArenaJoinMode.PLAYING);

				return;
			}
		}

		arena.joinPlayer(getPlayer(), ArenaJoinMode.PLAYING);
	}
}
