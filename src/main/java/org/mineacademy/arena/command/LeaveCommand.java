package org.mineacademy.arena.command;

import java.util.ArrayList;
import java.util.List;

import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaLeaveReason;
import org.mineacademy.arena.model.ArenaManager;

/**
 * The command to leave arenas when playing
 */
public class LeaveCommand extends ArenaSubCommand {

	protected LeaveCommand() {
		super("leave|l", "Leave an arena.");
	}

	@Override
	protected void onCommand() {
		checkConsole();
		checkInArena();

		final Arena arena = ArenaManager.findArena(getPlayer());
		arena.leavePlayer(getPlayer(), ArenaLeaveReason.COMMAND);
	}

	/**
	 * @see org.mineacademy.arena.command.ArenaSubCommand#tabComplete()
	 */
	@Override
	protected List<String> tabComplete() {
		return new ArrayList<>();
	}
}
