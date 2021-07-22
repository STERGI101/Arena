package org.mineacademy.arena.model;

/**
 * A simple exception to throw in the arena pipeline to mark the end of it.
 *
 * Example: Arena has the onPlayerSpawn method which can be overridden by any parent class
 * 			such as MobArena. If the method however, is finished handling in Arena,
 * 			it should not be executed for MobArena. Then we throw this exception and thus
 * 			the method pipeline will end.
 */
public final class ArenaPipelineEndException extends RuntimeException {

	private static final long serialVersionUID = 1L;
}
