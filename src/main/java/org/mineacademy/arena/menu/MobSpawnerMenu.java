package org.mineacademy.arena.menu;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.mineacademy.arena.model.monster.MobArenaSettings.MobSpawnpoint;
import org.mineacademy.fo.ItemUtil;
import org.mineacademy.fo.MathUtil;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.conversation.SimplePrompt;
import org.mineacademy.fo.menu.Menu;
import org.mineacademy.fo.menu.MenuPagged;
import org.mineacademy.fo.menu.button.Button;
import org.mineacademy.fo.menu.button.ButtonConversation;
import org.mineacademy.fo.menu.button.ButtonMenu;
import org.mineacademy.fo.menu.model.ItemCreator;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.CompSound;

import lombok.NonNull;

/**
 * The monster spawner menu to configure a mob spawner
 */
public class MobSpawnerMenu extends Menu {

	private final Button monstersButton;
	private final Button intensityButton;

	public MobSpawnerMenu(final MobSpawnpoint point) {
		setSize(9 * 3);
		setTitle("Mob spawner configuration");

		this.monstersButton = new ButtonMenu(new MonsterSelectMenu(this, point), ItemCreator.of(CompMaterial.SPAWNER,
				"Select monster",
				"",
				"Current: " + ItemUtil.bountifyCapitalized(point.getEntity()),
				"",
				"Click to select what",
				"monster to spawn."));

		this.intensityButton = new ButtonConversation(new MultiplierPrompt(point), ItemCreator.of(CompMaterial.REDSTONE,
				"Wave multiplier",
				"",
				"Current: " + MathUtil.formatTwoDigits(point.getMultiplier()),
				"",
				"Click to select the",
				"spawn amount multiplier",
				"in each wave."));
	}

	/**
	 * @see org.mineacademy.fo.menu.Menu#getItemAt(int)
	 */
	@Override
	public ItemStack getItemAt(final int slot) {

		if (slot == 9 * 1 + 3)
			return monstersButton.getItem();

		if (slot == 9 * 1 + 5)
			return intensityButton.getItem();

		return null;
	}

	@Override
	protected String[] getInfo() {
		return new String[] {
				"Configure your",
				"mob spawner here"
		};
	}

	/**
	 * Menu to select what monster a spawnpoint should have
	 */
	private final static class MonsterSelectMenu extends MenuPagged<EntityType> {

		/**
		 * The spawn point to select monsters to
		 */
		private final MobSpawnpoint point;

		private MonsterSelectMenu(final Menu parent, final MobSpawnpoint point) {
			super(parent, compileValidTypes());

			this.point = point;
		}

		/**
		 * Return a list of valid entities that can be spawned
		 *
		 * @return
		 */
		private static List<EntityType> compileValidTypes() {
			final List<EntityType> list = new ArrayList<>();

			for (final EntityType type : EntityType.values())
				if (type.isAlive() && type.isSpawnable())
					list.add(type);

			return list;
		}

		/**
		 * @see org.mineacademy.fo.menu.MenuPagged#convertToItemStack(java.lang.Object)
		 */
		@Override
		protected ItemStack convertToItemStack(final EntityType item) {
			return ItemCreator.of(
					CompMaterial.makeMonsterEgg(item),
					ItemUtil.bountifyCapitalized(item))
					.glow(point.getEntity() == item)
					.build()
					.makeMenuTool();
		}

		/**
		 * @see org.mineacademy.fo.menu.MenuPagged#onPageClick(org.bukkit.entity.Player, java.lang.Object, org.bukkit.event.inventory.ClickType)
		 */
		@Override
		protected void onPageClick(final Player player, final EntityType item, final ClickType click) {
			point.setEntity(item);

			restartMenu("&2Selected " + ItemUtil.bountifyCapitalized(item) + "!");
		}

		/**
		 * @see org.mineacademy.fo.menu.Menu#getInfo()
		 */
		@Override
		protected String[] getInfo() {
			return new String[] {
					"Select what entity",
					"this spawner spawns."
			};
		}
	}

	/**
	 * The prompt configuring how many times we should multiple the monster amount
	 * spawned from a single spawner by the wave number.
	 *
	 * = {multiplier} * {wave} = X monsters spawned
	 */
	private final class MultiplierPrompt extends SimplePrompt {

		/**
		 * The spawner to configure
		 */
		private final MobSpawnpoint point;

		private MultiplierPrompt(final MobSpawnpoint point) {
			super(false);

			this.point = point;
		}

		@Override
		protected String getPrompt(final ConversationContext ctx) {
			return "&6Write how many times X we should multiply the spawnpoint spawn amount on each wave. Current value: " + point.getMultiplier();
		}

		@Override
		protected boolean isInputValid(final ConversationContext context, final String input) {
			if (!Valid.isDecimal(input) && !Valid.isInteger(input))
				return false;

			final double level = Double.parseDouble(input);

			return level > 0 && level < 9009;
		}

		@Override
		protected String getFailedValidationText(final ConversationContext context, final String invalidInput) {
			return "Only specify a non-zero whole number such as 1 or 1.5.";
		}

		@Override
		protected @Nullable Prompt acceptValidatedInput(@NonNull final ConversationContext context, @NonNull final String input) {
			final double multiplier = Double.parseDouble(input);
			final Player player = getPlayer(context);

			point.setMultiplier(multiplier);
			CompSound.LEVEL_UP.play(player);

			tell(context, "&6Set the amount multiplier to " + multiplier + ".");
			return Prompt.END_OF_CONVERSATION;
		}
	}
}
