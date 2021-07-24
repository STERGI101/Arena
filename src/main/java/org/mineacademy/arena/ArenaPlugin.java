package org.mineacademy.arena;

import lombok.Getter;
import org.mineacademy.arena.command.ArenaCommandGroup;
import org.mineacademy.arena.settings.Localization;
import org.mineacademy.arena.model.*;
import org.mineacademy.arena.model.dm.DeathmatchArena;
import org.mineacademy.arena.model.eggwars.EggWarsArena;
import org.mineacademy.arena.model.monster.MobArena;
import org.mineacademy.arena.model.team.ctf.CaptureTheFlagArena;
import org.mineacademy.arena.model.team.tdm.TeamDeathmatchArena;
import org.mineacademy.arena.settings.Settings;
import org.mineacademy.arena.task.EscapeTask;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.command.SimpleCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.plugin.SimplePlugin;
import org.mineacademy.fo.settings.YamlStaticConfig;

import java.util.Arrays;
import java.util.List;

/**
 * The core plugin class for Arena
 */
public final class ArenaPlugin extends SimplePlugin {

	/**
	 * The main arena command
	 */
	@Getter
	private final SimpleCommandGroup mainCommand = new ArenaCommandGroup();

	/**
	 * Register arena types early
	 */
	@Override
	protected void onPluginPreStart() {
		// Register our arenas
		ArenaManager.registerArenaType(MobArena.class);
		ArenaManager.registerArenaType(DeathmatchArena.class);
		ArenaManager.registerArenaType(TeamDeathmatchArena.class);
		ArenaManager.registerArenaType(CaptureTheFlagArena.class);
		ArenaManager.registerArenaType(EggWarsArena.class);
	}

	/**
	 * Load the plugin and its configuration
	 */
	@Override
	protected void onPluginStart() {

		// Connect to MySQL
		// TODO Change this to be your own MySQL credentials, this won't work!
		//ArenaDatabase.start("mysql57.websupport.sk", 3311, "projectorion", "projectorion", "Te7=cXvxQI");

		// Enable messages prefix
		Common.ADD_TELL_PREFIX = true;

		// Use themed messages in commands
		SimpleCommand.USE_MESSENGER = true;

		Common.runLater(ArenaManager::loadArenas); // Uncomment this line if your arena world is loaded by a third party plugin such as Multiverse
	}

	/**
	 * Called on startup and reload, load arenas
	 */
	@Override
	protected void onReloadablesStart() {
		//ArenaManager.loadArenas(); // Comment this line if your arena world is loaded by a third party plugin such as Multiverse
		ArenaClass.loadClasses();
		ArenaTeam.loadTeams();

		ArenaReward.getInstance(); // Loads the file
		ArenaPlayer.clearAllData();

		registerEvents(new ArenaListener());

		Common.runTimer(20, new EscapeTask());
	}

	/**
	 * Stop arenas on server stop
	 */
	@Override
	protected void onPluginStop() {
		ArenaManager.stopArenas(ArenaStopReason.PLUGIN);
	}

	/**
	 * Stop arenas on reload
	 */
	@Override
	protected void onPluginReload() {
		ArenaManager.stopArenas(ArenaStopReason.RELOAD);
		ArenaManager.loadArenas(); // Uncomment this line if your arena world is loaded by a third party plugin such as Multiverse
	}

	/**
	 * Load the global settings classes
	 */
	@Override
	public List<Class<? extends YamlStaticConfig>> getSettings() {
		return Arrays.asList(Settings.class, Localization.class);
	}
}
