package org.mineacademy.arena.command;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.arena.tool.ArenaTool;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.MenuTools;
import org.mineacademy.fo.plugin.SimplePlugin;

/**
 * The command to obtain arena setup tools
 */
public class ToolsCommand extends ArenaSubCommand {

	protected ToolsCommand() {
		super("tools|t", "Goodies for editing arenas.");
	}

	@Override
	protected void onCommand() {
		checkConsole();

		final ArenaPlayer cache = ArenaPlayer.getCache(getPlayer());
		checkBoolean(cache.hasArena() && cache.getMode() == ArenaJoinMode.EDITING, "You may only use this command while editing an arena. Use '/arena edit' first.");

		new MenuTools() {

			@SuppressWarnings("rawtypes")
			@Override
			protected Object[] compileTools() {
				Valid.checkNotNull(cache, "Cache cannot be null!");
				Valid.checkBoolean(cache.hasArena(), "Cannot open tools if the player lacks an arena!");

				final List<Object> instances = new ArrayList<>();

				for (final Class<? extends ArenaTool> tool : ReflectionUtil.getClasses(SimplePlugin.getInstance(), ArenaTool.class)) {
					if (Modifier.isAbstract(tool.getModifiers()))
						continue;

					final ArenaTool instance = ReflectionUtil.getFieldContent(tool, "instance", null);

					if (instance.isApplicable(cache.getArena()))
						instances.add(instance);
				}

				return instances.toArray();
			}

			@Override
			protected String[] getInfo() {
				return new String[] {
						"Use these goodies to fancy",
						"up your arenas with ease!"
				};
			}
		}.displayTo(getPlayer());
	}

	@Override
	protected List<String> tabComplete() {
		return new ArrayList<>();
	}
}
