package org.mineacademy.arena.model.dm;

import org.mineacademy.arena.model.Arena;
import org.mineacademy.arena.model.ArenaSettings;
import org.mineacademy.fo.collection.SerializedMap;

import lombok.Getter;

/**
 * Represents settings used in deathmatch arenas
 */
@Getter
public class DeathmatchSettings extends ArenaSettings {

	/**
	 * The list of all entrance locations in the arena
	 */
	private LocationList entrances;

	/**
	 * Create new arena settings
	 *
	 * @param arena
	 */
	public DeathmatchSettings(final Arena arena) {
		super(arena);
	}

	/**
	 * @see org.mineacademy.arena.model.ArenaSettings#onLoadFinish()
	 */
	@Override
	protected void onLoadFinish() {
		super.onLoadFinish();

		this.entrances = getLocations("Entrance_Locations");
	}

	/**
	 * @see org.mineacademy.arena.model.ArenaSettings#isSetup()
	 */
	@Override
	public boolean isSetup() {
		return super.isSetup() && entrances.size() >= getMaxPlayers();
	}

	/**
	 * @see org.mineacademy.arena.model.ArenaSettings#serialize()
	 */
	@Override
	public SerializedMap serialize() {
		final SerializedMap map = super.serialize();

		map.putArray("Entrance_Locations", entrances);

		return map;
	}
}
