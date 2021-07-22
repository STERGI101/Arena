package org.mineacademy.arena.model;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.model.ChunkedTask;
import org.mineacademy.fo.region.Region;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.block.BaseBlock;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ArenaMapManager {

	/**
	 * Holds clipboards for saved regions (until we restore them or stop/reload/restart the server)
	 */
	private final StrictMap<String, Clipboard> savedClipboards = new StrictMap<>();

	/**
	 * Saves the arena region if it can be saved
	 *
	 * @param arena
	 */
	public void saveRegion(Arena arena) {
		final Region region = arena.getSettings().getRegion();

		if (region == null || !region.isWhole())
			return;

		final CuboidRegion cuboidRegion = new CuboidRegion(new BukkitWorld(region.getWorld()), toVector(region.getPrimary()), toVector(region.getSecondary()));
		final BlockArrayClipboard clipboard = new BlockArrayClipboard(cuboidRegion);

		try (EditSession editSession = createSession(cuboidRegion.getWorld())) {

			new ChunkedTask(1) {

				/**
				 * The copy operation we are using
				 */
				private Operation operation = new ForwardExtentCopy(editSession, cuboidRegion, clipboard, cuboidRegion.getMinimumPoint());

				/**
				 * Resume the operation means the operation moves forward
				 * until it can no longer be resumed
				 */
				@Override
				protected void onProcess(int index) {
					try {
						operation = operation.resume(new RunContext());

					} catch (final WorldEditException e) {
						e.printStackTrace();
					}
				}

				/**
				 * Return true if the operation can be resumed, ie not null
				 */
				@Override
				protected boolean canContinue(int index) {
					return operation != null;
				}

			}.startChain();
		}

		savedClipboards.put(arena.getName(), clipboard);
	}

	/**
	 * Restore arena region if it has been saved
	 *
	 * @param arena
	 */
	public void restoreRegion(Arena arena) {
		final Clipboard clipboard = savedClipboards.removeWeak(arena.getName());
		final Region region = arena.getSettings().getRegion();

		if (clipboard == null)
			return;

		try (EditSession editSession = createSession(new BukkitWorld(region.getWorld()))) {
			final List<BlockVector3> vectors = Common.convert(region.getBlocks(), (block) -> toVector(block));

			new ChunkedTask(50_000) {

				/**
				 * For each block in region find block stored in the clipboard,
				 * if it exists, restore it back
				 */
				@Override
				protected void onProcess(int index) {
					final BlockVector3 vector = vectors.get(index);
					final BaseBlock copy = clipboard.getFullBlock(vector);

					if (copy != null)
						try {
							editSession.setBlock(vector, copy);

						} catch (final MaxChangedBlocksException e) {
							e.printStackTrace();
						}
				}

				/**
				 * Flush the operation to make the blocks visible on finish
				 */
				@Override
				protected void onFinish() {
					editSession.flushSession();
				}

				/**
				 * Return if we can pull more blocks from our region or we are finished
				 *
				 * @return
				 */
				@Override
				protected boolean canContinue(int index) {
					return index < vectors.size();
				}

				/**
				 * Also show percentage how many blocks we have restored to finish
				 *
				 * @param initialTime
				 * @param processed
				 * @return
				 */
				@Override
				protected String getProcessMessage(long initialTime, int processed) {
					final long progress = Math.round(((double) getCurrentIndex() / (double) vectors.size()) * 100);

					return "[" + progress + "%] " + super.getProcessMessage(initialTime, processed);
				}

			}.startChain();
		}

	}

	/*
	 * Create a new edit session
	 */
	private EditSession createSession(World world) {
		final EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1);

		// Disable history control and bypass the //limit amount, cannot redo/undo the operation but faster
		session.setFastMode(true);

		return session;
	}

	/*
	 * Create a WorldEdit vector from the given block
	 */
	private BlockVector3 toVector(Block block) {
		return toVector(block.getLocation());
	}

	/*
	 * Create a WorldEdit vector from the given location
	 */
	private BlockVector3 toVector(Location location) {
		return BlockVector3.at(location.getX(), location.getY(), location.getZ());
	}
}
