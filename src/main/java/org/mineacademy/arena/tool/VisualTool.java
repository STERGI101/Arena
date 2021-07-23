package org.mineacademy.arena.tool;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.visual.BlockVisualizer;
import org.mineacademy.fo.visual.VisualizedRegion;

import java.util.List;

/**
 * A class that can visualize selection of blocks in the arena
 *
 * @param <T>
 */
public abstract class VisualTool<T extends Arena> extends ArenaTool<T> {

	/**
	 * Create a new visual tool
	 *
	 *
	 *
	 */
	protected VisualTool() {
		this(null);
	}

	/**
	 * Create a new visual tool
	 *
	 * @param arenaType
	 */
	protected VisualTool(final Class<T> arenaType) {
		super(arenaType);
	}

	/**
	 * Handle block clicking and automatically refreshes rendering of visualized blocks
	 *
	 * @param player
	 * @param arena
	 * @param click
	 * @param block
	 */
	@Override
	protected final void onBlockClick(final Player player, final T arena, final ClickType click, final Block block) {
		// Remove old blocks
		stopVisualizing(arena, player);

		// Call the block handling, probably new blocks will appear
		handleBlockClick(player, arena, click, block);

		// Render the new blocks
		visualize(arena, player);
	}

	/**
	 * Handles block clicking. Any changes here will be reflected automatically in the visualization
	 *
	 * @param player
	 * @param arena
	 * @param click
	 * @param block
	 */
	protected abstract void handleBlockClick(Player player, T arena, ClickType click, Block block);

	/**
	 * @see org.mineacademy.arena.tool.ArenaTool#onAirClick(org.bukkit.entity.Player, org.mineacademy.arena.model.Arena, org.bukkit.event.inventory.ClickType)
	 */
	@Override
	protected final void onAirClick(final Player player, final T arena, final ClickType click) {
		// Remove old blocks
		stopVisualizing(arena, player);

		// Call the block handling, probably new blocks will appear
		handleAirClick(player, arena, click);

		// Render the new blocks
		visualize(arena, player);
	}

	/**
	 * Handles air clicking and updates visualization automatically
	 *
	 * @param player
	 * @param arena
	 * @param click
	 */
	protected void handleAirClick(final Player player, final T arena, final ClickType click) {
	}

	/**
	 * @see org.mineacademy.arena.tool.ArenaTool#onEditStart(org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	public final void onEditStart(final Player player, final T arena) {
		// Visualize for the current tool if matches
		if (hasToolInHand(player))
			Common.runLater(() -> visualize(arena, player));
	}

	/**
	 * @see org.mineacademy.arena.tool.ArenaTool#onHotbarFocused(org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected final void onHotbarFocused(final Player player, final T arena) {
		visualize(arena, player);
	}

	/**
	 * @see org.mineacademy.arena.tool.ArenaTool#onEditStop(org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	public final void onEditStop(final Player player, final T arena) {
		stopVisualizing(arena, player);
	}

	/**
	 * @see org.mineacademy.arena.tool.ArenaTool#onHotbarDefocused(org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected final void onHotbarDefocused(final Player player, final T arena) {
		stopVisualizing(arena, player);
	}

	/**
	 * Return a list of points we should render in this visualization
	 *
	 * @param arena
	 * @return
	 */
	protected abstract List<Location> getVisualizedPoints(T arena);

	/**
	 * Return a region that this tool should draw particles around
	 *
	 * @param arena
	 * @return
	 */
	protected VisualizedRegion getVisualizedRegion(final T arena) {
		return null;
	}

	/**
	 * Return the name above the glowing block for the given parameters
	 *
	 * @param block
	 * @param player
	 * @param arena
	 * @return
	 */
	protected abstract String getBlockName(Block block, Player player, T arena);

	/**
	 * Return the block mask for the given parameters
	 *
	 * @param block
	 * @param player
	 * @param arena
	 * @return
	 */
	protected abstract CompMaterial getBlockMask(Block block, Player player, T arena);

	/*
	 * Visualize the region and points if exist
	 */
	private void visualize(final T arena, final Player player) {
		final VisualizedRegion region = getVisualizedRegion(arena);

		if (region != null && region.isWhole())
			if (!region.canSeeParticles(player))
				region.showParticles(player);

		for (final Location location : getVisualizedPoints(arena)) {
			if (location == null)
				continue;

			final Block block = location.getBlock();

			BlockVisualizer.visualize(block, getBlockMask(block, player, arena), getBlockName(block, player, arena));
		}
	}

	/*
	 * Stop visualizing region and points if they were so before
	 */
	private void stopVisualizing(final T arena, final Player player) {
		final VisualizedRegion region = getVisualizedRegion(arena);

		if (region != null && region.canSeeParticles(player))
			region.hideParticles(player);

		for (final Location location : getVisualizedPoints(arena)) {
			if (location == null)
				continue;

			final Block block = location.getBlock();

			if (BlockVisualizer.isVisualized(block))
				BlockVisualizer.stopVisualizing(block);
		}
	}
}
