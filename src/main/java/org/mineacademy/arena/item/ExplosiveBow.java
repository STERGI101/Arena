package org.mineacademy.arena.item;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaManager;
import org.mineacademy.arena.model.ArenaState;
import org.mineacademy.arena.util.ArenaUtil;
import org.mineacademy.fo.BlockUtil;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MinecraftVersion;
import org.mineacademy.fo.MinecraftVersion.V;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExplosiveBow extends Tool implements Listener {

	@Getter
	private final static Tool instance = new ExplosiveBow();

	private final Map<Location, Vector> explodedLocations = new HashMap<>();

	@Override
	public ItemStack getItem() {
		return ItemCreator.of(CompMaterial.BOW,
				"&6Explosive Bow",
				"",
				"Right click air to launch",
				"explosive arrows...")
				.glow(true)
				.build().make();
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onProjectileLaunch(final ProjectileLaunchEvent event) {
		if (!(event.getEntity().getShooter() instanceof Player))
			return;

		final Projectile projectile = event.getEntity();
		final Player player = (Player) projectile.getShooter();

		// Checks if the players item in their hand is similar to this item
		if (ItemUtil.isSimilar(player.getItemInHand(), getItem()))
			CompMetadata.setTempMetadata(projectile, "ExplosiveBow");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onProjectileHit(final ProjectileHitEvent event) {
		// Catch the shot arrow so that we can make an explosion at the hit location
		if (!CompMetadata.hasTempMetadata(event.getEntity(), "ExplosiveBow"))
			return;

		final Projectile projectile = event.getEntity();
		final Arena arena = ArenaManager.findArena(projectile.getLocation());

		if (!projectile.isValid() || arena == null) {
			projectile.remove();

			return;
		}

		// Before the explosion we must store where the block that this explosion will come from is located
		// as well as the direction and speed of the arrow
		explodedLocations.put(projectile.getLocation().getBlock().getLocation(), projectile.getVelocity());

		projectile.remove();
		projectile.getWorld().createExplosion(projectile.getLocation(), 3F);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockExplode(final BlockExplodeEvent event) {

		// We try to remove the exploded block location we stored above
		// This method is failsafe and simply returns null if nothing was removed
		final Vector vector = explodedLocations.remove(event.getBlock().getLocation());

		if (vector != null) {
			for (final Iterator<Block> it = event.blockList().iterator(); it.hasNext();) {
				final Block block = it.next();
				final Arena arena = ArenaManager.findArena(block.getLocation());

				if (arena == null || arena.getState() != ArenaState.PLAYED) {
					it.remove();

					continue;
				}

				// Use "chance" for chances between 0-100 or "chanceD" for whole numbers (chances between 0.00 for 0% and 1.00 for 100%)
				if (RandomUtil.chanceD(0.45)) {
					final FallingBlock falling = BlockUtil.shootBlock(block, vector);

					// Old Minecraft does not fire entity spawn event for flying blocks,
					if (MinecraftVersion.olderThan(V.v1_13))
						ArenaUtil.preventArenaLeave(arena, falling);

				} else
					// Otherwise just remove the block
					block.setType(CompMaterial.AIR.getMaterial());
			}

			// Prevent or greatly reduce the amount of dropped items
			event.setYield(0F);
		}
	}

	@Override
	protected void onBlockClick(final PlayerInteractEvent event) {
		// We do not use this since the Bow must be charged to launch arrows and simply clicking wont do anything
	}
}
