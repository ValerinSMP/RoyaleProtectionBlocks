package company.pluginName.Bukkit.Inventories.Protections.Merge;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.v2.Protections.ProtectionsManageInventory;
import company.pluginName.Exceptions.Exceptions;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Modules.FilePckg.Settings;
import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import company.pluginName.Modules.ProtectionsPckg.ProtectionsServiceImpl;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionUtilities;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import darkpanda73.PandaUtils.Utilities.Java.Arrays.ArrayUtilities;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.PlayerInteractionsService;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionMergeRequestInput;

@Inventory("protections_merge")
public class ProtectionMergeInventory extends PagedChestInventoryObject<Protection> {

	@PandaInject
	private static ProtectionsServiceImpl protectionsService;

	@PandaInject
	private static PlaceholdersService placeholdersService;

	@PandaInject
	private static PlayerInteractionsService playerInteractionsService;

	private Protection protection;

	public ProtectionMergeInventory(Player player, Protection protection) throws RoyaleProtectionBlocksExceptionImpl {
		super(player);

		if (Settings.SETTINGS_PROTECTION_MERGE_ENABLED.isFalse()) {
			throw Exceptions.Protections.Merge.DISABLED.generateException();
		}

		this.protection = protection;

		Replacement[] playerReplacements = placeholdersService.getPlayerReplacements(getPlayer());
		Replacement[] protectionReplacements = placeholdersService.getProtectionReplacements(protection);
		setReplacements(ArrayUtilities.join(new Replacement[playerReplacements.length + protectionReplacements.length],
				playerReplacements, protectionReplacements));
		setTitleReplacements(getReplacements());
	}

	@Override
	protected List<Protection> getEntityList() {
		return protectionsService.findAllowedParentProtectionsByPlayer(getPlayer())
				.filter(protection -> protection != this.protection
						&& protection.getOwnerUuid().equals(this.protection.getOwnerUuid())
						&& ProtectionUtilities.canMerge(protection, getPlayer())
						&& !this.protection.getChildProtectionsRecursively().contains(protection)
						&& (Protection.SETTINGS_PROTECTION_MERGE_MUSTBECLOSEFORMERGE.isFalse()
								|| protection.isInside(this.protection.getProtectionArea(), true)))
				.collect(Collectors.toList());
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

	@Override
	protected ItemStack generateEntityItem(Protection entity) {
		Replacement[] protectionReplacements = placeholdersService.getProtectionReplacements(entity);

		ItemBuilder itemBuilder = ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(), "Entity")
				.setReplacements(protectionReplacements);

		return itemBuilder.apply(entity.getDisplayItemOrDefault().clone());
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, Protection entity) {
		try {
			playerInteractionsService
					.protectionMergeRequest(ProtectionMergeRequestInput.inst(getPlayer(), protection, entity));
			new ProtectionsManageInventory(getPlayer(), entity).setPreviousInventory(null).openInventory();
		} catch (RoyaleProtectionBlocksException e1) {
			e1.sendError(getPlayer());
		}
	}

}
