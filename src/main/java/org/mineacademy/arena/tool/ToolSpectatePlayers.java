package org.mineacademy.arena.tool;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.menu.SpectatePlayersMenu;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.remain.CompMaterial;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Used to select what players to spectate
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolSpectatePlayers extends Tool {

	@Getter
	private static final Tool instance = new ToolSpectatePlayers();

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.of(CompMaterial.COMPASS,
				"Spectate players",
				"",
				"Click to select",
				"what players you",
				"want to spectate.")
				.build().make();
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#onBlockClick(org.bukkit.event.player.PlayerInteractEvent)
	 */
	@Override
	protected void onBlockClick(PlayerInteractEvent event) {
		final Player player = event.getPlayer();
		final ArenaPlayer cache = ArenaPlayer.getCache(player);

		if (cache.getMode() != ArenaJoinMode.SPECTATING) {
			Messenger.error(player, "You can only use this tool while spectating");

			return;
		}

		SpectatePlayersMenu.openMenu(player);
	}

	/**
	 * Do not ignore canceled to catch air clicking
	 */
	@Override
	protected boolean ignoreCancelled() {
		return false;
	}

	/**
	 * Automatically cancel the event
	 */
	@Override
	protected boolean autoCancel() {
		return true;
	}
}
