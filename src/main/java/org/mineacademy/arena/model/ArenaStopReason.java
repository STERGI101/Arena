package org.mineacademy.arena.model;

/**
 * Represents a reason why the arena has stopped
 */
public enum ArenaStopReason {

	/**
	 * Ended naturally due to game duration ran out
	 */
	TIMER,

	/**
	 * Ended due to command
	 */
	COMMAND,

	/**
	 * Was empty so stopped
	 */
	LAST_PLAYER_LEFT,

	/**
	 * Last team standing won
	 */
	LAST_TEAM_STANDING,

	/**
	 * All other teams have left the arena
	 */
	OTHER_TEAMS_LEFT,

	/**
	 * Lobby players were under the min players limit
	 */
	NOT_ENOUGH_PLAYERS,

	/**
	 * Error occured
	 */
	ERROR,

	/**
	 * Plugin or server reload
	 */
	RELOAD,

	/**
	 * Other reason such as another plugin using our API or simply server shutting down
	 */
	PLUGIN
}
