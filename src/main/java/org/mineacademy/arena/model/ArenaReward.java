package org.mineacademy.arena.model;

import java.util.List;
import java.util.Map;

import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.util.Constants;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class ArenaReward extends YamlConfig {

	/**
	 * The singleton instance for rewards file
	 */
	@Getter
	private final static ArenaReward instance = new ArenaReward();

	/**
	 * A list of item rewards
	 */
	private List<ArenaRewardItem> itemRewards;

	/**
	 * Create and load the rewards fle
	 */
	private ArenaReward() {
		loadConfiguration(NO_DEFAULT, "rewards.yml");
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#onLoadFinish()
	 */
	@Override
	protected void onLoadFinish() {
		this.itemRewards = getList("Item_Rewards", ArenaRewardItem.class, this);
	}

	/**
	 * Set the item rewards from the given item stacks, preserving old prices
	 *
	 * @param items
	 */
	public void setItemRewards(final ItemStack[] items) {

		for (int i = 0; i < items.length; i++) {
			final Double price = getItemPrice(i);
			ArenaRewardItem newItem = new ArenaRewardItem(this, items[i], price == null ? Constants.Defaults.ITEM_REWARD_PRICE : price);
			newItem = newItem.getItem() != null ? newItem : null;

			if (itemRewards.size() >= i + 1)
				itemRewards.set(i, newItem);

			else
				itemRewards.add(newItem);
		}

		save();
	}

	/**
	 * Remove all item rewards
	 */
	public void clearItemRewards() {
		itemRewards.clear();

		save();
	}

	/**
	 * Get item reward at the given slot
	 *
	 * @param slot
	 * @return
	 */
	public ArenaRewardItem getItemReward(final int slot) {
		return itemRewards.size() >= slot + 1 ? itemRewards.get(slot) : null;
	}

	/**
	 * Get item price, or null if the slot does not have a reward
	 *
	 * @param slot
	 * @return
	 */
	public Double getItemPrice(final int slot) {
		final ArenaRewardItem item = getItemReward(slot);

		return item != null ? item.getPrice() : null;
	}

	@Override
	public void save() {
		final SerializedMap map = SerializedMap.ofArray("Item_Rewards", itemRewards);

		for (final Map.Entry<String, Object> entry : map.entrySet())
			setNoSave(entry.getKey(), entry.getValue());

		super.save();
	}

	// --------------------------------------------------------------------------------------------------------------
	// Classes
	// --------------------------------------------------------------------------------------------------------------

	/**
	 * Represents a single reward itemstack
	 */
	@AllArgsConstructor(access = AccessLevel.PRIVATE)
	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	public final static class ArenaRewardItem implements ConfigSerializable {

		/**
		 * The parent class
		 */
		private final ArenaReward arenaReward;

		/**
		 * The item
		 */
		@Getter
		private ItemStack item;

		/**
		 * The price
		 */
		@Getter
		private double price = Constants.Defaults.ITEM_REWARD_PRICE;

		/**
		 * @param item the item to set
		 */
		public void setItem(final ItemStack item) {
			this.item = item;

			arenaReward.save();
		}

		/**
		 * @param price the price to set
		 */
		public void setPrice(final double price) {
			this.price = price;

			arenaReward.save();
		}

		public static ArenaRewardItem deserialize(final SerializedMap map, final ArenaReward arenaReward) {
			final ArenaRewardItem item = new ArenaRewardItem(arenaReward);

			item.item = map.getItem("Item");
			item.price = map.getDouble("Price");

			return item;
		}

		/**
		 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
		 */
		@Override
		public SerializedMap serialize() {
			return SerializedMap.ofArray(
					"Item", item,
					"Price", price);
		}
	}
}
