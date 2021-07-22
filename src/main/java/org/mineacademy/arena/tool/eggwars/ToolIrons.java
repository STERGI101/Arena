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
public class ToolIrons extends ToolEggWars {

	@Getter
	private static final Tool instance = new ToolIrons();

	private ToolIrons() {
		super("Iron", CompMaterial.IRON_BLOCK);
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.of(
				CompMaterial.IRON_ORE,
				"IRON SPAWNPOINT",
				"Click to toggle where",
				"iron will appear.")
				.build().make();
	}

	/**
	 * @see org.mineacademy.arena.tool.eggwars.ToolEggWars#getLocations(org.mineacademy.arena.model.eggwars.EggWarsSettings)
	 */
	@Override
	protected LocationList getLocations(EggWarsSettings settings) {
		return settings.getIron();
	}
}
