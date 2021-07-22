package org.mineacademy.arena.model.team.tdm;

import org.mineacademy.arena.model.team.TeamArena;

/**
 * Represents a simple team deathmatch where teams fight until death and
 * the last standing team wins the arena
 */
public class TeamDeathmatchArena extends TeamArena {

	/**
	 * The arena unique identification type
	 */
	public static final String TYPE = "teamdeathmatch";

	/**
	 * Create a new team deathmatch arena
	 *
	 * @param name
	 */
	public TeamDeathmatchArena(final String name) {
		super(TYPE, name);
	}

}
