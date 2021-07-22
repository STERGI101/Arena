package org.mineacademy.arena.task;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaLeaveReason;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.arena.util.Constants;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompMetadata;
import org.mineacademy.fo.remain.Remain;

/**
 * A repeating task kicking players who left the arena region while playing
 */
public class EscapeTask extends BukkitRunnable {

	@Override
	public void run() {

		try {
			// Scan through all players using remain compatible method
			for (final Player player : Remain.getOnlinePlayers()) {

				final Location location = player.getLocation();

				// Ignore falling to void
				if (location.getY() < 0)
					continue;

				// Ignore if has a tag
				if (CompMetadata.hasTempMetadata(player, Constants.Tag.TELEPORT_EXEMPTION))
					continue;

				final ArenaPlayer cache = ArenaPlayer.getCache(player);

				// Enable leaving region while editing
				if (cache.hasArena() && !cache.isLeavingArena() && cache.getMode() != ArenaJoinMode.EDITING) {
					final Arena arena = cache.getArena();
					final Region region = arena.getSettings().getRegion();

					// Ignore dead players and when arena is stopping
					if (!player.isDead() && !arena.isStopping() && !arena.isStarting() && arena.isPlayed() && !region.isWithin(player.getLocation()))
						arena.leavePlayer(player, ArenaLeaveReason.ESCAPE);
				}
			}
		} catch (final Throwable t) {
			Common.error(t, "Error checking for player escape");
		}
	}
}
