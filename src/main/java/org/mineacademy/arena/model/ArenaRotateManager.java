package org.mineacademy.arena.model;

import java.util.List;

import org.bukkit.entity.Player;
import org.mineacademy.arena.settings.Settings;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.remain.Remain;

import lombok.experimental.UtilityClass;

/**
 * Handles automatic arena rotation
 */
@UtilityClass
public final class ArenaRotateManager {

	/**
	 * The arena that is being played right now
	 */
	private Arena currentArena = null;

	/**
	 * When the player joins, start the rotation or join him into spectate
	 * mode
	 *
	 * @param player
	 */
	public void onPlayerJoin(Player player) {
		if (!isEnabled())
			return;

		if (!checkArenasInstalled())
			return;

		startIfStopped();

		final Arena arena = getArena();

		if (arena.getState() == ArenaState.EDITED) {
			player.teleport(arena.getSettings().getLobbyLocation());

			BoxedMessage.tell(player, "<center>&6Arena is being edited right now, please wait in the lobby.");
		}

		else if (arena.getState() == ArenaState.PLAYED) {
			final boolean success = arena.joinPlayer(player, ArenaJoinMode.SPECTATING);
			Valid.checkBoolean(success, "Could not join " + player.getName() + " to spectate " + arena.getName());

			BoxedMessage.tell(player, "<center>&6Arena is being played right now, you are now spectating.");
		}

		else
			arena.joinPlayer(player, ArenaJoinMode.PLAYING);
	}

	/*
	 * Starts the first arena in the list when there is no arena
	 */
	private void startIfStopped() {
		Valid.checkBoolean(!Settings.Rotate.ARENAS.isEmpty(), "Arenas to rotate cannot be empty!");

		if (currentArena == null) {
			final String name = Settings.Rotate.ARENAS.get(0);
			currentArena = ArenaManager.findArena(name);

			Valid.checkNotNull(currentArena, "Could not find arena '" + name + "' from your settings.yml in Rotate.Arenas. Ensure it is created, set up and loaded. Loaded arenas: " + ArenaManager.getArenaNames());
		}
	}

	/**
	 * When the player leaves the arena, teleport to a safe location
	 * By default that is the current arena lobby
	 *
	 * This is only called when the player could not automatically spectate the arena
	 *
	 * @param player
	 */
	public void onArenaLeave(Player player) {
		if (!isEnabled())
			return;

		final Arena arena = getArena();

		arena.teleport(player, arena.getSettings().getLobbyLocation());

		if (!arena.isStopping() && !arena.isStopped())
			BoxedMessage.tell(player,
					"<center>&6&lLOBBY",
					"&r",
					"<center>&7You have been teleported to the lobby.",
					"<center>&7New game will start when the arena finishes.");
	}

	/**
	 * When the arena stops, start the next arena automatically
	 */
	public void onArenaStop() {
		if (!isEnabled())
			return;

		if (!checkArenasInstalled())
			return;

		startIfStopped();
		rotateArena();

		final Arena arena = getArena();
		BoxedMessage.broadcast(
				"<center>&6&lNEW GAME",
				"&r",
				"<center>&7Starting " + arena.getName() + " in " + Settings.Rotate.DELAY_BETWEEN_ARENAS.getRaw());

		Common.runLater(Settings.Rotate.DELAY_BETWEEN_ARENAS.getTimeTicks(), () -> {
			for (final Player otherPlayer : Remain.getOnlinePlayers())
				arena.joinPlayer(otherPlayer, ArenaJoinMode.PLAYING);
		});
	}

	private boolean checkArenasInstalled() {
		if (ArenaManager.getArenas().isEmpty()) {
			BoxedMessage.broadcast(
					"<center>&c&lNO GAME COULD BE STARTED",
					"&r",
					"<center>&7There are no arenas installed on the server to play.");

			Common.log("Warning: Could not rotate arenas automatically since no arenas are installed on the server.");

			return false;
		}

		return true;
	}

	/*
	 * Automatically pick a next arena in the list
	 */
	private void rotateArena() {
		final List<String> arenas = Settings.Rotate.ARENAS;
		final String next = Common.getNext(getArena().getName(), arenas, true);

		final Arena nextArena = ArenaManager.findArena(next);
		Valid.checkNotNull(nextArena, "Arena " + next + " is not installed on this server!");

		currentArena = nextArena;
	}

	/*
	 * Return true if the rotate mode is enabled
	 */
	private boolean isEnabled() {
		return Settings.Rotate.ENABLED;
	}

	/*
	 * Return the current rotated arena
	 */
	private Arena getArena() {
		Valid.checkNotNull(currentArena, "Current arena cannot be null");

		return currentArena;
	}
}
