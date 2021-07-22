package org.mineacademy.arena.tool;

import java.util.ArrayList;
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
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.visual.VisualizedRegion;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the tool used to create arena region for any arena
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolRegion extends VisualTool<Arena> {

	@Getter
	private static final Tool instance = new ToolRegion();

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getBlockName(org.bukkit.block.Block, org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected String getBlockName(final Block block, final Player player, final Arena arena) {
		return "[&aRegion point&f]";
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getBlockMask(org.bukkit.block.Block, org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected CompMaterial getBlockMask(final Block block, final Player player, final Arena arena) {
		return CompMaterial.EMERALD_BLOCK;
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.of(
				CompMaterial.EMERALD,
				"&lREGION TOOL",
				"",
				"Use to set region points",
				"for an edited arena.",
				"",
				"&b<< &fLeft click &7– &fPrimary",
				"&fRight click &7– &fSecondary &b>>")
				.build().makeMenuTool();
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#handleBlockClick(org.bukkit.entity.Player, org.mineacademy.arena.model.Arena, org.bukkit.event.inventory.ClickType, org.bukkit.block.Block)
	 */
	@Override
	protected void handleBlockClick(final Player player, final Arena arena, final ClickType click, final Block block) {
		final ArenaSettings settings = arena.getSettings();

		final Location location = block.getLocation();
		final boolean primary = click == ClickType.LEFT;

		if (primary)
			settings.setRegion(location, null);
		else
			settings.setRegion(null, location);

		Messenger.success(player, "Set the " + (primary ? "primary" : "secondary") + " arena point.");
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getVisualizedPoints(org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected List<Location> getVisualizedPoints(final Arena arena) {
		final List<Location> blocks = new ArrayList<>();
		final Region region = arena.getSettings().getRegion();

		if (region != null) {
			if (region.getPrimary() != null)
				blocks.add(region.getPrimary());

			if (region.getSecondary() != null)
				blocks.add(region.getSecondary());
		}

		return blocks;
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getVisualizedRegion(org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected VisualizedRegion getVisualizedRegion(final Arena arena) {
		return arena.getSettings().getRegion();
	}

	@Override
	protected boolean autoCancel() {
		return true; // Cancel the event so that we don't destroy blocks when selecting them
	}
}
