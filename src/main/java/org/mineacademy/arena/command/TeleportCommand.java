package org.mineacademy.arena.command;

import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaManager;
import org.mineacademy.arena.util.Constants;
import org.mineacademy.fo.region.Region;
import org.mineacademy.fo.remain.CompMetadata;

/**
 * Quickly teleport to the arena
 */
public class TeleportCommand extends ArenaSubCommand {

	protected TeleportCommand() {
		super("teleport|tp", "Teleport to the arena center.");

		setUsage("[arena]");
	}

	@Override
	protected void onCommand() {
		checkConsole();

		Arena arena = null;

		// Automatically find arena by players location if he only types /arena join
		if (args.length == 0) {
			arena = ArenaManager.findArena(getPlayer().getLocation());

			checkNotNull(arena, "Could not find an arena, please specify its name.");
		} else
			arena = findArena(args[0]);

		final Region region = arena.getSettings().getRegion();
		checkBoolean(region != null && region.isWhole(), "The arena region is not finished!");

		CompMetadata.setTempMetadata(getPlayer(), Constants.Tag.TELEPORT_EXEMPTION);
		getPlayer().teleport(region.getCenter());
		CompMetadata.removeTempMetadata(getPlayer(), Constants.Tag.TELEPORT_EXEMPTION);

		tellSuccess("Teleported to the arena center.");
	}
}
