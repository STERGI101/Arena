package org.mineacademy.arena.tool;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.model.ArenaTeam;
import org.mineacademy.arena.model.team.ctf.CaptureTheFlagArena;
import org.mineacademy.arena.model.team.ctf.CaptureTheFlagSettings;
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
 * Represents a tool to create/remove team points where a crystal spawns
 * for {@link CaptureTheFlagArena}
 */
public class ToolTeamCrystal extends VisualTool<CaptureTheFlagArena> {

	@Getter
	private static final Tool instance = new ToolTeamCrystal();

	/**
	 * Create new tool for the arena type
	 */
	private ToolTeamCrystal() {
		super(CaptureTheFlagArena.class);
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getBlockName(org.bukkit.block.Block, org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected String getBlockName(final Block block, final Player player, final CaptureTheFlagArena arena) {
		final ArenaTeam team = findTeam(block.getLocation(), arena);

		return "&0[" + team.getColor() + team.getName() + " crystal&0]";
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getBlockMask(org.bukkit.block.Block, org.bukkit.entity.Player, org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected CompMaterial getBlockMask(final Block block, final Player player, final CaptureTheFlagArena arena) {
		final ArenaTeam team = findTeam(block.getLocation(), arena);

		return CompColor.toConcrete(team.getColor());
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.Tool#getItem()
	 */
	@Override
	public ItemStack getItem() {
		return ItemCreator.of(
				CompMaterial.END_CRYSTAL,
				"&lCRYSTAL SPAWNPOINT TOOL",
				"",
				"Click a block to set",
				"Click air to switch teams",
				"",
				"The crystal is the main",
				"objective to destroy",
				"of the opposite team")
				.build().makeMenuTool();
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#handleBlockClick(org.bukkit.entity.Player, org.mineacademy.arena.model.Arena, org.bukkit.event.inventory.ClickType, org.bukkit.block.Block)
	 */
	@Override
	protected void handleBlockClick(final Player player, final CaptureTheFlagArena arena, final ClickType click, final Block block) {
		final ArenaTeam team = findTeam(player);

		if (team == null) {
			Messenger.error(player, "Could not find a team to edit. Right/left click air to select one.");

			return;
		}

		final CaptureTheFlagSettings settings = arena.getSettings();
		final ArenaTeam oldTeam = settings.findCrystalTeam(block.getLocation());

		if (oldTeam != null && !oldTeam.equals(team)) {
			Messenger.error(player, "This block is already set as crystal for the " + oldTeam.getName() + " team.");

			return;
		}

		settings.setCrystal(team, block.getLocation());
		Messenger.success(player, "Placed a crystal for the " + team.getColor() + team.getName() + " &7team.");
	}

	/**
	 * @see org.mineacademy.fo.menu.tool.BlockTool#onAirClick(org.bukkit.entity.Player, org.bukkit.event.inventory.ClickType, org.bukkit.block.Block)
	 */
	@Override
	protected void handleAirClick(final Player player, final CaptureTheFlagArena arena, final ClickType click) {
		final List<ArenaTeam> teams = ArenaTeam.getTeams();

		if (teams.isEmpty()) {
			Messenger.error(player, "There are no teams created yet. Use '/arena teams' to create some.");

			return;
		}

		final ArenaTeam team = findTeam(player);
		final ArenaTeam next = Common.getNext(team, teams, click == ClickType.RIGHT);

		CompMetadata.setTempMetadata(player, Constants.Tag.TEAM_CRYSTAL, next.getName());
		Messenger.success(player, "&7" + (click == ClickType.RIGHT ? ">>" : "<<") + " Now placing crystal for the " + next.getColor() + next.getName() + " &7team.");
	}

	/**
	 * Finds what team the player is placing spawn points for, right now
	 */
	private ArenaTeam findTeam(final Player player) {
		if (CompMetadata.hasTempMetadata(player, Constants.Tag.TEAM_CRYSTAL)) {
			final String teamName = CompMetadata.getTempMetadata(player, Constants.Tag.TEAM_CRYSTAL).asString();

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
	private ArenaTeam findTeam(final Location location, final CaptureTheFlagArena arena) {
		final ArenaTeam team = arena.getSettings().findCrystalTeam(location);
		Valid.checkNotNull(team, "Crystal at " + Common.shortLocation(location) + " refers to an unknown team!");

		return team;
	}

	/**
	 * @see org.mineacademy.arena.tool.VisualTool#getVisualizedPoints(org.mineacademy.arena.model.Arena)
	 */
	@Override
	protected List<Location> getVisualizedPoints(final CaptureTheFlagArena arena) {
		return arena.getSettings().getCrystals();
	}

	@Override
	protected boolean autoCancel() {
		return true; // Cancel the event so that we don't destroy blocks when selecting them
	}
}
