package org.mineacademy.arena.settings;

import java.util.List;

import org.mineacademy.fo.Common;
import org.mineacademy.fo.model.SimpleTime;
import org.mineacademy.fo.settings.SimpleSettings;

public class Settings extends SimpleSettings {

	@Override
	protected int getConfigVersion() {
		return 1;
	}

	/**
	 * The settings for rotating arenas
	 * See the Orion plugin on how to incorporate this to settings.yml
	 */
	public static class Rotate {

		/**
		 * Enable the rotate system?
		 */
		public static Boolean ENABLED = true;

		/**
		 * What installed arenas to rotate?
		 */
		public static List<String> ARENAS = Common.toList("ctf", "demo", "dm");

		/**
		 * How long to wait between one arena stopping and the next in rotation to start?
		 */
		public static SimpleTime DELAY_BETWEEN_ARENAS = SimpleTime.from("5 seconds");
	}
}
