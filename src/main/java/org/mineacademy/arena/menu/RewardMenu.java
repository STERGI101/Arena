package org.mineacademy.arena.menu;

import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.arena.model.ArenaReward;
import org.mineacademy.arena.model.ArenaReward.ArenaRewardItem;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimpleDecimalPrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompSound;

import lombok.RequiredArgsConstructor;

/**
 * The menu to claim points for rewards
 */
public class RewardMenu extends Menu {

	/**
	 * How this inventory is opened
	 */
	private ViewMode mode;

	/**
	 * Open class upgrades menu
	 */
	private final Button classButton;

	/**
	 * Open up item purchases menu
	 */
	private final Button itemsButton;

	private RewardMenu(final ViewMode mode) {
		this.mode = mode;

		setSize(9 * 3);
		setTitle(mode == ViewMode.PURCHASE ? "Select your reward" : "Click to edit");

		this.classButton = new Button() {

			@Override
			public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
				if (mode == ViewMode.PURCHASE)
					ClassSelectionMenu.openUpgradeMenu(RewardMenu.this, player);

				else
					ClassSelectionMenu.openEditMenu(RewardMenu.this, player);
			}

			@Override
			public ItemStack getItem() {
				return ItemCreator.of(
						CompMaterial.IRON_SWORD,
						mode == ViewMode.PURCHASE ? "Upgrade your classes" : "Edit classes",
						"",
						mode == ViewMode.PURCHASE ? "Click to upgrade" : "Click to edit",
						"your class tiers")
						.build().make();
			}
		};

		this.itemsButton = new ButtonMenu(new RewardItemMenu(), ItemCreator.of(
				CompMaterial.ENDER_CHEST,
				mode == ViewMode.PURCHASE ? "Claim reward items" : "Edit reward items",
				"",
				mode == ViewMode.PURCHASE ? "Click to claim" : "Click to edit",
				"your rewards."));

	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
	 */
	@Override
	public ItemStack getItemAt(final int slot) {

		if (slot == 9 * 1 + 3)
			return classButton.getItem();

		if (slot == 9 * 1 + 5)
			return itemsButton.getItem();

		return null;
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"Your points: " + ArenaPlayer.getCache(getViewer()).getTotalPoints()
		};
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static access (for usability)
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Show the editing items menu
	 *
	 * @param player
	 */
	public static void openEditMenu(final Player player) {
		new RewardMenu(ViewMode.EDIT_ITEMS).displayTo(player);
	}

	/**
	 * Shows a purchase menu
	 *
	 * @param player
	 */
	public static void openPurchaseMenu(final Player player) {
		new RewardMenu(ViewMode.PURCHASE).displayTo(player);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Classes
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * The menu to claim points for rewards
	 */
	public class RewardItemMenu extends Menu {

		/**
		 * The last edit slot location (this is actually one number less)
		 * for placing items the player can claim as rewards
		 * <p>
		 * By default this is the first 4 rows in our inventory that equal
		 * to the natural players inventory size
		 */
		private final static int MAX_EDIT_SLOT = 9 * 4;

		/**
		 * The rewards instance for convenience
		 */
		private final ArenaReward rewards = ArenaReward.getInstance();

		/**
		 * The button to switch view modes
		 */
		private final Button modeButton;

		/**
		 * Clear up all items
		 */
		private final Button clearButton;

		/**
		 * Create a new menu in the given mode
		 */
		private RewardItemMenu() {
			super(RewardMenu.this);

			setSize(9 * 5);
			setTitle(mode.menuTitle);

			final boolean editingItems = mode == ViewMode.EDIT_ITEMS;

			// Bugfix: Open the reward item menu in Callable to avoid infinite loop (try removing it to see for yourself)
			this.modeButton = new Button() {

				@Override
				public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
					Valid.checkBoolean(mode != ViewMode.PURCHASE, "Cannot click this button in purchase mode!");

					final ItemStack[] content = getContent(0, MAX_EDIT_SLOT);

					if (mode == ViewMode.EDIT_ITEMS) {
						rewards.setItemRewards(content);

						CompSound.CHEST_CLOSE.play(player);
						Messenger.success(player, "Your items have been saved.");
					}

					mode = mode == ViewMode.EDIT_ITEMS ? ViewMode.EDIT_PRICES : ViewMode.EDIT_ITEMS;

					Common.runLater(() -> new RewardItemMenu().displayTo(player));
				}

				@Override
				public ItemStack getItem() {
					return ItemCreator.of(
							CompMaterial.valueOf(editingItems ? "GOLD_INGOT" : "STICK"),
							"Edit " + (editingItems ? "items" : "prices"),
							"Click to toggle.")
							.build().make();
				}
			};

			this.clearButton = Button.makeSimple(CompMaterial.STRING, "Clear items", "Delete all items.", (player) -> {
				rewards.clearItemRewards();

				restartMenu("&1Rewards cleaned and cleared");
			});
		}

		/**
		 * Enable dragging new items in edit mode
		 *
		 * @see org.mineacademy.fo.menu.Menu#isActionAllowed(org.mineacademy.fo.menu.model.MenuClickLocation, int, org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack)
		 */
		@Override
		protected boolean isActionAllowed(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor) {
			return mode == ViewMode.EDIT_ITEMS && (location != MenuClickLocation.MENU || slot < MAX_EDIT_SLOT);
		}

		/**
		 * Handle editing prices and purchasing items
		 *
		 * @see org.mineacademy.fo.menu.Menu#onMenuClick(org.bukkit.entity.Player, int, org.bukkit.inventory.ItemStack)
		 */
		@Override
		protected void onMenuClick(final Player player, final int slot, final ItemStack clicked) {
			final ArenaRewardItem item = rewards.getItemReward(slot);

			if (item == null || slot >= MAX_EDIT_SLOT)
				return;

			if (mode == ViewMode.EDIT_PRICES)
				SimpleDecimalPrompt.show(player, "&6Enter the price for this item. Current: " + item.getPrice() + " points", (price) -> {
					item.setPrice(price);

					Messenger.success(player, "Price of " + item.getItem().getType() + " set to " + item.getPrice() + " points");
				});

			else if (mode == ViewMode.PURCHASE) {
				final ArenaPlayer cache = ArenaPlayer.getCache(player);
				final double points = cache.getTotalPoints();
				final double price = item.getPrice();

				if (points < price) {
					CompSound.BURP.play(player);
					restartMenu("You lack " + MathUtil.formatTwoDigits(price - points) + " points to afford this");

					return;
				}

				cache.setTotalPoints(points - price);

				// Add items to the player inventory
				final Map<Integer, ItemStack> leftovers = PlayerUtil.addItems(player.getInventory(), item.getItem());

				// Dropping on floor if full
				for (final Entry<Integer, ItemStack> entry : leftovers.entrySet())
					player.getWorld().dropItem(player.getLocation(), entry.getValue());

				if (!leftovers.isEmpty())
					Messenger.warn(player, "Your inventory was full and items were dropped on the floor!");

				CompSound.LEVEL_UP.play(player);

				restartMenu("Purchased! Balance: " + cache.getTotalPoints() + " points");
			}
		}

		/**
		 * Display reward items and their prices if editing prices
		 *
		 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
		 */
		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot < MAX_EDIT_SLOT) {
				final ArenaRewardItem item = rewards.getItemReward(slot);

				if (item != null)
					return mode == ViewMode.EDIT_ITEMS ? item.getItem()
							: ItemCreator
									.of(item.getItem())
									.lore("")
									.lore("&6Price: " + item.getPrice() + " points")
									.lore("")
									.lore(mode == ViewMode.PURCHASE ? "Click to purchase." : "Click to edit.")
									.build().make();
			}

			if (mode != ViewMode.PURCHASE) {
				if (slot == getSize() - 4)
					return modeButton.getItem();

				if (slot == getSize() - 6)
					return clearButton.getItem();
			}

			if (slot > getSize() - 9)
				return ItemCreator.of(CompMaterial.GRAY_STAINED_GLASS_PANE, " ").build().make();

			return null;
		}

		@Override
		public Menu newInstance() {
			return new RewardItemMenu();
		}

		/**
		 * Show different info depending on mode
		 *
		 * @see org.mineacademy.fo.menu.Menu#getInfo()
		 */
		@Override
		protected String[] getInfo() {
			if (mode == ViewMode.PURCHASE)
				return new String[] {
						"Balance: " + MathUtil.formatTwoDigits(ArenaPlayer.getCache(getViewer()).getTotalPoints()) + " points"
				};
			else
				return new String[] {
						"Place items here as you want to",
						"show them for players to purchase",
						"them for their points."
				};
		}
	}

	/**
	 * The view mode for this menu, meaning how this menu is opened
	 */
	@RequiredArgsConstructor
	private enum ViewMode {

		/**
		 * Menu opened for purchasing items
		 */
		PURCHASE("Purchase Items"),

		/**
		 * Menu opened for editing items
		 */
		EDIT_ITEMS("Edit Items"),

		/**
		 * Menu opened for setting prices
		 */
		EDIT_PRICES("Edit Prices");

		/**
		 * The menu title for this mode
		 */
		private final String menuTitle;

	}
}
