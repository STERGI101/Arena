package org.mineacademy.arena.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaClass;
import org.mineacademy.arena.model.ArenaClass.ArenaClassTier;
import org.mineacademy.arena.model.ArenaManager;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPagged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonConversation;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.ButtonRemove;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompSound;
import org.mineacademy.fo.remain.Remain;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Menu rendering all classes for an arena
 */
public class ClassSelectionMenu extends MenuPagged<ArenaClass> {

	private static final int CONTENT_SLOT_CAP = 36;
	private static final int ARMOR_SLOT_CAP = 40;

	/**
	 * The way we open this menu. It can be opened to select classes or edit them
	 */
	private final ViewMode viewMode;

	/**
	 * The arena cache for convenience
	 */
	private final ArenaPlayer cache;

	/**
	 * The arena we are selecting classes to, null if menu is open for editing
	 */
	@Nullable
	private final Arena arena;

	/**
	 * The button to create a new class only visible in menu edit mode
	 */
	private final Button createButton;

	/**
	 * A dummy button to show instead of an empty menu
	 */
	private final Button emptyButton;

	/**
	 * Create a new class selection menu
	 *
	 * @param parent
	 * @param player
	 * @param arena
	 * @param viewMode
	 */
	private ClassSelectionMenu(final Menu parent, final Player player, final Arena arena, final ViewMode viewMode) {
		super(parent, compileClasses(player, arena, viewMode));

		this.cache = ArenaPlayer.getCache(player);
		this.arena = arena;
		this.viewMode = viewMode;

		setTitle(viewMode.menuTitle);

		if (isEmpty())
			setSize(9 * 3);

		// If we are not editing, show empty button (nothing)
		this.createButton = viewMode != ViewMode.EDIT ? Button.makeEmpty()
				: new ButtonConversation(new CreateClassNamePrompt(), ItemCreator.of(CompMaterial.EMERALD,
						"Create a class",
						"",
						"Click to add a",
						"new arena class."));

		this.emptyButton = Button.makeDummy(ItemCreator.of(CompMaterial.BLACK_STAINED_GLASS, "No classes available"));
	}

	/**
	 * Get classes suitable to show in the menu
	 *
	 * @param player
	 * @param arena
	 * @param viewMode
	 * @return
	 */
	private static List<ArenaClass> compileClasses(final Player player, final Arena arena, final ViewMode viewMode) {

		if (viewMode == ViewMode.SELECT)
			Valid.checkNotNull(arena, "Arena must be specified when selecting classes");

		final List<ArenaClass> classes = new ArrayList<>();
		final ArenaPlayer cache = ArenaPlayer.getCache(player);

		for (final ArenaClass arenaClass : ArenaClass.getClasses())
			if (viewMode == ViewMode.EDIT || (viewMode == ViewMode.UPGRADE && cache.getTier(arenaClass) > 0) || arenaClass.canAssign(player, arena))
				classes.add(arenaClass);

		return classes;
	}

	/**
	 * Convert a class into an icon
	 */
	@Override
	protected ItemStack convertToItemStack(final ArenaClass arenaClass) {
		final boolean maxTier = cache.getTier(arenaClass) == arenaClass.getTiers();

		final List<String> lore = new ArrayList<>();
		lore.add("");
		lore.add(viewMode != ViewMode.EDIT ? "Tier " + cache.getTier(arenaClass) : "Click to edit.");
		if (viewMode == ViewMode.UPGRADE)
			lore.add(maxTier ? "&2You have the highest tier." : "&6Click to upgrade");

		return ItemCreator
				.of(arenaClass.getIcon())
				.name(arenaClass.getName() + " Class")
				.lores(lore)
				.build().makeMenuTool();
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuPagged#getItemAt(int)
	 */
	@Override
	public ItemStack getItemAt(final int slot) {

		if (viewMode == ViewMode.EDIT)
			if ((slot == getSize() - 1 && getParent() == null) || (getParent() != null && slot == getSize() - 2))
				return createButton.getItem();

		if (isEmpty() && slot == getCenterSlot() && viewMode != ViewMode.EDIT)
			return emptyButton.getItem();

		return super.getItemAt(slot);
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuPagged#onPageClick(org.bukkit.entity.Player, java.lang.Object, org.bukkit.event.inventory.ClickType)
	 */
	@Override
	protected void onPageClick(final Player player, final ArenaClass arenaClass, final ClickType click) {

		if (viewMode == ViewMode.SELECT) {

			if (!arenaClass.equals(cache.getArenaClass())) {
				arenaClass.assignTo(player);
				CompSound.ENDERDRAGON_WINGS.play(player);

				restartMenu("&2Assigned class " + arenaClass.getName() + "!");
			}
		} else
			new ClassMenu(arenaClass);
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#newInstance()
	 */
	@Override
	public Menu newInstance() {
		return new ClassSelectionMenu(getParent(), getViewer(), arena, viewMode);
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#addInfoButton()
	 */
	@Override
	protected boolean addInfoButton() {
		return !isEmpty();
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		return new String[] {
				viewMode == ViewMode.SELECT ? "Select your class" : "Select a class",
				viewMode == ViewMode.SELECT ? "for this arena." : "to edit"
		};
	}

	/**
	 * The menu view mode
	 */
	@RequiredArgsConstructor
	private enum ViewMode {

		/**
		 * Menu is opened to select a class during arena lobby
		 */
		SELECT("Select your class"),

		/**
		 * Menu is opened to upgrade tiers
		 */
		UPGRADE("Upgrade your class"),

		/**
		 * Menu is opened to edit classes
		 */
		EDIT("Edit classes");

		private final String menuTitle;
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static access (for usability)
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Show the class menu where the player can edit classes
	 *
	 * @param parent
	 * @param player
	 */
	public static void openEditMenu(@Nullable final Menu parent, final Player player) {
		new ClassSelectionMenu(parent, player, null, ViewMode.EDIT).displayTo(player);
	}

	/**
	 * Show the class menu where the player can select their class during the lobby phase
	 *
	 * @param player
	 * @param arena
	 */
	public static void openSelectMenu(final Player player, final Arena arena) {
		new ClassSelectionMenu(null, player, arena, ViewMode.SELECT).displayTo(player);
	}

	/**
	 * Show the class menu where the player can upgrade their class when not playing
	 *
	 * @param parent
	 * @param player
	 */
	public static void openUpgradeMenu(@NonNull final Menu parent, final Player player) {
		new ClassSelectionMenu(parent, player, null, ViewMode.UPGRADE).displayTo(player);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Other menus inside
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * The prompt to give a new class a name
	 */
	private final class CreateClassNamePrompt extends SimplePrompt {

		private CreateClassNamePrompt() {
			super(true);
		}

		/**
		 * @see org.mineacademy.fo.conversation.SimplePrompt#getPrompt(org.bukkit.conversations.ConversationContext)
		 */
		@Override
		protected String getPrompt(final ConversationContext ctx) {
			return "&7Enter the class name.";
		}

		/**
		 * @see org.mineacademy.fo.conversation.SimplePrompt#isInputValid(org.bukkit.conversations.ConversationContext, java.lang.String)
		 */
		@Override
		protected boolean isInputValid(final ConversationContext context, final String input) {
			return !ArenaClass.isClassLoaded(input);
		}

		/**
		 * @see org.mineacademy.fo.conversation.SimplePrompt#getFailedValidationText(org.bukkit.conversations.ConversationContext, java.lang.String)
		 */
		@Override
		protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {
			return "Class named " + invalidInput + " is already loaded.";
		}

		/**
		 * @see org.bukkit.conversations.ValidatingPrompt#acceptValidatedInput(org.bukkit.conversations.ConversationContext, java.lang.String)
		 */
		@Override
		protected Prompt acceptValidatedInput(@NonNull final ConversationContext context, @NonNull final String input) {
			ArenaClass.loadOrCreateClass(input);

			return Prompt.END_OF_CONVERSATION;
		}

	}

	/**
	 * The class edit menu
	 */
	private final class ClassMenu extends Menu {

		/**
		 * The class we are editing
		 */
		private final ArenaClass arenaClass;

		/**
		 * The icon edit button
		 */
		private final Button iconButton;

		/**
		 * The applicable arenas button
		 */
		private final Button arenasButton;

		/**
		 * The permission button
		 */
		private final Button permissionButton;

		/**
		 * The tier edit button
		 */
		private final Button tierButton;

		/**
		 * The remove class button
		 */
		private final Button removeButton;

		/**
		 * Create a new class edit menu
		 *
		 * @param arenaClass
		 */
		private ClassMenu(final ArenaClass arenaClass) {
			super(ClassSelectionMenu.this);

			this.arenaClass = arenaClass;

			setTitle("Class " + arenaClass.getName());
			setSize(9 * 4);

			this.iconButton = new ButtonMenu(new IconMenu(), ItemCreator.of(
					CompMaterial.BEACON,
					"Edit class icon"));

			this.arenasButton = new ButtonMenu(new ApplicableArenasMenu(), ItemCreator.of(
					CompMaterial.IRON_SWORD,
					"Select applicable arenas"));

			this.permissionButton = new ButtonConversation(new PermissionPrompt(), ItemCreator.of(
					CompMaterial.PAPER,
					"Select permission",
					"",
					"Current: " + Common.getOrDefault(arenaClass.getPermission(), "none"),
					"",
					"Set the permission",
					"to use this class."));

			this.tierButton = new ButtonMenu(new TierMenu(1), ItemCreator.of(
					Remain.getMaterial("ELYTRA", CompMaterial.CHEST),
					"Edit class tiers"));

			this.removeButton = new ButtonRemove(this, "class", arenaClass.getName(), className -> ArenaClass.removeClass(arenaClass));

			// Hacky: Display the tier menu right away if upgrading
			if (viewMode == ViewMode.UPGRADE)
				new TierMenu(1).displayTo(ClassSelectionMenu.this.getViewer());
			else
				displayTo(ClassSelectionMenu.this.getViewer());
		}

		/**
		 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
		 */
		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == 9 * 1 + 1)
				return tierButton.getItem();

			if (slot == 9 * 1 + 3)
				return iconButton.getItem();

			if (slot == 9 * 1 + 5)
				return arenasButton.getItem();

			if (slot == 9 * 1 + 7)
				return permissionButton.getItem();

			if (slot == getSize() - 5)
				return removeButton.getItem();

			return null;
		}

		/**
		 * @see org.mineacademy.fo.menu.Menu#newInstance()
		 */
		@Override
		public Menu newInstance() {
			return new ClassMenu(arenaClass);
		}

		/**
		 * @see org.mineacademy.fo.menu.Menu#getInfo()
		 */
		@Override
		protected String[] getInfo() {
			return new String[] {
					"Edit your class",
					"settings here."
			};
		}

		/**
		 * The edit class icon menu
		 */
		private final class IconMenu extends Menu {

			/**
			 * Create a new menu
			 */
			private IconMenu() {
				super(ClassMenu.this);

				setSize(9 * 3);
				setTitle("Select class icon");
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
			 */
			@Override
			public ItemStack getItemAt(final int slot) {

				if (slot == getCenterSlot())
					return arenaClass.getIcon();

				return ItemCreator.of(CompMaterial.GRAY_STAINED_GLASS_PANE).name(" ").build().make();
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#onMenuClose(org.bukkit.entity.Player, org.bukkit.inventory.Inventory)
			 */
			@Override
			protected void onMenuClose(final Player player, final Inventory inventory) {
				final ItemStack item = inventory.getItem(getCenterSlot());

				arenaClass.setIcon(item);
				CompSound.SUCCESSFUL_HIT.play(player);
			}

			/**
			 * Enable clicking outside of the menu or in the slot item
			 */
			@Override
			protected boolean isActionAllowed(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor) {
				return location != MenuClickLocation.MENU || (location == MenuClickLocation.MENU && slot == getCenterSlot());
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#getInfo()
			 */
			@Override
			protected String[] getInfo() {
				return new String[] {
						"Place the class icon item",
						"to the center slot."
				};
			}
		}

		/**
		 * Menu to select what classes may use this class
		 */
		private final class ApplicableArenasMenu extends MenuPagged<Arena> {

			/**
			 * Create a new arena selection menu
			 */
			private ApplicableArenasMenu() {
				super(ClassMenu.this, ArenaManager.getArenas());

				setTitle("Select arenas for class");
			}

			/**
			 * @see org.mineacademy.fo.menu.MenuPagged#convertToItemStack(java.lang.Object)
			 */
			@Override
			protected ItemStack convertToItemStack(final Arena arena) {
				return ItemCreator.of(CompMaterial.BEACON,
						"Arena " + arena.getName(),
						"Click to select")
						.glow(arenaClass.getApplicableArenas().contains(arena.getName()))
						.build().make();
			}

			/**
			 * @see org.mineacademy.fo.menu.MenuPagged#onPageClick(org.bukkit.entity.Player, java.lang.Object, org.bukkit.event.inventory.ClickType)
			 */
			@Override
			protected void onPageClick(final Player player, final Arena arena, final ClickType click) {
				final boolean added = arenaClass.toggleApplicableArena(arena);

				restartMenu(added ? "&2Arena added!" : "&4Arena removed!");
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#getInfo()
			 */
			@Override
			protected String[] getInfo() {
				return new String[] {
						"Select what arenas this",
						"class may be used in."
				};
			}

		}

		/**
		 * Set the permission to use this class
		 */
		private final class PermissionPrompt extends SimplePrompt {

			/**
			 * @see org.mineacademy.fo.conversation.SimplePrompt#getPrompt(org.bukkit.conversations.ConversationContext)
			 */
			@Override
			protected String getPrompt(final ConversationContext ctx) {
				return "&6Enter the permission to use class " + arenaClass.getName() + " or 'default' to remove. Current: &7" + Common.getOrDefault(arenaClass.getPermission(), "none");
			}

			/**
			 * @see org.bukkit.conversations.ValidatingPrompt#acceptValidatedInput(org.bukkit.conversations.ConversationContext, java.lang.String)
			 */
			@Override
			protected Prompt acceptValidatedInput(@NonNull final ConversationContext context, @NonNull final String input) {
				final boolean def = input.equals("default") || input.equals("none");

				arenaClass.setPermission(def ? null : input);
				tell(context, "&6" + (def ? "Permission reset." : "Set permission to: " + input));

				return Prompt.END_OF_CONVERSATION;
			}
		}

		/**
		 * The menu enabling us to create/remove arena class tiers
		 */
		public final class TierMenu extends Menu {

			/**
			 * The tier level
			 */
			private final int tierLevel;

			/**
			 * The tier class
			 */
			private final ArenaClassTier tier;

			/**
			 * The button to configure potions
			 */
			private final Button potionsButton;

			/**
			 * The button to upgrade tier
			 */
			private final Button upgradeTier;

			/**
			 * The button to previous tier
			 */
			private final Button previousTier;

			/**
			 * The button to next tier
			 */
			private final Button nextTier;

			/**
			 * The button to edit the price with
			 */
			private final Button priceButton;

			/**
			 * Create this menu
			 *
			 * @param tierLevel
			 */
			private TierMenu(final int tierLevel) {
				super(viewMode == ViewMode.UPGRADE ? ClassSelectionMenu.this : ClassMenu.this);

				Valid.checkBoolean(tierLevel < arenaClass.getTiers() + 2, "Cannot jump more than 2 tier ahead in tier menu");

				final boolean nextTierExists = viewMode == ViewMode.UPGRADE ? tierLevel < arenaClass.getTiers() : tierLevel <= arenaClass.getTiers() || arenaClass.getTiers() == 0;

				this.tierLevel = tierLevel;
				this.tier = getOrMakeTier(tierLevel);

				setSize(9 * 6);
				setTitle(viewMode == ViewMode.UPGRADE ? "Tier " + tierLevel : "Edit Tier " + tierLevel);

				this.potionsButton = new ButtonMenu(new PotionsMenu(), ItemCreator.of(CompMaterial.POTION, "Edit potions"));

				final int playerTier = cache.getTier(arenaClass);
				final boolean mayBeUpgraded = playerTier + 1 == tierLevel;
				final boolean upgraded = playerTier < arenaClass.getTiers();
				final boolean hasMaxTier = playerTier == arenaClass.getTiers();

				this.upgradeTier = new Button() {

					@Override
					public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
						if (mayBeUpgraded) {

							final double funds = cache.getTotalPoints();
							final double price = tier.getPrice();

							if (funds < price)
								animateTitle("You lack " + MathUtil.formatTwoDigits(price - funds) + " Coins!");

							else {
								cache.setTotalPoints(funds - price);
								cache.saveTier(arenaClass, tierLevel);

								CompSound.LEVEL_UP.play(player);

								Common.runLater(() -> {
									final Menu newMenu = newInstance();

									newMenu.displayTo(player);
									Common.runLater(() -> newMenu.animateTitle("&2Purchased Tier " + tierLevel + " for " + funds + " Coins!"));
								});
							}
						}
					}

					@Override
					public ItemStack getItem() {
						final List<String> lores = new ArrayList<>();

						if (mayBeUpgraded) {
							lores.add("");
							lores.add("Price: " + tier.getPrice() + " points");
						}

						return ItemCreator
								.of(mayBeUpgraded ? CompMaterial.ENDER_EYE : upgraded || hasMaxTier ? CompMaterial.ENDER_PEARL : CompMaterial.GLASS)
								.name(mayBeUpgraded ? "&6Click to upgrade" : upgraded ? "&2You have this tier" : hasMaxTier ? "&7You have the max tier" : "&7This is the first tier")
								.lores(lores)
								.glow(mayBeUpgraded || upgraded || hasMaxTier)
								.build().make();
					}
				};

				this.previousTier = new Button() {

					final boolean abovefirstTier = tierLevel > 1;

					@Override
					public ItemStack getItem() {
						return ItemCreator
								.of(abovefirstTier ? CompMaterial.LIME_DYE : CompMaterial.GRAY_DYE)
								.name(abovefirstTier ? viewMode == ViewMode.UPGRADE ? "Previous tier" : "Edit previous tier" : "This is the first tier")
								.build().make();
					}

					@Override
					public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
						if (abovefirstTier)
							new TierMenu(tierLevel - 1).displayTo(player);
					}
				};

				this.nextTier = new Button() {

					@Override
					public ItemStack getItem() {
						return ItemCreator
								.of(nextTierExists ? CompMaterial.LIME_DYE : CompMaterial.GRAY_DYE)
								.name(viewMode == ViewMode.UPGRADE ? nextTierExists ? "Next tier" : "This is the highest tier" : nextTierExists ? "Edit next tier" : "Finish this tier first")
								.build().make();
					}

					@Override
					public void onClickedInMenu(final Player player, final Menu menu, final ClickType click) {
						if (nextTierExists) {
							final boolean empty = isEmpty(player.getOpenInventory().getTopInventory());
							final Menu nextTierMenu = new TierMenu(tierLevel + (empty && tierLevel > 1 ? -1 : 1));

							nextTierMenu.displayTo(player);

							if (empty && tierLevel > 1)
								Common.runLater(() -> nextTierMenu.animateTitle("&4Removed Tier " + tierLevel));
						}
					}
				};

				this.priceButton = Button.makeDecimalPrompt(ItemCreator.of(
						CompMaterial.GOLD_INGOT,
						"Edit Price",
						"",
						"Current: " + tier.getPrice() + " points",
						"",
						"Edit the price",
						"for this tier."),
						// The question for this prompt
						"Enter the price for this tier. Current: " + tier.getPrice() + " points",
						price -> tier.setPrice(price));
			}

			/*
			 * Get the tier for the given level, creating it if it does not exist
			 *
			 */
			private ArenaClassTier getOrMakeTier(final int tierLevel) {
				ArenaClassTier tier = arenaClass.getTier(tierLevel);

				if (tier == null)
					tier = arenaClass.addTier();

				return tier;
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
			 */
			@Override
			public ItemStack getItemAt(int slot) {
				final ItemStack[] content = tier.getContent();
				final ItemStack[] armor = tier.getArmorContent();

				// Display inventory content slot
				if (slot < CONTENT_SLOT_CAP)
					return content != null && content.length > slot ? content[slot] : null;

				// Display armor slots
				if (slot < ARMOR_SLOT_CAP) {
					slot = slot - CONTENT_SLOT_CAP;

					return armor != null && armor.length > slot ? armor[slot] : null;
				}

				// Fill the rest of the armor slots with stained glass
				if (slot <= 44)
					return ItemCreator.of(CompMaterial.GRAY_STAINED_GLASS_PANE).name(slot == ARMOR_SLOT_CAP ? viewMode == ViewMode.UPGRADE ? " " : "<&m--&r Place armor" : " ").build().make();

				if (viewMode != ViewMode.UPGRADE && slot == getSize() - 8)
					return potionsButton.getItem();

				if (slot == getSize() - 6)
					return previousTier.getItem();

				if (slot == getSize() - 5)
					return viewMode == ViewMode.UPGRADE ? upgradeTier.getItem() : priceButton.getItem();

				if (slot == getSize() - 4)
					return nextTier.getItem();

				return null;
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#onMenuClose(org.bukkit.entity.Player, org.bukkit.inventory.Inventory)
			 */
			@Override
			protected void onMenuClose(final Player player, final Inventory inventory) {
				if (viewMode == ViewMode.EDIT) {
					final ItemStack[] content = copy(inventory.getContents(), 0, CONTENT_SLOT_CAP);
					final ItemStack[] armor = copy(inventory.getContents(), CONTENT_SLOT_CAP, ARMOR_SLOT_CAP);

					// If both are empty, remove tier
					if (isEmpty(content, armor) && tierLevel > 1) {
						final int tiers = arenaClass.getTiers();

						// Only remove the tier if it exists
						if (tiers >= tierLevel)
							arenaClass.removeTier(tierLevel);

					} else
						tier.setAllContent(content, armor);
				}
			}

			/*
			 * Return if the content and armor inventories are both empty
			 */
			private boolean isEmpty(final Inventory inventory) {
				final ItemStack[] content = copy(inventory.getContents(), 0, CONTENT_SLOT_CAP);
				final ItemStack[] armor = copy(inventory.getContents(), CONTENT_SLOT_CAP, ARMOR_SLOT_CAP);

				return isEmpty(content, armor);
			}

			/*
			 * Return if both inventories are empty
			 */
			private boolean isEmpty(final ItemStack[] content, final ItemStack[] armor) {
				return Valid.isNullOrEmpty(content) && Valid.isNullOrEmpty(armor);
			}

			/*
			 * Copy content to match the array length, cloning items preventing ID mismatch in yaml files
			 */
			private ItemStack[] copy(final ItemStack[] content, final int from, final int to) {
				final ItemStack[] copy = new ItemStack[content.length];

				for (int i = from; i < copy.length; i++) {
					final ItemStack item = content[i];

					copy[i] = item != null ? item.clone() : null;
				}

				return Arrays.copyOfRange(copy, from, to);
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#isActionAllowed(org.mineacademy.fo.menu.model.MenuClickLocation, int, org.bukkit.inventory.ItemStack, org.bukkit.inventory.ItemStack)
			 */
			@Override
			protected boolean isActionAllowed(final MenuClickLocation location, final int slot, final ItemStack clicked, final ItemStack cursor) {
				if (viewMode != ViewMode.EDIT)
					return false;

				final SerializedMap armorSlots = SerializedMap.ofArray(
						"36", "BOOTS",
						"37", "LEGGINGS",
						"38", "CHESTPLATE");

				if (location == MenuClickLocation.MENU) {
					final String item = cursor.getType().toString();

					// Allow editing inventory content
					if (slot < CONTENT_SLOT_CAP || slot == ARMOR_SLOT_CAP - 1)
						return true;

					// Allow editing armor content with appropriate slots only
					final String restrictedItem = armorSlots.getString(String.valueOf(slot));

					if (restrictedItem != null) {
						final boolean allow = item.contains(restrictedItem) || CompMaterial.isAir(cursor.getType());

						// Remove the restricted item if stuck there
						if (!allow) {
							getViewer().getOpenInventory().getTopInventory().setItem(slot, null);

							animateTitle("&4" + ItemUtil.bountifyCapitalized(restrictedItem) + " only!");
						}

						return allow;
					}
				}

				return location != MenuClickLocation.MENU;
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#newInstance()
			 */
			@Override
			public Menu newInstance() {
				return new TierMenu(tierLevel);
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#getInfo()
			 */
			@Override
			protected String[] getInfo() {
				if (viewMode == ViewMode.UPGRADE)
					return new String[] {
							"Upgrade your class tiers",
							"here in this menu.",
							"",
							"Your balance: " + cache.getTotalPoints() + " points"
					};
				else
					return new String[] {
							"Edit the items for this tier",
							"as you'd like, then leave the",
							"menu to save changes automatically."
					};
			}

			/**
			 * The menu to edit potions
			 */
			private final class PotionsMenu extends MenuPagged<PotionEffectType> {

				/**
				 * Create a new menu
				 */
				private PotionsMenu() {
					super(TierMenu.this, compilePotions());
				}

				/**
				 * @see org.mineacademy.fo.menu.MenuPagged#convertToItemStack(java.lang.Object)
				 */
				@Override
				protected ItemStack convertToItemStack(final PotionEffectType type) {
					final int level = tier.getPotionEffect(type);

					final String name = (level > 0 ? "&f" : "&7") + ItemUtil.bountifyCapitalized(type.getName());
					final boolean longName = name.length() > 15;

					final ItemStack item = ItemCreator.of(
							level == 0 ? CompMaterial.GLASS_BOTTLE : CompMaterial.POTION,
							"&r" + name + " " + MathUtil.toRoman(level),
							"",
							(longName ? "  " : "") + " &8(Mouse click)",
							(longName ? " " : "") + " &7&l< &4-1    &2+1 &7&l>")
							.amount(level > 1 ? level : 1)
							.glow(level > 0)
							.hideTags(true).build().make();

					if (level > 0)
						Remain.setPotion(item, type, level);

					return item;
				}

				/**
				 * @see org.mineacademy.fo.menu.MenuPagged#onPageClick(org.bukkit.entity.Player, java.lang.Object, org.bukkit.event.inventory.ClickType)
				 */
				@Override
				protected void onPageClick(final Player player, final PotionEffectType type, final ClickType click) {
					int level = tier.getPotionEffect(type);

					level = MathUtil.range(click == ClickType.LEFT ? level - 1 : level + 1, 0, 127);

					tier.setPotionEffect(type, level);

					restartMenu("Set level to " + level);
				}

				/**
				 * @see org.mineacademy.fo.menu.Menu#getInfo()
				 */
				@Override
				protected String[] getInfo() {
					return new String[] {
							"Select potions to apply",
							"for the class Tier " + tierLevel
					};
				}

			}
		}
	}

	/*
	 * Compile potions applicable for arena class
	 */
	private static List<PotionEffectType> compilePotions() {
		final List<PotionEffectType> list = new ArrayList<>();

		for (final PotionEffectType type : PotionEffectType.values())
			if (type != null)
				list.add(type);

		return list;
	}
}
