package org.mineacademy.arena.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.event.inventory.ClickType;
import org.mineacademy.arena.model.ArenaJoinMode;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.arena.tool.ArenaTool;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.menu.tool.ToolRegistry;
import org.mineacademy.fo.remain.CompMaterial;

/**
 * The command to simulate clicking with arena tool
 */
public class SetPointCommand extends ArenaSubCommand {

	protected SetPointCommand() {
		super("setpoint", 1, "<left/right>", "Simulate clicking with arena tool.");
	}

	@Override
	protected void onCommand() {
		checkConsole();

		final ClickType click = ReflectionUtil.lookupEnumSilent(ClickType.class, args[0].toUpperCase());
		checkNotNull(click, "Please write either a 'right' or a 'left' click.");

		final ArenaPlayer cache = ArenaPlayer.getCache(getPlayer());
		checkBoolean(cache.getMode() == ArenaJoinMode.EDITING, "You may only use this command while editing an arena.");

		final Tool tool = ToolRegistry.getTool(getPlayer().getItemInHand());
		checkBoolean(tool instanceof ArenaTool, "You must be holding an arena tool! Use '/{label} tools' to get some.");

		final Block block = getPlayer().getLocation().add(0, -1, 0).getBlock();
		checkBoolean(CompMaterial.isAir(block), "You can only simulate clicking in air, otherwise click the block!");

		// Set a fake block so that we can process it in arena tool properly
		block.setType(CompMaterial.STONE.getMaterial());

		((ArenaTool<?>) tool).onBlockClick(getPlayer(), click, block);

		// Restore back to air
		block.setType(CompMaterial.AIR.getMaterial());

	}

	@Override
	protected List<String> tabComplete() {
		return new ArrayList<>();
	}
}
