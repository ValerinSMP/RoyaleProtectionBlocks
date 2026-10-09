package company.pluginName.Bukkit.Inventories.Admin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.Admin.AdminProtectionsListInventory.Filters;
import company.pluginName.Bukkit.Inventories.Admin.AdminProtectionsListInventory.Filters.Direction;
import company.pluginName.Bukkit.Inventories.Admin.AdminProtectionsListInventory.Filters.SortedBy;
import company.pluginName.Bukkit.Inventories.Shared.SearchPlayerInventory;
import company.pluginName.Bukkit.Inventories.Shared.SearchProtectionBlockInventory;
import company.pluginName.Bukkit.Inventories.Shared.SearchWorldsInventory;
import company.pluginName.Modules.FilePckg.Messages;
import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import company.pluginName.Modules.ProtectionBlocksPckg.ProtectionBlocksService;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemPregenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.GeneratedItem;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Listeners.PandaMessageListener.Callback;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Services.PandaMessageListenerService;
import darkpanda73.PandaUtils.Utilities.Java.Objects.Pair;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Inventory("admin_protections_list_filters")
public class AdminProtectionsListFiltersInventory extends PagedChestInventoryObject<Pair<SortedBy, Direction>> {

	private static interface OptionSet {

		public void perform(OptionClickEvent event);

		@Data
		@AllArgsConstructor
		public static class OptionClickEvent {

			private AdminProtectionsListFiltersInventory inventory;
			private InventoryClickEvent event;
			private Pair<SortedBy, Direction> entity;
			private int entityIndex;

		}

	}

	@AllArgsConstructor
	@Getter
	private static enum CommonOptionSet implements OptionSet {
		PRIMARY_OPTION_SET((event) -> {
			event.entity.setSecond(event.getEntity().getSecond().next());
			event.inventory.updateInventory();
		}), SECONDARY_OPTION_SET((event) -> {
			event.inventory.filters.getSorts().remove(event.getEntityIndex());
			event.inventory.updateInventory();
		});

		private Consumer<OptionClickEvent> function;

		public void perform(OptionClickEvent event) {
			this.function.accept(event);
		};
	}

	@AllArgsConstructor
	@Getter
	private static enum ShiftOptionSet implements OptionSet {
		PRIMARY_OPTION_SET((event) -> {
			if (event.entityIndex != 0) {
				Pair<SortedBy, Direction> sort = event.inventory.filters.getSorts().remove(event.entityIndex);
				event.inventory.filters.getSorts().add(event.entityIndex - 1, sort);
				event.getInventory().updateInventory();
			}
		}), SECONDARY_OPTION_SET((event) -> {
			if (event.entityIndex < event.inventory.filters.getSorts().size() - 1) {
				Pair<SortedBy, Direction> sort = event.inventory.filters.getSorts().remove(event.entityIndex);
				event.inventory.filters.getSorts().add(event.entityIndex + 1, sort);
				event.getInventory().updateInventory();
			}
		});

		private Consumer<OptionClickEvent> function;

		public void perform(OptionClickEvent event) {
			this.function.accept(event);
		};
	}

	private static final String MESSAGES_RADIUSSPECIFYINFO_PATH = "Messages.Radius-specify-info";

	private static final String NOTSET_PATH = "Not-set-item";
	private static final String UNAVAILABLE_PATH = "Unavailable-item";

	private static final String ENTITY_LEFTCLICKLORELINE_PATH = "Entity.Left-click-lore-line";
	private static final String ENTITY_RIGHTCLICKLORELINE_PATH = "Entity.Right-click-lore-line";
	private static final String ENTITY_SHIFTLEFTCLICKLORELINE_PATH = "Entity.Shift-left-click-lore-line";
	private static final String ENTITY_SHIFTRIGHTCLICKLORELINE_PATH = "Entity.Shift-right-click-lore-line";
	private static final String ENTITY_ALTERDIRECTIONLORELINE_PATH = "Entity.Alter-direction-lore-line";
	private static final String ENTITY_REMOVEFILTERLORELINE_PATH = "Entity.Remove-filter-lore-line";
	private static final String ENTITY_MOVEUPLORELINE_PATH = "Entity.Move-up-lore-line";
	private static final String ENTITY_MOVEDOWNLORELINE_PATH = "Entity.Move-down-lore-line";

	@PandaInject
	private static PlaceholdersService placeholdersService;

	@PandaInject
	private static ProtectionBlocksService protectionBlocksService;

	@PandaInject
	private static PandaMessageListenerService messageListenerService;

	private Filters filters;

	private OptionSet leftClickOptionSet = CommonOptionSet.PRIMARY_OPTION_SET;
	private OptionSet rightClickOptionSet = CommonOptionSet.SECONDARY_OPTION_SET;
	private OptionSet shiftLeftClickOptionSet = ShiftOptionSet.PRIMARY_OPTION_SET;
	private OptionSet shiftRightClickOptionSet = ShiftOptionSet.SECONDARY_OPTION_SET;

	public AdminProtectionsListFiltersInventory(Player player, Filters filters) {
		super(player);

		this.filters = filters;
	}

	@Override
	protected List<Pair<SortedBy, Direction>> getEntityList() {
		return filters.getSorts();
	}

	@Override
	protected ItemStack generateEntityItem(Pair<SortedBy, Direction> entity, int index) {
		ItemBuilder builder;

		switch (entity.getFirst()) {
		case CREATED_DATE:
			builder = ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(),
					"Entity.Created-date-sort-item");
			break;
		case DISTANCE:
			builder = ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(),
					"Entity.Distance-sort-item");
			break;
		case NAME:
			builder = ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(), "Entity.Name-sort-item");
			break;
		case OWNER:
			builder = ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(), "Entity.Owner-sort-item");
			break;
		case WORLD:
			builder = ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(), "Entity.World-sort-item");
			break;
		case PROTECTION_BLOCK:
			builder = ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(),
					"Entity.Protection-block-sort-item");
			break;
		default:
			builder = ItemBuilder.inst();
			break;
		}

		List<Replacement> replacements = new ArrayList<>();
		replacements.add(new Replacement("{sort_direction}", () -> entity.getSecond().name()));

		List<String> extraLore = new ArrayList<>();

		if ((this.leftClickOptionSet == CommonOptionSet.PRIMARY_OPTION_SET)
				|| this.leftClickOptionSet == ShiftOptionSet.PRIMARY_OPTION_SET && index > 0) {
			String leftClickAction = this.leftClickOptionSet == CommonOptionSet.PRIMARY_OPTION_SET
					? getChestInventoryData().getCustomFields().get(ENTITY_ALTERDIRECTIONLORELINE_PATH).toString()
					: getChestInventoryData().getCustomFields().get(ENTITY_MOVEUPLORELINE_PATH).toString();

			extraLore.add(getChestInventoryData().getCustomFields().get(ENTITY_LEFTCLICKLORELINE_PATH).toString());
			replacements.add(new Replacement("{left_click_action}", () -> leftClickAction));
		}

		if ((this.rightClickOptionSet == CommonOptionSet.SECONDARY_OPTION_SET && this.filters.getSorts().size() > 1)
				|| (this.rightClickOptionSet == ShiftOptionSet.SECONDARY_OPTION_SET
						&& index < this.filters.getSorts().size() - 1)) {
			String rightClickAction = this.rightClickOptionSet == CommonOptionSet.SECONDARY_OPTION_SET
					? getChestInventoryData().getCustomFields().get(ENTITY_REMOVEFILTERLORELINE_PATH).toString()
					: getChestInventoryData().getCustomFields().get(ENTITY_MOVEDOWNLORELINE_PATH).toString();
			extraLore.add(getChestInventoryData().getCustomFields().get(ENTITY_RIGHTCLICKLORELINE_PATH).toString());
			replacements.add(new Replacement("{right_click_action}", () -> rightClickAction));
		}

		if ((this.shiftLeftClickOptionSet == CommonOptionSet.PRIMARY_OPTION_SET)
				|| this.shiftLeftClickOptionSet == ShiftOptionSet.PRIMARY_OPTION_SET && index > 0) {
			String shiftLeftClickAction = this.shiftLeftClickOptionSet == CommonOptionSet.PRIMARY_OPTION_SET
					? getChestInventoryData().getCustomFields().get(ENTITY_ALTERDIRECTIONLORELINE_PATH).toString()
					: getChestInventoryData().getCustomFields().get(ENTITY_MOVEUPLORELINE_PATH).toString();

			extraLore.add(getChestInventoryData().getCustomFields().get(ENTITY_SHIFTLEFTCLICKLORELINE_PATH).toString());
			replacements.add(new Replacement("{shift_left_click_action}", () -> shiftLeftClickAction));
		}

		if ((this.shiftRightClickOptionSet == CommonOptionSet.SECONDARY_OPTION_SET
				&& this.filters.getSorts().size() > 1)
				|| (this.shiftRightClickOptionSet == ShiftOptionSet.SECONDARY_OPTION_SET
						&& index < this.filters.getSorts().size() - 1)) {
			String shiftRightClickAction = this.shiftRightClickOptionSet == CommonOptionSet.SECONDARY_OPTION_SET
					? getChestInventoryData().getCustomFields().get(ENTITY_REMOVEFILTERLORELINE_PATH).toString()
					: getChestInventoryData().getCustomFields().get(ENTITY_MOVEDOWNLORELINE_PATH).toString();
			extraLore
					.add(getChestInventoryData().getCustomFields().get(ENTITY_SHIFTRIGHTCLICKLORELINE_PATH).toString());
			replacements.add(new Replacement("{shift_right_click_action}", () -> shiftRightClickAction));
		}

		extraLore.add(0, "&0");
		builder.getLore().addAll(extraLore);

		return builder.setReplacements(replacements.toArray(new Replacement[replacements.size()])).build();
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, Pair<SortedBy, Direction> entity, int index) {
		if (e.getClick() == ClickType.LEFT) {
			this.leftClickOptionSet.perform(new OptionSet.OptionClickEvent(this, e, entity, index));
		} else if (e.getClick() == ClickType.RIGHT) {
			this.rightClickOptionSet.perform(new OptionSet.OptionClickEvent(this, e, entity, index));
		} else if (e.getClick() == ClickType.SHIFT_LEFT) {
			this.shiftLeftClickOptionSet.perform(new OptionSet.OptionClickEvent(this, e, entity, index));
		} else if (e.getClick() == ClickType.SHIFT_RIGHT) {
			this.shiftRightClickOptionSet.perform(new OptionSet.OptionClickEvent(this, e, entity, index));
		}
	}

	@Override
	protected int getOffsetPerPage() {
		return 1;
	}

	@ItemPregenerator("Player-filter-button")
	@ItemPregenerator("World-filter-button")
	@ItemPregenerator("Protection-block-filter-button")
	private static void onPregenerateNotSetButton(Item item) {
		item.getItems().put(Item.DISPLAYITEM_KEY,
				ItemBuilder.inst().fromMap(item.getData(), Item.DISPLAYITEM_KEY).build());
		item.getItems().put(NOTSET_PATH, ItemBuilder.inst().fromMap(item.getData(), NOTSET_PATH).build());
	}

	@ItemPregenerator("Radius-filter-button")
	private static void onPregenerateUnavailableButton(Item item) {
		item.getItems().put(Item.DISPLAYITEM_KEY,
				ItemBuilder.inst().fromMap(item.getData(), Item.DISPLAYITEM_KEY).build());
		item.getItems().put(NOTSET_PATH, ItemBuilder.inst().fromMap(item.getData(), NOTSET_PATH).build());
		item.getItems().put(UNAVAILABLE_PATH, ItemBuilder.inst().fromMap(item.getData(), UNAVAILABLE_PATH).build());
	}

	@ItemGenerator("Player-filter-button")
	private ItemStack onGeneratePlayerFilterButton(Item item) {
		ItemBuilder builder = ItemBuilder.inst()
				.fromItem(this.filters.getFilteredPlayer() != null ? item.getItems().get(Item.DISPLAYITEM_KEY)
						: item.getItems().get(NOTSET_PATH))
				.setReplacements(new Replacement("{player_name}",
						() -> this.filters.getFilteredPlayer() != null ? this.filters.getFilteredPlayer().getName()
								: "???"));

		return this.filters.getFilteredPlayer() != null
				? processPlayerHead(builder, this.filters.getFilteredPlayer().toBukkit())
				: builder.build();
	}

	@ItemGenerator("World-filter-button")
	private ItemStack onGenerateWorldFilterButton(Item item) {
		return ItemBuilder.inst()
				.fromItem(this.filters.getFilteredWorld() != null ? item.getItems().get(Item.DISPLAYITEM_KEY)
						: item.getItems().get(NOTSET_PATH))
				.setReplacements(new Replacement("{world_name}",
						() -> this.filters.getFilteredWorld() != null ? this.filters.getFilteredWorld().getName()
								: "???"))
				.build();
	}

	@ItemGenerator("Radius-filter-button")
	private ItemStack onGenerateRadiusFilterButton(Item item) {
		if (this.filters.getFilteredWorld() != null
				&& !this.filters.getFilteredWorld().equals(this.getPlayer().getWorld())) {
			return ItemBuilder.inst().fromItem(item.getItems().get(UNAVAILABLE_PATH)).build();
		}

		return ItemBuilder.inst()
				.fromItem(this.filters.getFilteredRadius() > 0 ? item.getItems().get(Item.DISPLAYITEM_KEY)
						: item.getItems().get(NOTSET_PATH))
				.setReplacements(new Replacement("{radius}", () -> String.valueOf(this.filters.getFilteredRadius())))
				.build();
	}

	@ItemGenerator("Protection-block-filter-button")
	private ItemStack onGenerateProtectionBlockFilterButton(Item item) {
		if (this.filters.getFilteredProtectionBlock() != null) {
			return ItemBuilder.inst().fromItem(this.filters.getFilteredProtectionBlock().getItem())
					.fromMap(item.getData(), Item.DISPLAYITEM_KEY)
					.setReplacements(placeholdersService
							.getProtectionBlockReplacements(this.filters.getFilteredProtectionBlock()))
					.setApplyNullValues(true).build();
		} else {
			return item.getItems().get(NOTSET_PATH);
		}
	}

	@ItemExecutor("Player-filter-button")
	private void onClickPlayerFilterButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		if (e.getClick() == ClickType.LEFT) {
			new SearchPlayerInventory(getPlayer(), (player) -> {
				this.filters.setFilteredPlayer(player);
				updateInventory();
			}).openInventory();
		} else if (e.getClick() == ClickType.RIGHT) {
			this.filters.setFilteredPlayer(null);
			updateInventory();
		}
	}

	@ItemExecutor("World-filter-button")
	private void onClickWorldFilterButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		if (e.getClick() == ClickType.LEFT) {
			new SearchWorldsInventory(getPlayer(), (world) -> {
				this.filters.setFilteredWorld(world);
				updateInventory();

				if (world != null && !world.equals(this.getPlayer().getWorld())) {
					this.filters.setFilteredRadius(0);
				}
			}).openInventory();
		} else if (e.getClick() == ClickType.RIGHT) {
			this.filters.setFilteredWorld(null);
			updateInventory();
		}
	}

	@ItemExecutor("Radius-filter-button")
	private void onClickRadiusFilterButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		if (this.filters.getFilteredWorld() == null
				|| this.filters.getFilteredWorld().equals(this.getPlayer().getWorld())) {
			if (e.getClick() == ClickType.LEFT) {
				messageListenerService.getListener().replaceListening(getPlayer().getUniqueId(), new Callback() {

					@Override
					public boolean message(String message) {
						try {
							int radius = Integer.parseInt(message);

							filters.setFilteredRadius(radius);
						} catch (NumberFormatException e1) {
							MessageTemplate.inst(Messages.ERROR_INVALIDNUMBER.applyPrefix()).process()
									.sendMessage(e.getWhoClicked());
						}
						return true;
					}

					public void cancel() {
						openInventory();
					}

				});
				closeInventory();
				MessageTemplate
						.inst(PandaPrefixedStringField.applyPrefix(getChestInventoryData().getCustomFields()
								.get(MESSAGES_RADIUSSPECIFYINFO_PATH).toString()))
						.process().sendMessage(e.getWhoClicked());
			} else if (e.getClick() == ClickType.RIGHT) {
				this.filters.setFilteredRadius(0);
				updateInventory();
			}
		}
	}

	@ItemExecutor("Protection-block-filter-button")
	private void onClickProtectionBlockFilterButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		if (e.getClick() == ClickType.LEFT) {
			new SearchProtectionBlockInventory(getPlayer(),
					new ArrayList<>(protectionBlocksService.getProtectionBlocks().values()), (block) -> {
						this.filters.setFilteredProtectionBlock(block);
						updateInventory();
					}).openInventory();
		} else if (e.getClick() == ClickType.RIGHT) {
			this.filters.setFilteredProtectionBlock(null);
			updateInventory();
		}
	}

	@ItemExecutor("Add-sort-button")
	private void onClickAddSortButton() {
		new AdminProtectionsListFiltersSelectSortInventory(getPlayer(), filters).openInventory();
	}

	@ItemExecutor("Back-button")
	private void onClickBackButton() {
		((AdminProtectionsListInventory) getPreviousInventory().getHolder()).updateEntityList();
		goToPreviousInventory();
	}

	@ItemGenerator("Back-button")
	private ItemStack generateBackButton(Item item) {
		return getPreviousInventory() != null ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemExecutor("Close-button")
	private void onClickCloseButton() {
		closeInventory();
	}

}
