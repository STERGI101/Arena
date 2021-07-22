package org.mineacademy.arena.tool;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.model.monster.MobArena;
import org.mineacademy.arena.model.monster.MobArenaSettings;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.Getter;

/**
 * Represents the tool to create mob arena entering point
 */
public class ToolEntrance extends VisualTool<MobArena> {

	@Getter
	private static final Tool instance = new ToolEntrance();

	private ToolEntrance() {
		super(MobArena.class);
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getBlockName(org.bukkit.block.Block, org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected String getBlockName(final Block block, final Player player, final MobArena arena) {
		return "[&bEntrance point&f]";
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getBlockMask(org.bukkit.block.Block, org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected CompMaterial getBlockMask(final Block block, final Player player, final MobArena arena) {
		return CompMaterial.DIAMOND_BLOCK;
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.of(
				CompMaterial.DIAMOND,
				"&lENTRANCE TOOL",
				"",
				"Right click to place",
				"arena entrance point.")
				.build().makeMenuTool();
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#handleBlockClick(org.bukkit.entity.Player, org.mineacademy.arena.model.Arena, org.bukkit.event.inventory.ClickType, org.bukkit.block.Block)
	 */
	@Override
	protected void handleBlockClick(final Player player, final MobArena arena, final ClickType click, final Block block) {
		final MobArenaSettings settings = arena.getSettings();
		final Location location = block.getLocation();

		location.setYaw(player.getLocation().getYaw());
		location.setPitch(0);

		settings.setEntranceLocation(location);

		Messenger.success(player, "Set the arena entrance point for players.");
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getVisualizedPoints(org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected List<Location> getVisualizedPoints(final MobArena arena) {
		return Arrays.asList(arena.getSettings().getEntranceLocation());
	}

	@Override
	protected boolean autoCancel() {
		return true; // Cancel the event so that we don't destroy blocks when selecting them
	}
}
