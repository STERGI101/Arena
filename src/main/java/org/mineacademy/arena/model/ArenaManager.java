package org.mineacademy.arena.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.FileUtil;
import org.mineacademy.fo.ReflectionUtil;
import org.mineacademy.fo.ReflectionUtil.ReflectionException;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.FoException;
import org.mineacademy.fo.region.Region;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class ArenaManager {

	/**
	 * Stored classes that extend the {@link Arena} class to support multiple arena types
	 */
	private final StrictMap<String, Class<? extends Arena>> registeredTypes = new StrictMap<>();

	/**
	 * The list of loaded arenas
	 */
	private final List<Arena> loadedArenas = new ArrayList<>();

	/**
	 * Register a new valid arena type
	 * <p>
	 * Please put "public static final String TYPE" with the unique arena type
	 * in your class
	 *
	 * @param clazz
	 */
	public void registerArenaType(final Class<? extends Arena> clazz) {
		final String type;

		try {
			type = ReflectionUtil.getStaticFieldContent(clazz, "TYPE");

		} catch (final ReflectionException ex) {
			throw new FoException("Please put 'public static String TYPE' with the unique arena type to " + clazz);
		}

		registeredTypes.put(type, clazz);
	}

	/**
	 * Return true if the type of arena is registered
	 *
	 * @param type
	 * @return
	 */
	public boolean hasArenaType(final String type) {
		return registeredTypes.contains(type);
	}

	/**
	 * Remove all supported arene types
	 */
	public void clearRegisteredArenaTypes() {
		registeredTypes.clear();
	}

	/**
	 * Loads all arenas in the arenas/ file. Old arenas in our memory are trashed.
	 */
	public void loadArenas() {
		loadedArenas.clear();

		final File[] arenaFiles = FileUtil.getFiles("arenas", "yml");

		for (final File arenaFile : arenaFiles) {
			final String name = FileUtil.getFileName(arenaFile);
			final String type = detectArenaType(arenaFile);

			loadOrCreateArena(name, type);
		}
	}

	/*
	 * Pre-load the arena file to detect its type
	 */
	private String detectArenaType(final File file) {
		final FileConfiguration config = FileUtil.loadConfigurationStrict(file);
		final String type = config.getString("Type");

		Valid.checkNotNull(type, "Arena type not specified for " + file);
		return type;
	}

	/**
	 * Stops all currently played arenas
	 */
	public void stopArenas(final ArenaStopReason reason) {
		for (final Arena arena : getArenas())
			if (!arena.isStopped())
				arena.stopArena(reason);
	}

	/**
	 * Load or create an arena by its name
	 *
	 * @param name
	 * @param type
	 * @return the created arena
	 */
	public Arena loadOrCreateArena(final String name, @NonNull final String type) {
		final Class<? extends Arena> arenaClass = registeredTypes.get(type);
		Valid.checkNotNull(arenaClass, "Arena type " + type + " not supported. Available: " + registeredTypes.keySet());
		Valid.checkBoolean(!isArenaLoaded(name), "Arena " + name + " is already loaded: " + getArenaNames());

		try {
			final Arena arena = ReflectionUtil.instantiate(arenaClass, name);
			loadedArenas.add(arena);

			Common.log("[+] Loaded " + type + " arena " + arena.getName());
			return arena;

		} catch (final ReflectionException ex) {
			Common.throwError(ex, "Failed to create arena type " + type + ", ensure that " + arenaClass + " has 1 public constructor only taking the arena name");

		} catch (final Throwable t) {
			Common.throwError(t, "Failed to load arena " + name + " of type " + type);
		}

		return null;
	}

	/**
	 * Remove an arena by its name
	 *
	 * @param name
	 */
	public void removeArena(final String name) {
		final Arena arena = findArena(name);
		Valid.checkNotNull(arena, "Arena " + name + " is not loaded! Available arenas: " + getArenaNames());

		removeArena(arena);
	}

	/**
	 * Remove an existing arena
	 *
	 * @param arena
	 */
	public void removeArena(@NonNull final Arena arena) {
		if (!arena.isStopped())
			arena.stopArena(ArenaStopReason.PLUGIN);

		arena.getSettings().delete();
		loadedArenas.remove(arena);
	}

	/**
	 * Return true if the arena by the given name is already loaded.
	 *
	 * @param name
	 * @return
	 */
	public boolean isArenaLoaded(final String name) {
		return findArena(name) != null;
	}

	/**
	 * Get an arena by its name
	 *
	 * @param name
	 * @return
	 */
	public Arena findArena(@NonNull final String name) {
		for (final Arena arena : loadedArenas)
			if (arena.getName().equalsIgnoreCase(name))
				return arena;

		return null;
	}

	/**
	 * Get an arena at the given location
	 *
	 * @param location
	 */
	public Arena findArena(@NonNull final Location location) {
		for (final Arena arena : loadedArenas) {
			final Region region = arena.getSettings().getRegion();

			if (region != null && region.isWhole() && region.isWithin(location))
				return arena;
		}

		return null;
	}

	/**
	 * Return the arena where the given player is.
	 * <p>
	 * By the nature of our plugin it is only possible
	 * to be joined in a single arena in this arena manager at a time.
	 *
	 * @param player
	 * @return
	 */
	public Arena findArena(@NonNull final Player player) {
		final ArenaPlayer cache = ArenaPlayer.getCache(player);

		return cache.hasArena() ? cache.getArena() : null;
	}

	/**
	 * Get all loaded arenas of the given arena class
	 *
	 * @return
	 */
	public List<Arena> getArenasOfType(final Class<? extends Arena> type) {
		final List<Arena> arenas = new ArrayList<>();

		for (final Arena arena : loadedArenas)
			if (type.isAssignableFrom(arena.getClass()))
				arenas.add(arena);

		return arenas;
	}

	/**
	 * Get all loaded arenas
	 *
	 * @return
	 */
	public List<Arena> getArenas() {
		return Collections.unmodifiableList(loadedArenas);
	}

	/**
	 * Get all loaded arena names
	 *
	 * @return
	 */
	public List<String> getArenaNames() {
		return Common.convert(loadedArenas, Arena::getName);
	}

	/**
	 * Get all registered arena types
	 *
	 * @return
	 */
	public Set<String> getArenaTypes() {
		return Collections.unmodifiableSet(registeredTypes.keySet());
	}
}
