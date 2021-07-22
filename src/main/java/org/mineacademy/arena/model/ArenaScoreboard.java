package org.mineacademy.arena.model;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.model.SimpleScoreboard;

import lombok.Getter;

/**
 * A simple arena scoreboard
 */
public class ArenaScoreboard extends SimpleScoreboard {

	/**
	 * The arena
	 */
	@Getter
	private final Arena arena;

	/**
	 * Create a new scoreboard
	 *
	 * @param arena
	 */
	public ArenaScoreboard(final Arena arena) {
		this.arena = arena;

		this.setTitle("&8- &7Arena &c" + arena.getName() + " &8-");
		this.setTheme(ChatColor.RED, ChatColor.GRAY);
		this.setUpdateDelayTicks(20 /* 1 second */);
	}

	/**
	 * @see org.mineacademy.fo.model.SimpleScoreboard#replaceVariables(java.lang.String)
	 */
	@Override
	protected String replaceVariables(final Player player, String message) {
		final ArenaSettings settings = arena.getSettings();

		message = Replacer.of(message).replaceAll(
				"remaining_start", Common.plural(arena.getStartCountdown().getTimeLeft(), "second"),
				"remaining_end", Common.plural(arena.getHeartbeat().getTimeLeft(), "second"),
				"players", arena.getPlayers(arena.getState() == ArenaState.EDITED ? ArenaJoinMode.EDITING : ArenaJoinMode.PLAYING).size(),
				"state", arena.getState().getLocalized(),
				"lobby_set", settings.getLobbyLocation() != null,
				"region_set", settings.getRegion() != null && settings.getRegion().isWhole(),
				"reset_set", settings.getResetLocation() != null);

		message = replaceVariablesLate(player, message);

		return message.replace("_true", "&ayes").replace("_false", "&4no");
	}

	/**
	 * If you decide to continue to use {@link #replaceVariables(String)}
	 * you could just extend this to preserve the yes/no colored lines
	 *
	 * @param player
	 * @param message
	 * @return
	 */
	protected String replaceVariablesLate(final Player player, final String message) {
		return message;
	}

	/**
	 * Called automatically when the player joins
	 *
	 * @param player
	 * @param joinMode
	 */
	public void onPlayerJoin(final Player player, final ArenaJoinMode joinMode) {
		show(player);
	}

	/**
	 * Called on player leave
	 *
	 * @param player
	 */
	public void onPlayerLeave(final Player player) {
		if (isViewing(player))
			hide(player);
	}

	/**
	 * Called automatically on lobby start
	 */
	public void onLobbyStart() {
		addRows("",
				"Players: {players}",
				"Time to start: {remaining_start}");
	}

	/**
	 * Called automatically when the first player stars to edit the arena
	 */
	public void onEditStart() {
		addRows("",
				"Editing players: {players}",
				"",
				"Lobby: _{lobby_set}",
				"Region: _{region_set}");

		if (arena.getSettings().isWorldResetEnabled())
			addRows("Reset location: _{reset_set}");

		addEditRows();
		addRows("",
				"&7Use: /arena tools to edit.");
	}

	/**
	 * Called in the middle of adding scoreboard rows to the table rendered
	 * at edit, can be handy if you just need to add a few more lines
	 */
	protected void addEditRows() {
	}

	/**
	 * Called automatically when the game starts
	 */
	public void onStart() {
		removeRow("Time to start");
		addRows("Time left: {remaining_end}");
	}

	/**
	 * Called on arena stop
	 */
	public void onStop() {
		clearRows();

		stop();
	}
}
