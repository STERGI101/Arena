package org.mineacademy.arena.menu;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.MenuPagged;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;

/**
 * The menu used to select players to teleport to when spectating
 */
public class SpectatePlayersMenu extends MenuPagged<Player> {

	/**
	 * Create a new spectate menu
	 *
	 * @param pageSize
	 * @param pages
	 */
	private SpectatePlayersMenu(Player viewer) {
		super(compilePlayers(viewer));

		setTitle("Select players to spectate");
	}

	/*
	 * Get a list of players we can spectate
	 */
	private static List<Player> compilePlayers(Player viewer) {
		final ArenaPlayer cache = ArenaPlayer.getCache(viewer);
		Valid.checkBoolean(cache.getMode() == ArenaJoinMode.SPECTATING, "Spectate menu may only be opened in spectate arena mode!");

		return cache.getArena().getPlayers(ArenaJoinMode.PLAYING);
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuPagged#convertToItemStack(java.lang.Object)
	 */
	@Override
	protected ItemStack convertToItemStack(Player player) {
		return ItemCreator.of(
				CompMaterial.PLAYER_HEAD,
				player.getName(),
				"",
				"Click to teleport",
				"to that player.")
				.skullOwner(player.getName())
				.build().make();
	}

	/**
	 * @see org.mineacademy.fo.menu.MenuPagged#onPageClick(org.bukkit.entity.Player, java.lang.Object, org.bukkit.event.inventory.ClickType)
	 */
	@Override
	protected void onPageClick(Player viewer, Player clickedPlayer, ClickType click) {
		viewer.closeInventory();
		viewer.teleport(clickedPlayer.getLocation().add(0, 1, 0));
		viewer.setCompassTarget(clickedPlayer.getLocation());

		Messenger.success(viewer, "You are now teleported to " + clickedPlayer.getName());
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#getInfo()
	 */
	@Override
	protected String[] getInfo() {
		return new String[] {
				"Click a player to teleport",
				"to him to spectate his",
				"performance in the arena!"
		};
	}

	/**
	 * Open the spectate player selection menu to the given player
	 *
	 * @param player
	 */
	public static void openMenu(Player player) {
		new SpectatePlayersMenu(player).displayTo(player);
	}

}
