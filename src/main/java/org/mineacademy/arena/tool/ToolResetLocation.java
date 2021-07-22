package org.mineacademy.arena.tool;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaSettings;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolResetLocation extends VisualTool<Arena> {

	@Getter
	private static final Tool instance = new ToolResetLocation();

	@Override
	protected String getBlockName(final Block block, final Player player, final Arena arena) {
		return "[Reset point]";
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getBlockMask(org.bukkit.block.Block, org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected CompMaterial getBlockMask(final Block block, final Player player, final Arena arena) {
		return CompMaterial.IRON_BLOCK;
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.of(
				CompMaterial.IRON_INGOT,
				"&lRESET POINT TOOL",
				"",
				"Right click to set an",
				"arena reset point.")
				.build().makeMenuTool();
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#handleBlockClick(org.bukkit.entity.Player, org.mineacademy.arena.model.Arena, org.bukkit.event.inventory.ClickType, org.bukkit.block.Block)
	 */
	@Override
	protected void handleBlockClick(final Player player, final Arena arena, final ClickType click, final Block block) {
		final ArenaSettings settings = arena.getSettings();
		final Location location = block.getLocation();

		if (!settings.isWorldResetEnabled()) {
			Messenger.error(player, "Arena " + arena.getName() + " has world reset disabled!");

			return;
		}

		if (location.getWorld().getName().equals(settings.getRegion().getWorld().getName())) {
			Messenger.error(player, "The reset point must be in another world!");

			return;
		}

		location.setYaw(player.getLocation().getYaw());
		location.setPitch(0);

		settings.setResetLocation(location);

		Messenger.success(player, "Set the point where players are moved before world restore for arena " + arena.getName() + ".");
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getVisualizedPoints(org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected List<Location> getVisualizedPoints(final Arena arena) {
		return Arrays.asList(arena.getSettings().getResetLocation());
	}

	/**
	 * @see org.mineacademy.arena.tool.ArenaTool#isApplicable(org.mineacademy.arena.model.Arena)
	 */
	@Override
	public boolean isApplicable(Arena arena) {
		return super.isApplicable(arena) && arena.getSettings().isWorldResetEnabled();
	}

	@Override
	protected boolean autoCancel() {
		return true; // Cancel the event so that we don't destroy blocks when selecting them
	}
}
