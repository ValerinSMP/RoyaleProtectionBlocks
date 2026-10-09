package company.pluginName.Bukkit.Inventories.Admin;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.Admin.AdminProtectionsListInventory.Filters;
import company.pluginName.Bukkit.Inventories.Admin.AdminProtectionsListInventory.Filters.Direction;
import company.pluginName.Bukkit.Inventories.Admin.AdminProtectionsListInventory.Filters.SortedBy;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Services.PandaMessageListenerService;
import darkpanda73.PandaUtils.Utilities.Java.Objects.Pair;

@Inventory("admin_protections_list_filters_select_sort")
public class AdminProtectionsListFiltersSelectSortInventory extends PagedChestInventoryObject<SortedBy> {

	@PandaInject
	private static PandaMessageListenerService messageListenerService;

	private Filters filters;

	public AdminProtectionsListFiltersSelectSortInventory(Player player, Filters filters) {
		super(player);

		this.filters = filters;
	}

	@Override
	protected List<SortedBy> getEntityList() {
		return Arrays.asList(SortedBy.values());
	}

	@Override
	protected ItemStack generateEntityItem(SortedBy entity) {
		ItemBuilder builder;

		switch (entity) {
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

		return builder.build();
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, SortedBy entity) {
		this.filters.getSorts().add(Pair.of(entity, Direction.ASCENDANT));
		((AdminProtectionsListFiltersInventory) getPreviousInventory().getHolder()).updateEntityList();
		goToPreviousInventory();
	}

	@Override
	protected int getOffsetPerPage() {
		return 1;
	}

	@ItemExecutor("Back-button")
	private void onClickBackButton() {
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
