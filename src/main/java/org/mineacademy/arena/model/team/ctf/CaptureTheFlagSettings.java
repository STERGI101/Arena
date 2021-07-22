package org.mineacademy.arena.model.team.ctf;

import java.util.List;

import org.bukkit.Location;
import org.mineacademy.arena.model.ArenaTeam;
import org.mineacademy.arena.model.ArenaTeamPoints;
import org.mineacademy.arena.model.team.TeamArenaSettings;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.Getter;

/**
 * Represents settings used in monster arenas
 */
public class CaptureTheFlagSettings extends TeamArenaSettings {

	/**
	 * How many times can the player punch an opposite teams crystal before
	 * destroying it
	 */
	@Getter
	private int crystalHitThreshold;

	/**
	 * The crystal spawn point for each team
	 */
	private ArenaTeamPoints crystalPoints;

	/**
	 * Create new arena settings
	 *
	 * @param arena
	 */
	public CaptureTheFlagSettings(final CaptureTheFlagArena arena) {
		super(arena);
	}

	/**
	 * @see org.mineacademy.arena.model.ArenaSettings#onLoadFinish()
	 */
	@Override
	protected void onLoadFinish() {
		super.onLoadFinish();

		this.crystalPoints = new ArenaTeamPoints(this, getMap("Team_Crystals"));
		this.crystalHitThreshold = getInteger("Crystal_Hit_Threshold", 30);
	}

	/**
	 * Set a new crystal hit threshold
	 *
	 * @param crystalHitThreshold the crystalHitThreshold to set
	 */
	public void setCrystalHitThreshold(final int crystalHitThreshold) {
		this.crystalHitThreshold = crystalHitThreshold;

		save();
	}

	/**
	 * Set the crystal spawn point at the given location, removing the old point
	 * since we only enable 1 crystal per team
	 *
	 * @param location
	 */
	public void setCrystal(final ArenaTeam team, final Location location) {
		crystalPoints.setPoint(team, location);
	}

	/**
	 * Add a new crystal
	 *
	 * @param location
	 */
	public void addCrystal(final ArenaTeam team, final Location location) {
		crystalPoints.addPoint(team, location);
	}

	/**
	 * Remove a crystal
	 *
	 * @param location
	 */
	public void removeCrystal(final ArenaTeam team, final Location location) {
		crystalPoints.removePoint(team, location);
	}

	/**
	 * Return true if the given crystal location exists
	 *
	 * @param location
	 * @return
	 */
	public boolean hasCrystal(final Location location) {
		return crystalPoints.hasPoint(location);
	}

	/**
	 * Return what team owns the crystal at the given location
	 *
	 * @param location
	 * @return
	 */
	public ArenaTeam findCrystalTeam(final Location location) {
		return crystalPoints.findTeam(location);
	}

	/**
	 * Get the crystal for the given team
	 *
	 * @param team
	 * @return
	 */
	public Location findCrystal(final ArenaTeam team) {
		return crystalPoints.findPoint(team);
	}

	/**
	 * Get a list of all crystal locations
	 *
	 * @return
	 */
	public List<Location> getCrystals() {
		return crystalPoints.getLocations();
	}

	/**
	 * @see org.mineacademy.arena.model.ArenaSettings#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = super.serialize();

		map.putArray(
				"Team_Crystals", crystalPoints,
				"Crystal_Hit_Threshold", crystalHitThreshold);

		return map;
	}
}
