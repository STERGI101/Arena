package org.mineacademy.arena.tool;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.menu.tool.BlockTool;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Handles tools that click within an arena.
 */
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ArenaTool<T extends Arena> extends BlockTool {

	/**
	 * The arena class this tool may be used on
	 */
	@Getter
	private final Class<T> arenaType;

	/**
	 * Create a tool that may be used for any arena
	 */
	protected ArenaTool() {
		this.arenaType = null;
	}

	/**
	 * Handles clicking with this tool, automatically fires {@link #onBlockClick(Player, Arena, ClickType, Block)}
	 * and only fires it when the player is editing something
	 *
	 * @see org.mineacademy.fo.menu.tool.BlockTool#onBlockClick(org.bukkit.entity.Player, org.bukkit.event.inventory.ClickType, org.bukkit.block.Block)
	 */
	@Override
	public final void onBlockClick(final Player player, final ClickType click, final Block block) {
		final ArenaPlayer arenaPlayer = ArenaPlayer.getCache(player);

		if (arenaPlayer.getMode() != ArenaJoinMode.EDITING) {
			Messenger.error(player, "To use this tool to select an arena to edit with /arena edit.");

			return;
		}

		if (arenaType != null && !isApplicable(arenaPlayer.getArena())) {
			Messenger.error(player, "This tool is not compatible with this arena!");

			return;
		}

		if (!BlockUtil.isForBlockSelection(block.getType())) {
			Messenger.error(player, "This block cannot be selected.");

			return;
		}

		onBlockClick(player, (T) arenaPlayer.getArena(), click, block);
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.BlockTool#onAirClick(org.bukkit.entity.Player, org.bukkit.event.inventory.ClickType)
	 */
	@Override
	protected final void onAirClick(final Player player, final ClickType click) {
		final ArenaPlayer arenaPlayer = ArenaPlayer.getCache(player);

		if (arenaPlayer.getMode() != ArenaJoinMode.EDITING)
			return;

		if (arenaType != null && !isApplicable(arenaPlayer.getArena()))
			return;

		onAirClick(player, (T) arenaPlayer.getArena(), click);
	}

	/**
	 * Automatically handle hotbar focus for while editing the arena
	 *
	 * @see org.mineacademy.fo.menu.tool.Tool#onHotbarFocused(org.bukkit.entity.Player)
	 */
	@Override
	protected final void onHotbarFocused(final Player player) {
		final Arena arena = getEditedArena(player);

		if (arena != null && isApplicable(arena))
			onHotbarFocused(player, (T) arena);
	}

	/**
	 * Automatically handle hotbar defocus for while editing the arena
	 *
	 * @see org.mineacademy.fo.menu.tool.Tool#onHotbarDefocused(org.bukkit.entity.Player)
	 */
	@Override
	protected final void onHotbarDefocused(final Player player) {
		final Arena arena = getEditedArena(player);

		if (arena != null && isApplicable(arena))
			onHotbarDefocused(player, (T) arena);

	}

	/**
	 * Handles a click when a player holding this tool clicks inside of their
	 *
	 * @param player
	 * @param arena
	 * @param click
	 * @param block
	 */
	protected abstract void onBlockClick(Player player, T arena, ClickType click, Block block);

	/**
	 * Fired when the player clicks an air in an arena
	 *
	 * @param player
	 * @param arena
	 * @param click
	 */
	protected void onAirClick(final Player player, final T arena, final ClickType click) {
	}

	/**
	 * Called when the player that edits the given arena focuses his hotbar on this tool
	 *
	 * @param player
	 * @param arena
	 */
	protected void onHotbarFocused(final Player player, final T arena) {
	}

	/**
	 * Called when the player that edits the given arena defocuses his hotbar having this tool
	 *
	 * @param player
	 * @param arena
	 */
	protected void onHotbarDefocused(final Player player, final T arena) {
	}

	/**
	 * Called automatically when the player starts editing the given arena
	 *
	 * @param player
	 * @param arena
	 */
	public void onEditStart(final Player player, final T arena) {
	}

	/**
	 * Called automatically when the player stops editing the given arena
	 *
	 * @param player
	 * @param arena
	 */
	public void onEditStop(final Player player, final T arena) {
	}

	/**
	 * Return if this tool can be used in the given arena
	 *
	 * @param arena
	 * @return
	 */
	public boolean isApplicable(final Arena arena) {
		return arenaType == null || arenaType.isAssignableFrom(arena.getClass());
	}

	/*
	 * Return the arena that the player is currently editing
	 * or null if none
	 */
	private Arena getEditedArena(final Player player) {
		final ArenaPlayer arenaPlayer = ArenaPlayer.getCache(player);

		return arenaPlayer.hasArena() && arenaPlayer.getMode() == ArenaJoinMode.EDITING ? arenaPlayer.getArena() : null;
	}
}
