package org.mineacademy.arena.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.model.team.TeamArena;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.PlayerUtil;
import org.mineacademy.fo.RandomUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.remain.CompColor;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.settings.YamlConfig;

import lombok.Getter;
import lombok.NonNull;

/**
 * Represents an arena team
 */
public final class ArenaTeam extends YamlConfig {

	/**
	 * The list of loaded teams
	 */
	private static volatile List<ArenaTeam> loadedTeams = new ArrayList<>();

	/**
	 * The color for this team
	 */
	@Getter
	private ChatColor color;

	/**
	 * The icon to display in the menu
	 */
	private ItemStack icon;

	/**
	 * A list of applicable arenas where this team may be applied
	 */
	private List<String> applicableArenas;

	/**
	 * The permission to use this team
	 */
	@Getter
	private String permission;

	/**
	 * Creat a new arena team by the name
	 *
	 * @param name
	 */
	private ArenaTeam(final String name) {

		loadConfiguration(NO_DEFAULT, "teams/" + name + ".yml");
	}

	/**
	 * @see org.mineacademy.fo.settings.YamlConfig#onLoadFinish()
	 */
	@Override
	protected void onLoadFinish() {
		this.color = get("Color", ChatColor.class, RandomUtil.nextChatColor());
		this.icon = get("Icon", ItemStack.class);
		this.applicableArenas = getStringList("Applicable_Arenas");
		this.permission = getString("Permission");
	}

	@Override
	public void save() {
		final SerializedMap map = SerializedMap.ofArray("Applicable_Arenas", applicableArenas);

		// Enable null values
		map.asMap().put("Color", color);
		map.asMap().put("Icon", icon);
		map.asMap().put("Permission", permission);

		for (final Map.Entry<String, Object> entry : map.entrySet())
			setNoSave(entry.getKey(), entry.getValue());

		super.save();
	}

	/**
	 * Assign this team to the given player
	 *
	 * @param player
	 */
	public void assignTo(final Player player) {
		final ArenaPlayer cache = ArenaPlayer.getCache(player);
		Valid.checkBoolean(cache.hasArena() && cache.getMode() == ArenaJoinMode.PLAYING, "Teams may only be selected when playing an arena");

		final Arena arena = cache.getArena();
		Valid.checkBoolean(canAssign(player, arena), "Player " + player.getName() + " may not be assigned team " + getName());
		Valid.checkBoolean(arena.hasTeams(), "Arena " + arena.getName() + " does not support teams!");

		cache.setArenaTeam(this);
	}

	/**
	 * Return if the player may get this team
	 *
	 * @param player
	 * @param arena
	 * @return
	 */
	public boolean canAssign(final Player player, final Arena arena) {
		if (!PlayerUtil.hasPerm(player, permission))
			return false;

		if (!applicableArenas.contains(arena.getName()) && !applicableArenas.isEmpty())
			return false;

		return true;
	}

	/**
	 * Set the team color
	 *
	 * @param color the color to set
	 */
	public void setColor(final ChatColor color) {
		Valid.checkBoolean(!color.isFormat(), "Cannot set team colors that are format: " + color.name());
		this.color = color;

		save();
	}

	/**
	 * Set the team icon
	 *
	 * @param icon the icon to set
	 */
	public void setIcon(final ItemStack icon) {
		this.icon = icon;

		save();
	}

	/**
	 * Return the icon, or the default one if not set
	 *
	 * @return
	 */
	public ItemStack getIcon() {
		return icon != null && icon.getType() != Material.AIR ? icon : CompMaterial.makeWool(CompColor.fromChatColor(color), 1);
	}

	/**
	 * Adds or removes the given arena from applicable depending on if it was there previously
	 *
	 * @param arena
	 * @return true if the arena was added, false if removed
	 */
	public boolean toggleApplicableArena(final Arena arena) {
		final String arenaName = arena.getName();
		final boolean has = applicableArenas.contains(arenaName);

		if (has)
			applicableArenas.remove(arenaName);
		else
			applicableArenas.add(arenaName);

		save();
		return !has;
	}

	/**
	 * Add arena in which this team can be used
	 *
	 * @param arena
	 */
	public void addApplicableArena(final Arena arena) {
		Valid.checkBoolean(!applicableArenas.contains(arena.getName()), "Applicable arenas already contain " + arena);

		applicableArenas.add(arena.getName());
		save();
	}

	/**
	 * Remove arena in which this team can be used
	 *
	 * @param arena
	 */
	public void removeApplicableArena(final Arena arena) {
		Valid.checkBoolean(applicableArenas.contains(arena.getName()), "Applicable arenas do not contain " + arena);

		applicableArenas.remove(arena.getName());
		save();
	}

	/**
	 * Get the list of arenas this team can be used in
	 *
	 * @return the applicableArenas
	 */
	public List<String> getApplicableArenas() {
		return Collections.unmodifiableList(applicableArenas);
	}

	/**
	 * Set the permission to use this team, set to null to allow everyone
	 *
	 * @param permission the permission to set
	 */
	public void setPermission(final String permission) {
		this.permission = permission;

		save();
	}

	/**
	 * Return a list of players in this team in the arena
	 *
	 * @param arena
	 * @return
	 */
	public List<ArenaPlayer> getPlayers(final TeamArena arena) {
		return arena.getTeamPlayers(this);
	}

	@Override
	public boolean equals(final Object object) {
		return object instanceof ArenaTeam && ((ArenaTeam) object).getName().equals(this.getName());
	}

	@Override
	public String toString() {
		return "ArenaTeam{" + getName() + "}";
	}

	// ------–------–------–------–------–------–------–------–------–------–------–------–
	// Static
	// ------–------–------–------–------–------–------–------–------–------–------–------–

	/**
	 * Load all teams in the plugin
	 */
	public static void loadTeams() {
		loadedTeams.clear();

		final File[] files = FileUtil.getFiles("teams", "yml");

		for (final File file : files) {
			final String name = FileUtil.getFileName(file);

			loadOrCreateTeam(name);
		}
	}

	/**
	 * Load or creates a new team
	 *
	 * @param name
	 * @return
	 */
	public static ArenaTeam loadOrCreateTeam(final String name) {
		Valid.checkBoolean(!isTeamLoaded(name), "Team " + name + " is already loaded: " + getTeamNames());

		try {
			final ArenaTeam team = new ArenaTeam(name);
			loadedTeams.add(team);

			Common.log("[+] Loaded team " + team.getName());
			return team;

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to load team " + name);
		}

		return null;
	}

	/**
	 * Permanently delete a team
	 *
	 * @param team
	 */
	public static void removeTeam(@NonNull final ArenaTeam team) {
		Valid.checkBoolean(isTeamLoaded(team.getName()), "Team " + team.getName() + " not loaded. Available: " + getTeamNames());

		team.delete();

		loadedTeams.remove(team);
	}

	/**
	 * Return true if the given team exists
	 *
	 * @param name
	 * @return
	 */
	public static boolean isTeamLoaded(final String name) {
		return findTeam(name) != null;
	}

	/**
	 * Find a team by name
	 *
	 * @param name
	 * @return
	 */
	public static ArenaTeam findTeam(@NonNull final String name) {
		for (final ArenaTeam team : loadedTeams)
			if (team.getName().equalsIgnoreCase(name))
				return team;

		return null;
	}

	/**
	 * Get all loaded teams
	 *
	 * @return
	 */
	public static List<ArenaTeam> getTeams() {
		return Collections.unmodifiableList(loadedTeams);
	}

	/**
	 * Get all loaded team names
	 *
	 * @return
	 */
	public static List<String> getTeamNames() {
		return Common.convert(loadedTeams, ArenaTeam::getName);
	}
}
