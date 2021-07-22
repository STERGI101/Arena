package org.mineacademy.arena.util;

import lombok.experimental.UtilityClass;

/**
 * Holds unmodifiable string constants for our arena
 */
@UtilityClass
public class Constants {

	/**
	 * Holds constants relevant to tagging (using invisible metadata)
	 */
	@UtilityClass
	public class Tag {

		/**
		 * Players having this metadata tag may be teleported without restrictions set in {@link org.mineacademy.arena.model.ArenaListener}
		 */
		public final String TELEPORT_EXEMPTION = "TeleportExemption";

		/**
		 * The metadata for the team tool used to switch what team we are editing right now
		 */
		public final String TEAM_TOOL = "TeamTool";

		/**
		 * The metadata for the team crystal tool used to place team crystals
		 */
		public final String TEAM_CRYSTAL = "TeamCrystal";

		/**
		 * What is the damage of the crystal?
		 */
		public final String CRYSTAL_DAMAGE = "CrystalDamage";

		/**
		 * Is the crystal alive?
		 */
		public final String CRYSTAL_ALIVE = "CrystalAlive";

		/**
		 * Custom entrance location for player
		 */
		public final String ENTRANCE_LOCATION = "EntranceLocation";
	}

	/**
	 * Holds default values
	 */
	@UtilityClass
	public class Defaults {

		/**
		 * The default price when creating a new reward item
		 */
		public final double ITEM_REWARD_PRICE = 10;

		/**
		 * The default price when creating a new class tier to upgrade it
		 */
		public final double TIER_UPGRADE_PRICE = 100;

	}
}
