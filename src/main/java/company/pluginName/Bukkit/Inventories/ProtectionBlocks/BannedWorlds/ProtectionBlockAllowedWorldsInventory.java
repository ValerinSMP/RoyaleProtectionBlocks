package company.pluginName.Bukkit.Inventories.ProtectionBlocks.BannedWorlds;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.Shared.ConfirmationInventory;
import company.pluginName.Bukkit.Inventories.Shared.SearchWorldsInventory;
import company.pluginName.Modules.ProtectionBlocksPckg.Objects.ProtectionBlock;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;

@Inventory("protectionblocks_allowedworlds")
public class ProtectionBlockAllowedWorldsInventory extends PagedChestInventoryObject<String> {

	private ProtectionBlock protectionBlock;

	public ProtectionBlockAllowedWorldsInventory(Player player, ProtectionBlock protectionBlock) {
		super(player);

		this.protectionBlock = protectionBlock;

		setReplacements(new Replacement[] {
				new Replacement("{block}", () -> protectionBlock.getId() != null ? protectionBlock.getId() : "???") });
		setTitleReplacements(getReplacements());
	}

	@Override
	protected List<String> getEntityList() {
		return new ArrayList<>(protectionBlock.getBlockAllowedWorlds().get());
	}

	@Override
	protected ItemStack generateEntityItem(String entity) {
		return ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(), "Entity")
				.setReplacements(new Replacement("{world}", () -> entity)).build();
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, String entity) {
		new ConfirmationInventory(getPlayer(), () -> {
			protectionBlock.getBlockAllowedWorlds().remove(entity);
			updateEntityList();
		}).openInventory();
	}

	@ItemExecutor("Back-button")
	private void executeBackButton() {
		goToPreviousInventory();
	}

	@ItemGenerator("Back-button")
	private ItemStack generateBackButton(Item item) {
		return getPreviousInventory() != null ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemExecutor("Close-button")
	private void executeCloseButton() {
		closeInventory();
	}

	@ItemExecutor("Search-button")
	private void executeSearchButton() {
		new SearchWorldsInventory(getPlayer(), world -> {
			if (world != null) {
				protectionBlock.getBlockAllowedWorlds().add(world.getName());
				updateEntityList();
			}

			updateInventory();
		}).openInventory();
	}

}
