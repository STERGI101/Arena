package org.mineacademy.arena.model.eggwars;

import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.util.Vector;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaHeartbeat;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.YamlConfig.LocationList;

/**
 * Heartbeat for the eggwars arena handling spawning of items
 */
public class EggWarsHeartbeat extends ArenaHeartbeat {

	private int tier = 1;

	/**
	 * Create a new heart beat
	 *
	 * @param arena
	 */
	protected EggWarsHeartbeat(final Arena arena) {
		super(arena);
	}

	/**
	 * @see org.mineacademy.arena.model.ArenaHeartbeat#onTick()
	 */
	@Override
	protected void onTick() {
		super.onTick();

		final EggWarsSettings settings = getArena().getSettings();
		final int elapsedSeconds = getCountdownSeconds() - getTimeLeft();

		if (elapsedSeconds % 60 * 5 == 0) // every 5 minutes there is an upgrade in how many items are spawned
			tier++;

		// Drop iron
		if (elapsedSeconds % 2 == 0)
			dropItems(settings.getIron(), tier, ItemCreator.of(
					CompMaterial.IRON_INGOT,
					"Iron Ingot",
					"",
					"Use this to buy items!"));

		// Drop gold
		if (elapsedSeconds % 4 == 0)
			dropItems(settings.getGold(), tier, ItemCreator.of(
					CompMaterial.GOLD_INGOT,
					"Gold Ingot",
					"",
					"Use this to buy better items!"));

		// Drop diamonds
		if (elapsedSeconds % 8 == 0)
			dropItems(settings.getDiamonds(), tier, ItemCreator.of(
					CompMaterial.DIAMOND,
					"Diamond",
					"",
					"Use this to buy the best items!"));
	}

	/*
	 * Drop items at the given locations 1 block above
	 */
	private void dropItems(LocationList items, int amountToSpawn, ItemCreator.ItemCreatorBuilder item) {
		for (final Location point : items) {
			final Item droppedItem = point.getWorld().dropItem(point.clone().add(0.5, 1, 0.5), item.amount(amountToSpawn).build().make());

			droppedItem.setVelocity(new Vector(0, 0, 0));
		}
	}

	/**
	 * @see org.mineacademy.arena.model.ArenaHeartbeat#getArena()
	 */
	@Override
	public EggWarsArena getArena() {
		return (EggWarsArena) super.getArena();
	}
}
