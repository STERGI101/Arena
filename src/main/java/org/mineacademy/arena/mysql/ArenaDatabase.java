package org.mineacademy.arena.mysql;

import org.bukkit.entity.Player;
import org.mineacademy.arena.model.ArenaPlayer;
import org.mineacademy.fo.collection.SerializedMap;
import org.mineacademy.fo.database.SimpleFlatDatabase;

/**
 * Represents a database connection
 */
public class ArenaDatabase extends SimpleFlatDatabase<ArenaPlayer> {

	/**
	 * The instance
	 */
	private static final ArenaDatabase instance = new ArenaDatabase();

	private ArenaDatabase() {
		addVariable("table", "Arena");
	}

	/**
	 * @see org.mineacademy.fo.database.SimpleFlatDatabase#onLoad(org.mineacademy.fo.collection.SerializedMap, java.lang.Object)
	 */
	@Override
	protected void onLoad(final SerializedMap map, final ArenaPlayer data) {
		data.loadFromMySQL(map);
	}

	/**
	 * @see org.mineacademy.fo.database.SimpleFlatDatabase#onSave(java.lang.Object)
	 */
	@Override
	protected SerializedMap onSave(final ArenaPlayer data) {
		return data.serialize();
	}

	/**
	 * Connects to the database
	 *
	 * @param host
	 * @param port
	 * @param database
	 * @param user
	 * @param password
	 */
	public static void start(final String host, final int port, final String database, final String user, final String password) {
		instance.connect(host, port, database, user, password);
	}

	/**
	 * Saves data about the player to the database
	 *
	 * @param player
	 */
	public static void save(final Player player) {
		final ArenaPlayer cache = ArenaPlayer.getCache(player);

		instance.save(player.getName(), player.getUniqueId(), cache);
	}

	/**
	 * Loads data about the player to the database
	 * @param player
	 */
	public static void load(final Player player) {
		final ArenaPlayer cache = ArenaPlayer.getCache(player);

		instance.load(player.getUniqueId(), cache);
	}
}
