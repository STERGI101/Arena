package org.mineacademy.arena.model;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.Countdown;

/**
 * The countdown responsible for starting arenas
 */
public class ArenaCountdownStart extends Countdown {

	/**
	 * The arena that to start
	 */
	private final Arena arena;

	/**
	 * Create a new start countdown
	 *
	 * @param arena
	 */
	protected ArenaCountdownStart(final Arena arena) {
		super(arena.getSettings().getLobbyDuration());

		this.arena = arena;
	}

	/**
	 * Called automatically each tick closer to the start
	 * 1 second by default
	 *
	 * @see org.mineacademy.fo.model.Countdown#onTick()
	 */
	@Override
	protected void onTick() {
		// Broadcast every fifth second or every second when there are 5 or less seconds left
		if (getTimeLeft() <= 5 || getTimeLeft() % 10 == 0)
			arena.broadcastWarn("Arena starts in less than " + Common.plural(getTimeLeft(), "second"));
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
	 *
	 * @see org.mineacademy.fo.model.Countdown#onEnd()
	 */
	@Override
	protected void onEnd() {
		arena.startArena();
	}
}
