package org.mineacademy.arena.model;

import java.util.Arrays;
import java.util.List;

import org.mineacademy.fo.TimeUtil;
import org.mineacademy.fo.model.Countdown;

import lombok.Getter;

/**
 * The countdown responsible for ticking played arenas
 */
public class ArenaHeartbeat extends Countdown {

	/**
	 * The arena that to tick
	 */
	@Getter
	private final Arena arena;

	/**
	 * Create a new countdown
	 *
	 * @param arena
	 */
	protected ArenaHeartbeat(final Arena arena) {
		super(arena.getSettings().getGameDuration());

		this.arena = arena;
	}

	/**
	 * Called automatically on startup
	 */
	@Override
	protected void onStart() {
	}

	/**
	 * Called automatically each tick 1 second by default
	 */
	@Override
	protected void onTick() {
		final List<Integer> broadcastTimes = Arrays.asList(20, 30, 60);

		if (getTimeLeft() % 120 == 0 || getTimeLeft() <= 10 || broadcastTimes.contains(getTimeLeft()))
			arena.broadcastWarn("Arena ends in less than " + TimeUtil.formatTimeGeneric(getTimeLeft()));
	}

	/**
	 * Stop the arena if the ticking fails
	 *
	 * @param t
	 */
	@Override
	protected void onTickError(final Throwable t) {
		arena.stopArena(ArenaStopReason.ERROR);
	}

	/**
	 * Called when the countdown runs up yo!
	 */
	@Override
	protected void onEnd() {
		arena.stopArena(ArenaStopReason.TIMER);
	}
}
