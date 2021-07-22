package org.mineacademy.arena.localization;

import org.mineacademy.fo.settings.SimpleLocalization;

public class Localization extends SimpleLocalization {
	@Override
	protected int getConfigVersion() {
		return 1;
	}

	public static class Commands {
		public static String CLASSES_DESCRIPTION;

		private static void init(){
			pathPrefix("Commands");

			CLASSES_DESCRIPTION = getString("Classes_Description");
		}

	}

	public static class Class_Selection{
		public static String INGAME_CLASSSELECTION_ERROR;

		private static void init(){
			pathPrefix("Class_Selection");

			INGAME_CLASSSELECTION_ERROR = getString("InGame_ClassSelection_ERROR");

		}

	}
}
