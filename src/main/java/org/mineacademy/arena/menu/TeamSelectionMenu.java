package org.mineacademy.arena.menu;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaManager;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.arena.model.ArenaTeam;
import org.mineacademy.arena.model.team.TeamArena;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPagged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonConversation;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.button.ButtonRemove;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.model.MenuClickLocation;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompSound;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Menu rendering all teams for an arena
 */
public class TeamSelectionMenu extends MenuPagged<ArenaTeam> {

	/**
	 * The way we open this menu. It can be opened to select teams or edit them
	 */
	private final ViewMode viewMode;

	/**
	 * The arena cache for convenience
	 */
	private final ArenaPlayer cache;

	/**
	 * The arena we are selecting teams to, null if menu is open for editing
	 */
	@Nullable
	private final TeamArena arena;

	/**
	 * The button to create a new team only visible in menu edit mode
	 */
	private final Button createButton;

	/**
	 * Create a new team selection menu
	 *
	 * @param parent
	 * @param player
	 * @param arena
	 * @param viewMode
	 */
	private TeamSelectionMenu(final Menu parent, final Player player, final TeamArena arena, final ViewMode viewMode) {
		super(parent, compile(player, arena, viewMode));

		this.cache = ArenaPlayer.getCache(player);
		this.arena = arena;
		this.viewMode = viewMode;

		setTitle(viewMode.menuTitle);

		// If we are not editing, show empty button (nothing)
		this.createButton = viewMode != ViewMode.EDIT ? Button.makeEmpty()
				: new ButtonConversation(new CreateTeamNamePrompt(), ItemCreator.of(CompMaterial.EMERALD,
						"Create a team",
						"",
						"Click to add a",
						"new arena team."));

	}

	/**
	 * Get teams suitable to show in the menu
	 *
	 * @param player
	 * @param arena
	 * @param viewMode
	 * @return
	 */
	private static List<ArenaTeam> compile(final Player player, final Arena arena, final ViewMode viewMode) {
		if (viewMode == ViewMode.SELECT)
			Valid.checkNotNull(arena, "Arena must be specified when selecting teams");

		final List<ArenaTeam> teams = new ArrayList<>();

		for (final ArenaTeam team : ArenaTeam.getTeams())
			if (viewMode == ViewMode.EDIT || team.canAssign(player, arena))
				teams.add(team);

		return teams;
	}

	/**
	 * Convert a team into an icon
	 */
	@Override
	protected ItemStack convertToItemStack(final ArenaTeam team) {
		final List<String> lores = new ArrayList<>();

		lores.add("");

		if (viewMode == ViewMode.SELECT) {
			lores.add(Common.plural(arena.getTeamPlayers(team).size(), "player"));
			lores.add("");
		}

		lores.add(viewMode == ViewMode.SELECT ? "Click to join" : "Click to edit");

		return ItemCreator.of(team.getIcon())
				.name("Team " + team.getName())
				.lores(lores)
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

		return super.getItemAt(slot);
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuPagged#onPageClick(org.bukkit.entity.Player, java.lang.Object, org.bukkit.event.inventory.ClickType)
	 */
	@Override
	protected void onPageClick(final Player player, final ArenaTeam team, final ClickType click) {
		if (viewMode == ViewMode.SELECT) {

			if (team.equals(cache.getArenaTeam()))
				return;

			if (!arena.isBalancedJoin(team, false)) {
				restartMenu("&4Team is full!");

				return;
			}

			team.assignTo(player);
			CompSound.ENDERDRAGON_WINGS.play(player);

			Messenger.success(player, "&2Assigned team " + team.getName() + "!");

			// Open class selection menu after the player selects their team
			ClassSelectionMenu.openSelectMenu(player, arena);

		} else
			new TeamMenu(team);
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#newInstance()
	 */
	@Override
	public Menu newInstance() {
		return new TeamSelectionMenu(getParent(), getViewer(), arena, viewMode);
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		return new String[] {
				viewMode == ViewMode.SELECT ? "Select your team" : "Select a team",
				viewMode == ViewMode.SELECT ? "for this arena." : "to edit"
		};
	}

	/**
	 * The menu view mode
	 */
	@RequiredArgsConstructor
	private enum ViewMode {

		/**
		 * Menu is opened to select a team during arena lobby
		 */
		SELECT("Select your team"),

		/**
		 * Menu is opened to edit teams
		 */
		EDIT("Edit teams");

		private final String menuTitle;
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static access (for usability)
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Show the team menu where the player can edit teams
	 *
	 * @param parent
	 * @param player
	 */
	public static void openEditMenu(@Nullable final Menu parent, final Player player) {
		new TeamSelectionMenu(parent, player, null, ViewMode.EDIT).displayTo(player);
	}

	/**
	 * Show the team menu where the player can select their team during the lobby phase
	 *
	 * @param player
	 * @param arena
	 */
	public static void openSelectMenu(final Player player, final TeamArena arena) {
		new TeamSelectionMenu(null, player, arena, ViewMode.SELECT).displayTo(player);
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Other menus inside
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * The prompt to give a new team a name
	 */
	private final class CreateTeamNamePrompt extends SimplePrompt {

		private CreateTeamNamePrompt() {
			super(true);
		}

		/**
		 * @see org.mineacademy.fo.conversation.SimplePrompt#getPrompt(org.bukkit.conversations.ConversationContext)
		 */
		@Override
		protected String getPrompt(final ConversationContext ctx) {
			return "&7Enter the team name.";
		}

		/**
		 * @see org.mineacademy.fo.conversation.SimplePrompt#isInputValid(org.bukkit.conversations.ConversationContext, java.lang.String)
		 */
		@Override
		protected boolean isInputValid(final ConversationContext context, final String input) {
			return !ArenaTeam.isTeamLoaded(input);
		}

		/**
		 * @see org.mineacademy.fo.conversation.SimplePrompt#getFailedValidationText(org.bukkit.conversations.ConversationContext, java.lang.String)
		 */
		@Override
		protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {
			return "Team named " + invalidInput + " is already loaded.";
		}

		/**
		 * @see org.bukkit.conversations.ValidatingPrompt#acceptValidatedInput(org.bukkit.conversations.ConversationContext, java.lang.String)
		 */
		@Override
		protected Prompt acceptValidatedInput(@NonNull final ConversationContext context, @NonNull final String input) {
			ArenaTeam.loadOrCreateTeam(input);

			return Prompt.END_OF_CONVERSATION;
		}

	}

	/**
	 * The team edit menu
	 */
	private final class TeamMenu extends Menu {

		/**
		 * The team we are editing
		 */
		private final ArenaTeam team;

		/**
		 * The button to select team color
		 */
		private final Button colorButton;

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
		 * The remove team button
		 */
		private final Button removeButton;

		/**
		 * Create a new team edit menu
		 *
		 * @param team
		 */
		private TeamMenu(final ArenaTeam team) {
			super(TeamSelectionMenu.this);

			this.team = team;

			setTitle("Team " + team.getName());
			setSize(9 * 4);

			this.colorButton = new ButtonMenu(new ColorMenu(), ItemCreator.of(
					CompColor.toWool(team.getColor()),
					"Edit team color",
					"",
					"Current: " + ItemUtil.bountifyCapitalized(team.getColor().name())));

			this.iconButton = new ButtonMenu(new IconMenu(), ItemCreator.of(
					CompMaterial.BEACON,
					"Edit team icon"));

			this.arenasButton = new ButtonMenu(new ApplicableArenasMenu(), ItemCreator.of(
					CompMaterial.IRON_SWORD,
					"Select applicable arenas"));

			this.permissionButton = new ButtonConversation(new PermissionPrompt(), ItemCreator.of(
					CompMaterial.PAPER,
					"Select permission",
					"",
					"Current: " + Common.getOrDefault(team.getPermission(), "none"),
					"",
					"Set the permission",
					"to use this team."));

			this.removeButton = new ButtonRemove(this, "team", team.getName(), teamName -> ArenaTeam.removeTeam(team));

			// Hacky: Display the tier menu right away if upgrading
			displayTo(TeamSelectionMenu.this.getViewer());
		}

		/**
		 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
		 */
		@Override
		public ItemStack getItemAt(final int slot) {

			if (slot == 9 * 1 + 1)
				return colorButton.getItem();

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
			return new TeamMenu(team);
		}

		/**
		 * @see org.mineacademy.fo.menu.Menu#getInfo()
		 */
		@Override
		protected String[] getInfo() {
			return new String[] {
					"Edit your team",
					"settings here."
			};
		}

		/**
		 * Select a teams color
		 */
		private final class ColorMenu extends MenuPagged<ChatColor> {

			protected ColorMenu() {
				super(TeamMenu.this, CompColor.getChatColors(), true);
			}

			/**
			 * @see org.mineacademy.fo.menu.MenuPagged#convertToItemStack(java.lang.Object)
			 */
			@Override
			protected ItemStack convertToItemStack(final ChatColor item) {
				final boolean current = team.getColor() == item;

				return ItemCreator.of(
						CompColor.toWool(item),
						ItemUtil.bountifyCapitalized(item.name()) + " Color",
						"",
						current ? "This is your team's color" : "Click to select")
						.glow(current)
						.build().make();
			}

			/**
			 * @see org.mineacademy.fo.menu.MenuPagged#onPageClick(org.bukkit.entity.Player, java.lang.Object, org.bukkit.event.inventory.ClickType)
			 */
			@Override
			protected void onPageClick(final Player player, final ChatColor item, final ClickType click) {
				team.setColor(item);

				restartMenu("Selected " + ItemUtil.bountifyCapitalized(item.name()) + " Color");
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#getInfo()
			 */
			@Override
			protected String[] getInfo() {
				return new String[] {
						"Select the color for your team",
						"applied for helmets and the chat"
				};
			}

		}

		/**
		 * The edit team icon menu
		 */
		private final class IconMenu extends Menu {

			/**
			 * Create a new menu
			 */
			private IconMenu() {
				super(TeamMenu.this);

				setSize(9 * 3);
				setTitle("Select team icon");
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
			 */
			@Override
			public ItemStack getItemAt(final int slot) {

				if (slot == getCenterSlot())
					return team.getIcon();

				return ItemCreator.of(CompMaterial.GRAY_STAINED_GLASS_PANE).name(" ").build().make();
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#onMenuClose(org.bukkit.entity.Player, org.bukkit.inventory.Inventory)
			 */
			@Override
			protected void onMenuClose(final Player player, final Inventory inventory) {
				final ItemStack item = inventory.getItem(getCenterSlot());

				team.setIcon(item);
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
						"Place the team icon item",
						"to the center slot."
				};
			}
		}

		/**
		 * Menu to select what teams may use this team
		 */
		private final class ApplicableArenasMenu extends MenuPagged<Arena> {

			/**
			 * Create a new arena selection menu
			 */
			private ApplicableArenasMenu() {
				super(TeamMenu.this, ArenaManager.getArenasOfType(TeamArena.class));

				setTitle("Select arenas for team");
			}

			/**
			 * @see org.mineacademy.fo.menu.MenuPagged#convertToItemStack(java.lang.Object)
			 */
			@Override
			protected ItemStack convertToItemStack(final Arena arena) {
				return ItemCreator.of(CompMaterial.BEACON,
						"Arena " + arena.getName(),
						"Click to select")
						.glow(team.getApplicableArenas().contains(arena.getName()))
						.build().make();
			}

			/**
			 * @see org.mineacademy.fo.menu.MenuPagged#onPageClick(org.bukkit.entity.Player, java.lang.Object, org.bukkit.event.inventory.ClickType)
			 */
			@Override
			protected void onPageClick(final Player player, final Arena arena, final ClickType click) {
				final boolean added = team.toggleApplicableArena(arena);

				restartMenu(added ? "&2Arena added!" : "&4Arena removed!");
			}

			/**
			 * @see org.mineacademy.fo.menu.Menu#getInfo()
			 */
			@Override
			protected String[] getInfo() {
				return new String[] {
						"Select what arenas this",
						"team may be used in."
				};
			}

		}

		/**
		 * Set the permission to use this team
		 */
		private final class PermissionPrompt extends SimplePrompt {

			/**
			 * @see org.mineacademy.fo.conversation.SimplePrompt#getPrompt(org.bukkit.conversations.ConversationContext)
			 */
			@Override
			protected String getPrompt(final ConversationContext ctx) {
				return "&6Enter the permission to use team " + team.getName() + " or 'default' to remove. Current: &7" + Common.getOrDefault(team.getPermission(), "none");
			}

			/**
			 * @see org.bukkit.conversations.ValidatingPrompt#acceptValidatedInput(org.bukkit.conversations.ConversationContext, java.lang.String)
			 */
			@Override
			protected Prompt acceptValidatedInput(@NonNull final ConversationContext context, @NonNull final String input) {
				final boolean def = input.equals("default") || input.equals("none");

				team.setPermission(def ? null : input);
				tell(context, "&6" + (def ? "Permission reset." : "Set permission to: " + input));

				return Prompt.END_OF_CONVERSATION;
			}
		}
	}
}
