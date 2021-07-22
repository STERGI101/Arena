package org.mineacademy.arena.tool.eggwars;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.arena.model.eggwars.EggWarsArena;
import org.mineacademy.arena.model.eggwars.EggWarsSettings;
import org.mineacademy.arena.tool.VisualTool;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.YamlConfig.LocationList;

import lombok.AccessLevel;
import lombok.Setter;

@Setter(value = AccessLevel.PROTECTED)
public abstract class ToolEggWars extends VisualTool<EggWarsArena> {

	private final String blockName;
	private final CompMaterial blockMask;
	private boolean limitToPlayerMaximum = false;

	/**
	 * Create new tool for the arena type
	 */
	protected ToolEggWars(String blockName, CompMaterial blockMask) {
		super(EggWarsArena.class);

		this.blockName = blockName;
		this.blockMask = blockMask;
	}

	@Override
	protected String getBlockName(final Block block, final Player player, final EggWarsArena arena) {
		return blockName;
	}

	@Override
	protected CompMaterial getBlockMask(final Block block, final Player player, final EggWarsArena arena) {
		return blockMask;
	}

	@Override
	protected void handleBlockClick(final Player player, final EggWarsArena arena, final ClickType click, final Block block) {
		final EggWarsSettings settings = arena.getSettings();
		final LocationList locations = getLocations(settings);
		final String name = blockName.toLowerCase();

		int left = MathUtil.range(settings.getMaxPlayers() - locations.size(), 0, Integer.MAX_VALUE);

		if (limitToPlayerMaximum && !locations.hasLocation(block.getLocation()) && left == 0) {
			Messenger.warn(player, "Cannot place more " + name + " than max player count. Remove some.");

			return;
		}

		final boolean added = locations.toggle(block.getLocation());
		left = added ? left - 1 : left + 1;

		Messenger.success(player, "Successfully " + (added ? "&2added&7" : "&cremoved&7") + " " + name + " spawn point."
				+ (limitToPlayerMaximum ? (left > 0 ? " Create " + left + " more to match the max player count." : " All necessary " + name + " set.") : ""));
	}

	protected abstract LocationList getLocations(EggWarsSettings settings);

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getVisualizedPoints(org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected List<Location> getVisualizedPoints(EggWarsArena arena) {
		return getLocations(arena.getSettings()).getLocations();
	}

	@Override
	protected boolean autoCancel() {
		return true; // Cancel the event so that we don't destroy blocks when selecting them
	}
}
