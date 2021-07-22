package org.mineacademy.arena.tool;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.model.ArenaTeam;
import org.mineacademy.arena.model.team.TeamArena;
import org.mineacademy.arena.model.team.TeamArenaSettings;
import org.mineacademy.arena.util.Constants;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.menu.tool.Tool;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompMetadata;

import lombok.Getter;

/**
 * Represents a tool to create/remove team spawn points
 */
public class ToolTeamSpawnpoint extends VisualTool<TeamArena> {

	@Getter
	private static final Tool instance = new ToolTeamSpawnpoint();

	/**
	 * Create new tool for the arena type
	 */
	private ToolTeamSpawnpoint() {
		super(TeamArena.class);
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getBlockName(org.bukkit.block.Block, org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected String getBlockName(final Block block, final Player player, final TeamArena arena) {
		final ArenaTeam team = findTeam(block.getLocation(), arena);

		return "[" + team.getColor() + team.getName() + " team spawnpoint&f]";
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getBlockMask(org.bukkit.block.Block, org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected CompMaterial getBlockMask(final Block block, final Player player, final TeamArena arena) {
		final ArenaTeam team = findTeam(block.getLocation(), arena);

		return CompColor.toWool(team.getColor());
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.of(
				CompMaterial.CLAY_BALL,
				"&lTEAM SPAWNPOINT TOOL",
				"",
				"Click a block to set",
				"Click air to switch teams")
				.build().makeMenuTool();
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#handleBlockClick(org.bukkit.entity.Player, org.mineacademy.arena.model.Arena, org.bukkit.event.inventory.ClickType, org.bukkit.block.Block)
	 */
	@Override
	protected void handleBlockClick(final Player player, final TeamArena arena, final ClickType click, final Block block) {
		final ArenaTeam team = findTeam(player);

		if (team == null) {
			Messenger.error(player, "Could not find a team to edit. Right/left click air to select one.");

			return;
		}

		final TeamArenaSettings settings = arena.getSettings();
		final ArenaTeam oldTeam = settings.findTeam(block.getLocation());

		if (oldTeam != null && !oldTeam.equals(team)) {
			Messenger.error(player, "This block is already set as spawnpoint for the " + oldTeam.getName() + " team.");

			return;
		}

		settings.setSpawnpoint(team, block.getLocation());
		Messenger.success(player, "Placed a spawnpoint for the " + team.getColor() + team.getName() + " &7team.");
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.BlockTool#onAirClick(org.bukkit.entity.Player, org.bukkit.event.inventory.ClickType, org.bukkit.block.Block)
	 */
	@Override
	protected void handleAirClick(final Player player, final TeamArena arena, final ClickType click) {
		final List<ArenaTeam> teams = ArenaTeam.getTeams();

		if (teams.isEmpty()) {
			Messenger.error(player, "There are no teams created yet. Use '/arena teams' to create some.");

			return;
		}

		final ArenaTeam team = findTeam(player);
		final ArenaTeam next = Common.getNext(team, teams, click == ClickType.RIGHT);

		CompMetadata.setTempMetadata(player, Constants.Tag.TEAM_TOOL, next.getName());
		Messenger.success(player, "&7" + (click == ClickType.RIGHT ? ">>" : "<<") + " Now placing spawnpoints for the " + next.getColor() + next.getName() + " &7team.");
	}

	/**
	 * Finds what team the player is placing spawn points for, right now
	 */
	private ArenaTeam findTeam(final Player player) {
		if (CompMetadata.hasTempMetadata(player, Constants.Tag.TEAM_TOOL)) {
			final String teamName = CompMetadata.getTempMetadata(player, Constants.Tag.TEAM_TOOL).asString();

			return ArenaTeam.findTeam(teamName);
		}

		return null;
	}

	/**
	 * Find a team at the given location
	 *
	 * @param location
	 * @param arena
	 * @return
	 */
	private ArenaTeam findTeam(final Location location, final TeamArena arena) {
		final ArenaTeam team = arena.getSettings().findTeam(location);
		Valid.checkNotNull(team, "Spawnpoint at " + Common.shortLocation(location) + " refers to an unknown team!");

		return team;
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getVisualizedPoints(org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected List<Location> getVisualizedPoints(final TeamArena arena) {
		return arena.getSettings().getSpawnpoints();
	}

	@Override
	protected boolean autoCancel() {
		return true; // Cancel the event so that we don't destroy blocks when selecting them
	}
}
