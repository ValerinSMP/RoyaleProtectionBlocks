package company.pluginName.Bukkit.Inventories.ProtectionBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.Shared.ConfirmationInventory;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Modules.FilePckg.Messages;
import company.pluginName.Modules.PermissionsPckg.PermissionsService;
import company.pluginName.Modules.ProtectionBlocksPckg.ProtectionBlocksService;
import company.pluginName.Modules.ProtectionBlocksPckg.Objects.ProtectionBlock;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import darkpanda73.PandaUtils.Utilities.Java.String.StringUtilities;

@Inventory("protectionblocks_list")
public class ProtectionBlocksListInventory extends PagedChestInventoryObject<ProtectionBlock> {

	private static final String ENTITY_COPYLORELINE_PATH = "Entity.Copy-lore-line";
	private static final String ENTITY_EDITLORELINE_PATH = "Entity.Edit-lore-line";
	private static final String ENTITY_REMOVELORELINE_PATH = "Entity.Remove-lore-line";

	@PandaInject
	private static ProtectionBlocksService protectionBlocksService;

	private boolean canEdit;
	private boolean canCreate;
	private boolean canDelete;

	public ProtectionBlocksListInventory(Player player) {
		super(player);
	}

	@Override
	protected List<ProtectionBlock> getEntityList() {
		return new ArrayList<>(protectionBlocksService.getProtectionBlocks().values());
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
				new Replacement("{block_permission}",
						() -> entity.getPermission() != null ? entity.getPermission() : "&7&o---"),
				new Replacement("{block_allowed_worlds}",
						() -> entity.getBlockAllowedWorlds().get().size() > 0
								? entity.getBlockAllowedWorlds().get().stream().collect(Collectors.joining(", "))
								: "&7&oAll"),
				new Replacement("{block_price}",
						() -> entity.getPrice() != null ? StringUtilities.toCurrency(entity.getPrice()) : "&7&o---") };

		ItemBuilder itemBuilder = ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(), "Entity")
				.setReplacements(replacements);
		itemBuilder.getLore().clear();

		if (entity != null) {
			itemBuilder.fromItem(entity.getItem());
		} else {
			itemBuilder.setMaterial(Material.PLAYER_HEAD).setAmount(1).setSkin(
					"eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMjcwNWZkOTRhMGM0MzE5MjdmYjRlNjM5YjBmY2ZiNDk3MTdlNDEyMjg1YTAyYjQzOWUwMTEyZGEyMmIyZTJlYyJ9fX0=");
		}

		List<String> lore = itemBuilder.getLore().size() > 0 ? itemBuilder.getLore() : new ArrayList<>();
		lore.addAll(getChestInventoryData().getEntityLore());
		lore.add("&0");

		Object getCopyLore = getChestInventoryData().getCustomFields().getOrDefault(ENTITY_COPYLORELINE_PATH, "");
		if (getCopyLore != null && !getCopyLore.toString().isEmpty()) {
			lore.add(getCopyLore.toString());
		}

		if (canEdit) {
			Object editLore = getChestInventoryData().getCustomFields().getOrDefault(ENTITY_EDITLORELINE_PATH, "");
			if (editLore != null && !editLore.toString().isEmpty()) {
				lore.add(editLore.toString());
			}
		}

		if (canDelete) {
			Object removeLore = getChestInventoryData().getCustomFields().getOrDefault(ENTITY_REMOVELORELINE_PATH, "");
			if (removeLore != null && !removeLore.toString().isEmpty()) {
				lore.add(removeLore.toString());
			}
		}

		return itemBuilder.setLore(lore).apply(entity.getItem().clone());
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, ProtectionBlock entity) {
		if (e.getClick() == ClickType.LEFT && !e.isShiftClick()) {
			e.getWhoClicked().getOpenInventory().setCursor(entity.generateItem());
		} else if (e.getClick() == ClickType.SHIFT_LEFT && canEdit) {
			new ProtectionBlockManageInventory(getPlayer(), entity).openInventory();
		} else if (e.getClick() == ClickType.RIGHT && canDelete) {
			new ConfirmationInventory(getPlayer(), () -> {
				try {
					entity.delete(getPlayer());
					MessageTemplate.inst(Messages.MESSAGE_PROTECTIONS_BLOCKS_REMOVEDSUCCESSFULLY.applyPrefix())
							.process().sendMessage(getPlayer());
				} catch (RoyaleProtectionBlocksExceptionImpl e1) {
					e1.sendError(getPlayer());
				}
			}).openInventory();
		}
	}

	@Override
	protected void onPreUpdate() {
		super.onPreUpdate();

		canEdit = PermissionsService.BLOCKS_EDIT.hasPermission(getPlayer());
		canCreate = PermissionsService.BLOCKS_CREATE.hasPermission(getPlayer());
		canDelete = PermissionsService.BLOCKS_DELETE.hasPermission(getPlayer());
	}

	@ItemGenerator("Create-block-button")
	private ItemStack generateCreateBlockButton(Item item) {
		return canCreate ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemExecutor("Create-block-button")
	private void executeCreateBlockButton(Item item) {
		new ProtectionBlockManageInventory(getPlayer()).openInventory();

	}

	@ItemExecutor("Close-button")
	private void executeCloseButton() {
		closeInventory();
	}

}
