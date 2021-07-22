package org.mineacademy.arena.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.mineacademy.fo.SerializeUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.model.ConfigSerializable;

import lombok.RequiredArgsConstructor;

/**
 * Represents a model for team points in the arena
 *
 * We enable only 1 point per team
 */
@RequiredArgsConstructor
public final class ArenaTeamPoints implements ConfigSerializable {

	/**
	 * The arena settings where we have these points
	 */
	private final ArenaSettings settings;

	/**
	 * The crystal spawn point for each team
	 */
	private final SerializedMap /*location, team name*/ points;

	/**
	 * Set the point at the given location, removing the old point
	 * since we only enable 1 point per team
	 *
	 * @param location
	 */
	public void setPoint(final ArenaTeam team, final Location location) {
		final String teamName = team.getName();

		for (final Map.Entry<String, Object> entry : points.entrySet())
			if (entry.getValue().equals(teamName)) {
				points.remove(entry.getKey());

				break;
			}

		points.put(SerializeUtil.serializeLoc(location), team.getName());
		settings.save();
	}

	/**
	 * Add a new point
	 *
	 * @param location
	 */
	public void addPoint(final ArenaTeam team, final Location location) {
		Valid.checkBoolean(!hasPoint(location), "Point at " + location + " already exists!");

		points.put(SerializeUtil.serializeLoc(location), team.getName());
		settings.save();
	}

	/**
	 * Remove a point
	 *
	 * @param location
	 */
	public void removePoint(final ArenaTeam team, final Location location) {
		Valid.checkBoolean(hasPoint(location), "Point at " + location + " does not exist!");

		points.asMap().remove(SerializeUtil.serializeLoc(location));
		settings.save();
	}

	/**
	 * Return true if the given location exists
	 *
	 * @param location
	 * @return
	 */
	public boolean hasPoint(final Location location) {
		return findTeam(location) != null;
	}

	/**
	 * Return what team owns the point at the given location
	 *
	 * @param location
	 * @return
	 */
	public ArenaTeam findTeam(final Location location) {
		final String teamName = points.getString(SerializeUtil.serializeLoc(location));

		return teamName != null ? ArenaTeam.findTeam(teamName) : null;
	}

	/**
	 * Get the point for the given team
	 *
	 * @param team
	 * @return
	 */
	public Location findPoint(final ArenaTeam team) {
		final List<Location> teamPoints = new ArrayList<>();

		for (final Map.Entry<String, Object> entry : points.entrySet()) {
			final Location location = SerializeUtil.deserializeLocation(entry.getKey());
			final ArenaTeam teamAtLocation = ArenaTeam.findTeam((String) entry.getValue());

			if (team.equals(teamAtLocation))
				teamPoints.add(location);
		}

		if (teamPoints.isEmpty())
			return null;

		Valid.checkBoolean(teamPoints.size() == 1, "Cannot have more than 1 point for team " + team.getName() + "!");
		return teamPoints.get(0);
	}

	/**
	 * Get a list of all point locations
	 *
	 * @return
	 */
	public List<Location> getLocations() {
		final List<Location> locations = new ArrayList<>();

		for (final String location : points.keySet())
			locations.add(SerializeUtil.deserializeLocation(location));

		return locations;
	}

	/**
	 * Return locations
	 *
	 * @return all locations
	 */
	public SerializedMap getPoints() {
		return points;
	}

	/**
	 * @see org.mineacademy.fo.model.ConfigSerializable#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		return points;
	}
}
