package org.mineacademy.arena.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * What mode is the player joined the arena in?
 */
@RequiredArgsConstructor
public enum ArenaJoinMode {

	/**
	 * The player is playing the arena
	 */
	PLAYING("playing"),

	/**
	 * The player is editing the arena
	 */
	EDITING("editing"),

	/**
	 * The player is dead and now spectating the arena
	 */
	SPECTATING("spectating");

	@Getter
	private final String localized;
}
