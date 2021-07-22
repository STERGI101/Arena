package org.mineacademy.arena.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The mode in which the arena is in
 */
@RequiredArgsConstructor
public enum ArenaState {

	/**
	 * The arena is stopped with no players
	 */
	STOPPED("stopped"),

	/**
	 * The arena is starting and counting down to the {@link #PLAYED} mode
	 */
	LOBBY("lobby"),

	/**
	 * The arena is being played
	 */
	PLAYED("played"),

	/**
	 * The arena is being edited
	 */
	EDITED("edited");

	/**
	 * The localized key name
	 */
	@Getter
	private final String localized;
}
