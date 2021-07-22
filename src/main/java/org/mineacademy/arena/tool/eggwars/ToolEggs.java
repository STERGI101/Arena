package org.mineacademy.arena.tool.eggwars;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.model.eggwars.EggWarsSettings;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.YamlConfig.LocationList;

import lombok.Getter;

/**
 *
 */
public class ToolEggs extends ToolEggWars {

	@Getter
	private static final Tool instance = new ToolEggs();

	private ToolEggs() {
		super("Egg", CompMaterial.COAL_BLOCK);

		setLimitToPlayerMaximum(true);
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.of(
				CompMaterial.DRAGON_EGG,
				"EGG SPAWNPOINT",
				"Click to toggle where",
				"eggs for players will appear.",
				"",
				"We will pair the closest",
				"egg with player spawnpoint",
				"automatically.")
				.build().make();
	}

	/**
	 * @see org.mineacademy.arena.tool.eggwars.ToolEggWars#getLocations(org.mineacademy.arena.model.eggwars.EggWarsSettings)
	 */
	@Override
	protected LocationList getLocations(EggWarsSettings settings) {
		return settings.getEggs();
	}
}
