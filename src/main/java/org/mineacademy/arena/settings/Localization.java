package org.mineacademy.arena.settings;

import org.mineacademy.fo.model.Replacer;
import org.mineacademy.fo.settings.SimpleLocalization;

public class Localization extends SimpleLocalization {

	// TODO


	@Override
	protected int getConfigVersion() {
		return 1;
	}

	public static class Commands {
		public static String CLASSES_DESCRIPTION;
		public static String CLASSES_EDIT_PERM_ERROR;
		public static String EDIT_DESCRIPTION;
		public static String EDIT_ARENA_NOT_FOUND;
		public static String JOIN_DESCRIPTION;
		public static String JOIN_ARENA_NOT_FOUND;
		public static String LEAVE_DESCRIPTION;
		public static String LIST_DESCRIPTION;
		public static Replacer LIST_ERROR;
		public static String NEW_DESCRIPTION;
		public static Replacer NEW_ERROR;
		public static Replacer NEW_SUCCESS;
		public static String REMOVE_DESCRIPTION;
		public static Replacer REMOVE_SUCCESS;
		public static Replacer FIND_ARENA_ERROR;
		public static String PLAYER_NOT_IN_ARENA;
		public static Replacer PLAYER_IN_ARENAMODE_WHILE_EDITING;

		private static void init(){
			pathPrefix("Commands");

			CLASSES_DESCRIPTION = getString("Classes_Description");
			CLASSES_EDIT_PERM_ERROR = getString("Classes_Edit_Perm_Error");
			EDIT_DESCRIPTION = getString("Edit_Description");
			EDIT_ARENA_NOT_FOUND = getString("Edit_Arena_Not_Found");
			JOIN_DESCRIPTION = getString("Join_Description");
			JOIN_ARENA_NOT_FOUND = getString("Join_Arena_Not_Found");
			LEAVE_DESCRIPTION = getString("Leave_Description");
			LIST_DESCRIPTION = getString("List_Description");
			LIST_ERROR = getReplacer("List_Error");
			NEW_DESCRIPTION = getString("New_Description");
			NEW_ERROR = getReplacer("New_Error");
			NEW_SUCCESS = getReplacer("New_Success");
			REMOVE_DESCRIPTION = getString("Remove_Description");
			FIND_ARENA_ERROR = getReplacer("Find_Arena_Error");
			PLAYER_NOT_IN_ARENA = getString("Player_Not_In_Arena");
			PLAYER_IN_ARENAMODE_WHILE_EDITING = getReplacer("Player_In_ArenaMode_While_Editing");
		}

	}

	public static class Arenas {


		private static void init(){
			pathPrefix("Arenas");


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
