package org.mineacademy.arena.command;

import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.command.ReloadCommand;
import org.mineacademy.fo.command.SimpleCommandGroup;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * The main /arena command
 */
public class ArenaCommandGroup extends SimpleCommandGroup {

	@Override
	protected void registerSubcommands() {

		// Auto-register all sub commands
		for (final Class<? extends ArenaSubCommand> clazz : ReflectionUtil.getClasses(SimplePlugin.getInstance(), ArenaSubCommand.class)) {
			//Common.log("Registering command " + clazz.getSimpleName());

			registerSubcommand(ReflectionUtil.instantiate(clazz));
		}

		// Register the premade reload command from Foundation, remove if you want to have your own
		registerSubcommand(new ReloadCommand());
	}

}
