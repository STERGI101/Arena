package org.mineacademy.arena.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictSet;
import org.mineacademy.fo.model.ChunkedTask;
import org.mineacademy.fo.region.Region;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

/**
 * Enable us to reset arena world by
 * 1) Stopping the autosave when the arena starts
 * 2) Restoring back chunks to their previous state on arena end
 */
@UtilityClass
public final class ArenaWorldManager {

	/**
	 * A list of worlds that are being processes
	 */
	private final StrictSet<World> processedWorlds = new StrictSet<>();

	/**
	 * Disable autosave for the given world
	 *
	 * @param arena
	 */
	public void disableAutoSave(Arena arena) {
		checkApplicable(arena);

		final World world = arena.getSettings().getRegion().getWorld();
		world.setAutoSave(false);

		Common.log("Arena " + arena.getName() + " disabled world save for " + world.getName());
	}

	/**
	 * Attempts to restore all chunks in the world before autosave was disabled
	 *
	 * @param arena
	 */
	@SneakyThrows
	public void restoreWorld(Arena arena) {
		checkApplicable(arena);

		final Region region = arena.getSettings().getRegion();
		final World world = region.getWorld();
		final Location resetLocation = arena.getSettings().getResetLocation();

		for (final Player player : world.getPlayers())
			player.teleport(resetLocation);

		processedWorlds.add(world);

		final List<Block> blocks = region.getBlocks();

		new ChunkedTask(500_000) {

			final Set<Chunk> chunks = new HashSet<>();

			@Override
			protected void onProcess(int index) {
				final Block block = blocks.get(index);

				chunks.add(block.getChunk());
			}

			@Override
			protected boolean canContinue(int index) {
				return index < blocks.size();
			}

			@Override
			protected String getLabel() {
				return "blocks";
			}

			@Override
			protected void onFinish() {
				Common.log("Arena " + arena.getName() + " finished converting blocks to chunks.");

				new ChunkedTask(50) {

					final List<Chunk> chunksCopy = new ArrayList<>(chunks);

					@Override
					protected void onProcess(int index) {
						final Chunk chunk = chunksCopy.get(index);

						chunk.unload(false);
						chunk.load();
					}

					@Override
					protected boolean canContinue(int index) {
						return index < chunksCopy.size();
					}

					@Override
					protected String getLabel() {
						return "chunks";
					}

					@Override
					protected void onFinish() {
						Common.log("Arena " + arena.getName() + " finished resetting world " + world.getName() + ".");

						processedWorlds.remove(world);
					}
				}.startChain();
			}
		}.startChain();
	}

	/**
	 * Return if the given world is being processed right now
	 *
	 * @param world
	 * @return
	 */
	public boolean isWorldBeingProcessed(World world) {
		return processedWorlds.contains(world);
	}

	/*
	 * Check a few settings if we can proceed
	 */
	private void checkApplicable(Arena arena) {
		final ArenaSettings settings = arena.getSettings();

		Valid.checkBoolean(!isWorldBeingProcessed(settings.getRegion().getWorld()), "Arena " + arena.getName() + " world is already being processed!");
		Valid.checkBoolean(arena.getSettings().isWorldResetEnabled(), "Cannot use world restore, arena does not support it!");
		Valid.checkNotNull(settings.getResetLocation(), "Cannot use world restore, reset location is empty!");
	}
}
