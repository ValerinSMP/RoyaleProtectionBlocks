package company.pluginName.Bukkit.Inventories.Shared;

import java.util.List;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.ProtectionBlocks.IProtectionBlock;

@Inventory("searchprotectionblock")
public class SearchProtectionBlockInventory extends PagedChestInventoryObject<IProtectionBlock> {

	@PandaInject
	private static PlaceholdersService placeholdersService;

	private List<? extends IProtectionBlock> protectionBlocks;
	private Consumer<IProtectionBlock> action;

	public SearchProtectionBlockInventory(Player player, List<? extends IProtectionBlock> protectionBlocks,
			Consumer<IProtectionBlock> action) {
		super(player);

		this.protectionBlocks = protectionBlocks;
		this.action = action;
	}

	@Override
	protected List<? extends IProtectionBlock> getEntityList() {
		return this.protectionBlocks;
	}

	@Override
	protected ItemStack generateEntityItem(IProtectionBlock protectionBlock) {
		return ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(), "Entity")
				.setReplacements(placeholdersService.getProtectionBlockReplacements(protectionBlock))
				.apply(protectionBlock.getItem().clone());
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, IProtectionBlock entity) {
		closeInventory();
		action.accept(entity);
	}

	@ItemExecutor("Back-button")
	private void onClickBack() {
		goToPreviousInventory();
	}

	@ItemGenerator("Back-button")
	private ItemStack generateBackButton(Item item) {
		return getPreviousInventory() != null ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemExecutor("Close-button")
	private void onClickClose() {
		closeInventory();
	}

}
