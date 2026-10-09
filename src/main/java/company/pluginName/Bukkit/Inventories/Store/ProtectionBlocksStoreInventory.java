package company.pluginName.Bukkit.Inventories.Store;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.Shared.ConfirmationInventory;
import company.pluginName.Modules.FilePckg.Messages;
import company.pluginName.Modules.FilePckg.Settings;
import company.pluginName.Modules.ProtectionBlocksPckg.ProtectionBlocksService;
import company.pluginName.Modules.ProtectionBlocksPckg.Objects.ProtectionBlock;
import company.pluginName.Utils.EconomyUtilities;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import darkpanda73.PandaUtils.Utilities.Java.String.StringUtilities;

@Inventory("protectionblocks_store")
public class ProtectionBlocksStoreInventory extends PagedChestInventoryObject<ProtectionBlock> {

	@AllArgsConstructor
	@Getter
	private static enum Filter {
		ALL((player, block) -> true),
		AFFORDABLE((player, block) -> EconomyUtilities.canAfford(player, block.getBlockInformation().getPrice()));

		private BiFunction<Player, ProtectionBlock, Boolean> filterFunction;

		public Filter previous() {
			return values()[(values().length + ordinal() - 1) % values().length];
		}

		public Filter next() {
			return values()[(values().length + ordinal() + 1) % values().length];
		}
	}

	@AllArgsConstructor
	@Getter
	private static enum Sort {
		PRICE((block1, block2) -> block1.getPrice().compareTo(block2.getPrice()));

		private Comparator<ProtectionBlock> sortFunction;

		public Sort previous() {
			return values()[(values().length + ordinal() - 1) % values().length];
		}

		public Sort next() {
			return values()[(values().length + ordinal() + 1) % values().length];
		}
	}

	@PandaInject
	private static ProtectionBlocksService protectionBlocksService;

	private Filter currentFilter = Filter.ALL;
	private Sort currentSort = Sort.PRICE;

	public ProtectionBlocksStoreInventory(Player player) {
		super(player);
	}

	@Override
	protected List<ProtectionBlock> getEntityList() {
		return protectionBlocksService.getProtectionBlocks().values().stream()
				.filter(block -> block.isForSale() && currentFilter.getFilterFunction().apply(getPlayer(), block))
				.sorted(currentSort.getSortFunction()).collect(Collectors.toList());
	}

	@Override
	protected ItemStack generateEntityItem(ProtectionBlock entity) {
		Replacement[] replacements = {
				new Replacement("{blocks_x}", () -> String.valueOf((entity.getBlocksX() * 2) + 1)),
				new Replacement("{blocks_y}",
						() -> entity.getBlocksY() == -1 ? Messages.MESSAGE_GENERAL_NOLIMIT.toString()
								: String.valueOf((entity.getBlocksY() * 2) + 1)),
				new Replacement("{blocks_z}", () -> String.valueOf((entity.getBlocksZ() * 2) + 1)),
				new Replacement("{block_id}", () -> entity.getId()),
				new Replacement("{block_permission}", () -> entity.getPermission()),
				new Replacement("{block_allowed_worlds}",
						() -> entity.getBlockAllowedWorlds().get().stream().collect(Collectors.joining(", "))),
				new Replacement("{block_price}",
						() -> entity.getPrice() != null ? StringUtilities.toCurrency(entity.getPrice()) : "---") };

		ItemBuilder itemBuilder = ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(), "Entity")
				.setLore(new ArrayList<>()).setReplacements(replacements);

		if (entity != null) {
			itemBuilder.fromItem(entity.getItem());
		} else {
			itemBuilder.setMaterial(Material.PLAYER_HEAD).setAmount(1).setSkin(
					"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjcwNWZkOTRhMGM0MzE5MjdmYjRlNjM5YjBmY2ZiNDk3MTdlNDEyMjg1YTAyYjQzOWUwMTEyZGEyMmIyZTJlYyJ9fX0=");
		}

		itemBuilder.getLore().addAll(getChestInventoryData().getEntityLore());

		return itemBuilder.apply(entity.getItem().clone());
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, ProtectionBlock entity) {
		if (Settings.SETTINGS_STORE_REQUESTCONFIRMATIONONPURCHASETHROUGHGUI.isTrue()) {
			new ConfirmationInventory(getPlayer(), () -> {
				entity.purchase(getPlayer());
			}).openInventory();
		} else {
			entity.purchase(getPlayer());
		}
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

	@ItemGenerator("All-filter-button")
	private ItemStack generateAllFilterItem(Item item) {
		return this.currentFilter == Filter.ALL ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Affordable-filter-button")
	private ItemStack generateAffordableFilterItem(Item item) {
		return this.currentFilter == Filter.AFFORDABLE ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemExecutor("All-filter-button")
	@ItemExecutor("Affordable-filter-button")
	private void onClickFilterButton() {
		this.currentFilter = this.currentFilter.next();
		updateEntityList();
		updateInventory();
	}

}
