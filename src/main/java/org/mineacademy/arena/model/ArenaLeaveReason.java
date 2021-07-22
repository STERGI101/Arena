package org.mineacademy.arena.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import lombok.Getter;

/**
 * The reason why a player is to leave an arena
 */
public enum ArenaLeaveReason {

	/**
	 * Arena has naturally ended its duration
	 */
	TIMER("The arena timer has run out! Thank you for playing.", Flag.REWARD),

	/**
	 * Player has died over the arena lives limit
	 */
	NO_LIVES_LEFT("You have died {deaths} times and the game is now over for you!", Flag.SPECTATE, Flag.REWARD),

	/**
	 * No class selected in time during lobby
	 */
	NO_CLASS("You did not select a class in time!"),

	/**
	 * No team selected in time during lobby
	 */
	NO_TEAM("You did not select a team in time!"),

	/**
	 * Lobby failed to start the arena due to min players threshold not met
	 */
	NOT_ENOUGH_PLAYERS("Arena could not start, lacking {lacking_players}!"),

	/**
	 * Player has disconnected
	 */
	DISCONNECT,

	/**
	 * The player was the last man alive
	 */
	LAST_STANDING("Congratulations for winning the arena {arena}!", Flag.REWARD),

	/**
	 * The players team was the last one to survive
	 */
	LAST_TEAM_STANDING("Congratulations! Your team won the game!", Flag.REWARD),

	/**
	 * All other teams are gone
	 */
	OTHER_TEAMS_LEFT("All other teams have left the arena!", Flag.REWARD),

	/**
	 * The team crystal got destroyed!
	 */
	CRYSTAL_DESTROYED("Your crystal got destroyed!", Flag.SPECTATE),

	/**
	 * Player has escaped the arena region
	 */
	ESCAPE("We have detected you moved away from the arena {arena} and your game is now over!"),

	/**
	 * Player used /arena leave command
	 */
	COMMAND("You have left the arena {arena}."),

	/**
	 * Player stopped editing the arena
	 */
	EDIT_STOP("You are no longer editing arena {arena}."),

	/**
	 * Arena stopped due to other causes
	 */
	ARENA_STOP("The arena {arena} has been stopped. Thank you for playing.");

	/**
	 * The message told to the player, may be null
	 */
	@Getter
	@Nullable
	private final String message;

	/**
	 * List of options for this class
	 */
	private List<Flag> flags;

	/**
	 * Create a new reason not having a message
	 */
	private ArenaLeaveReason() {
		this(null);
	}

	private ArenaLeaveReason(String message, Flag... flags) {
		this.message = message;
		this.flags = flags != null ? Arrays.asList(flags) : new ArrayList<>();
	}

	/**
	 * Can the player spectate the arena after leaving for this reason?
	 *
	 * @return the canSpectate
	 */
	public boolean canSpectate() {
		return flags.contains(Flag.SPECTATE);
	}

	/**
	 * Can the player receive reward after leaving for this reason?
	 *
	 * @return
	 */
	public boolean canReward() {
		return flags.contains(Flag.REWARD);
	}

	private enum Flag {
		REWARD,
		SPECTATE
	}
}
