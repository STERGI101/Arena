package org.mineacademy.arena.menu;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.model.eggwars.EggWarsArena;
import org.mineacademy.arena.model.eggwars.EggWarsSettings.ItemCurrency;
import org.mineacademy.arena.model.eggwars.EggWarsSettings.ShopItem;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.remain.CompMaterial;

import javax.annotation.Nullable;

/**
 * Enable to purchase items from the villager when playing egg wars
 * or edit the items that are offered
 */
public class EggWarsVillagerMenu extends Menu {

	/**
	 * The arena we display items in
	 */
	private final EggWarsArena arena;

	/**
	 * Is the menu displayed for editing or purchasing?
	 */
	private final ViewMode viewMode;

	/**
	 * The button to change how the menu is shown
	 */
	private final Button modeButton;

	/**
	 * Create a new villager menu
	 *
	 * @param arena
	 * @param viewMode
	 */
	private EggWarsVillagerMenu(EggWarsArena arena, ViewMode viewMode) {
		this.arena = arena;
		this.viewMode = viewMode;

		setSize(9 * 5);
		setTitle(viewMode.menuTitle);
		setInfo("Select items you want to",
				"purchase to use in the arena!");

		this.modeButton = new Button() {

			@Override
			public void onClickedInMenu(Player player, Menu menu, ClickType click) {
				player.closeInventory();

				new EggWarsVillagerMenu(arena, viewMode == ViewMode.EDIT_PRICES ? ViewMode.EDIT_ITEMS : ViewMode.EDIT_PRICES).displayTo(player);
			}

			@Override
			public ItemStack getItem() {
				final boolean editingPrices = viewMode == ViewMode.EDIT_PRICES;

				return ItemCreator.of(
						editingPrices ? CompMaterial.STRING : CompMaterial.GOLD_INGOT,
						"Editing " + (editingPrices ? "prices" : "items"),
						"",
						"Click to edit " + (editingPrices ? "items" : "prices"))
						.build().make();
			}
		};
	}

	/**
	 * Show edit price prompt or purchase items
	 *
	 * @see org.mineacademy.fo.menu.Menu#onMenuClick(org.bukkit.entity.Player, int, org.bukkit.inventory.ItemStack)
	 */
	@Override
	protected void onMenuClick(Player player, int slot, ItemStack clicked) {
		final ShopItem item = arena.getSettings().getItem(slot);

		if (item == null)
			return;

		if (viewMode == ViewMode.EDIT_PRICES)
			SimplePrompt.show(player, new PricePrompt(item));

		else if (viewMode == ViewMode.PURCHASE) {
			final int price = item.getPrice();
			final CompMaterial material = item.getCurrency().getMaterial();

			if (!PlayerUtil.containsAtLeast(player, price, material)) {
				restartMenu("&4Lacking funds!");

				return;
			}

			PlayerUtil.take(player, material, price);
			PlayerUtil.addItems(player.getInventory(), item.getItem());

			restartMenu("&2Purchase made!");
		}
	}

	/**
	 * Return the item at the given slot location, in this case
	 * the items being sold/edited or our buttons
	 *
	 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
	 */
	@Override
	public ItemStack getItemAt(int slot) {
		final ShopItem item = arena.getSettings().getItem(slot);

		if (item != null) {
			final ItemCreator.ItemCreatorBuilder builder = ItemCreator.of(item.getItem());

			if (viewMode != ViewMode.EDIT_ITEMS) {
				builder.lore("");
				builder.lore("&6Price: " + item.getPriceFormatted());

				if (viewMode == ViewMode.EDIT_PRICES)
					builder.lore("&6Click to edit");
			}

			return builder.build().make();
		}

		if (slot == getSize() - 5 && viewMode != ViewMode.PURCHASE)
			return modeButton.getItem();

		return null;
	}

	/**
	 * Save items when edit menu is closed
	 *
	 * @see org.mineacademy.fo.menu.Menu#onMenuClose(org.bukkit.entity.Player, org.bukkit.inventory.Inventory)
	 */
	@Override
	protected void onMenuClose(Player player, Inventory inventory) {
		if (viewMode == ViewMode.EDIT_ITEMS) {
			final ItemStack[] items = getContent(0, 9 * 4);

			for (int index = 0; index < items.length; index++)
				arena.getSettings().setItem(index, items[index]);
		}
	}

	/**
	 * Allow dragging items to the menu while edited
	 *
	 * @see org.mineacademy.fo.menu.Menu#isActionAllowed(org.mineacademy.fo.menu.model.MenuClickLocation, int, org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack)
	 */
	@Override
	protected boolean isActionAllowed(MenuClickLocation location, int slot, ItemStack clicked, ItemStack cursor) {
		return viewMode == ViewMode.EDIT_ITEMS && (location == MenuClickLocation.PLAYER_INVENTORY || (location == MenuClickLocation.MENU && slot < getSize() - 9));
	}

	/**
	 * Return to this menu after setting the price
	 *
	 * @see org.mineacademy.fo.menu.Menu#newInstance()
	 */
	@Override
	public Menu newInstance() {
		return new EggWarsVillagerMenu(arena, viewMode);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static access (for usability)
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Open the menu for editing items
	 *
	 * @param arena
	 * @param player
	 */
	public static void openEditMenu(EggWarsArena arena, Player player) {
		new EggWarsVillagerMenu(arena, ViewMode.EDIT_ITEMS).displayTo(player);
	}

	/**
	 * Open the menu for playing
	 *
	 * @param arena
	 * @param player
	 */
	public static void openPurchaseMenu(EggWarsArena arena, Player player) {
		new EggWarsVillagerMenu(arena, ViewMode.PURCHASE).displayTo(player);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Classes
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * How this menu is displayed
	 */
	@RequiredArgsConstructor
	private enum ViewMode {

		/**
		 * Menu is opened for editing items
		 */
		EDIT_ITEMS("Editing Shop Items"),

		/**
		 * Menu is opened for setting up prices
		 */
		EDIT_PRICES("Editing Shop Prices"),

		/**
		 * Menu is opened in game for purchasing
		 */
		PURCHASE("Villager Shop");

		/**
		 * The title of the menu in this mode
		 */
		private final String menuTitle;
	}

	/**
	 * A simple prompt to change an item's price
	 */
	@RequiredArgsConstructor
	private class PricePrompt extends SimplePrompt {

		/**
		 * The item
		 */
		private final ShopItem item;

		/**
		 * @see org.mineacademy.fo.conversation.SimplePrompt#getPrompt(org.bukkit.conversations.ConversationContext)
		 */
		@Override
		protected String getPrompt(ConversationContext ctx) {
			return "&6Enter the price for this item. Currently: " + item.getPriceFormatted();
		}

		/**
		 * @see org.mineacademy.fo.conversation.SimplePrompt#isInputValid(org.bukkit.conversations.ConversationContext, java.lang.String)
		 */
		@Override
		protected boolean isInputValid(ConversationContext context, String input) {
			final String[] split = input.split(" ");

			try {
				ReflectionUtil.lookupEnum(ItemCurrency.class, split[1]);

			} catch (final Throwable ex) {
				return false;
			}

			return split.length == 2 && Valid.isInteger(split[0]);
		}

		/**
		 * @see org.mineacademy.fo.conversation.SimplePrompt#getFailedValidationText(org.bukkit.conversations.ConversationContext, java.lang.String)
		 */
		@Override
		protected String getFailedValidationText(ConversationContext context, String invalidInput) {
			return "&cPlease use the format: <price> " + Common.convert(ItemCurrency.values(), currency -> currency.toString().toLowerCase());
		}

		/**
		 * @see org.bukkit.conversations.ValidatingPrompt#acceptValidatedInput(org.bukkit.conversations.ConversationContext, java.lang.String)
		 */
		@Override
		protected @Nullable Prompt acceptValidatedInput(@NonNull ConversationContext context, @NonNull String input) {
			final String[] split = input.split(" ");
			final int price = Integer.parseInt(split[0]);
			final ItemCurrency currency = ReflectionUtil.lookupEnum(ItemCurrency.class, split[1]);

			item.setPrice(price, currency);
			tell(context, "&2Item price has been set to " + item.getPriceFormatted() + ".");

			return Prompt.END_OF_CONVERSATION;
		}
	}
}
