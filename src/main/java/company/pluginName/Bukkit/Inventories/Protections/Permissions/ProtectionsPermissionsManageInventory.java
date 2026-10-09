package company.pluginName.Bukkit.Inventories.Protections.Permissions;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Modules.FilePckg.Messages;
import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemPregenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.ChestInventoryObject;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Utilities.Java.Arrays.ArrayUtilities;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.PermissionGroup;
import royale.RoyaleProtectionBlocks.Plugin.API.Objects.Permissions.PermissionInterface;

@Inventory("protections_permissions_manage")
public class ProtectionsPermissionsManageInventory extends ChestInventoryObject {

	private static final String TRUE_ITEM_PATH = "True-item";
	private static final String FALSE_PATH = "False-item";
	private static final String DISABLED_PATH = "Disabled-item";

	@PandaInject
	private static PlaceholdersService placeholdersService;

	private Protection protection;
	private PermissionInterface permission;

	public ProtectionsPermissionsManageInventory(Player player, Protection protection, PermissionInterface permission) {
		super(player);

		this.protection = protection;
		this.permission = permission;

		Replacement[] protectionReplacements = placeholdersService.getProtectionReplacements(protection);
		Replacement[] playerReplacements = placeholdersService.getPlayerReplacements(player);
		Replacement[] customReplacements = new Replacement[] {
				new Replacement("{permission_displayname}", () -> this.permission.getDisplayName()),
				new Replacement("{permission_nonmembersvalue}",
						() -> Boolean.TRUE
								.equals(this.protection.getPermissionValue(permission, PermissionGroup.NON_MEMBERS))
										? Messages.MESSAGE_GENERAL_TRUE.getContent()
										: Messages.MESSAGE_GENERAL_FALSE.getContent())
						.cacheText(false),
				new Replacement("{permission_membersvalue}",
						() -> Boolean.TRUE
								.equals(this.protection.getPermissionValue(permission, PermissionGroup.MEMBERS))
										? Messages.MESSAGE_GENERAL_TRUE.getContent()
										: Messages.MESSAGE_GENERAL_FALSE.getContent())
						.cacheText(false),
				new Replacement("{permission_ownersvalue}",
						() -> Boolean.TRUE
								.equals(this.protection.getPermissionValue(permission, PermissionGroup.OWNERS))
										? Messages.MESSAGE_GENERAL_TRUE.getContent()
										: Messages.MESSAGE_GENERAL_FALSE.getContent())
						.cacheText(false) };

		setReplacements(ArrayUtilities.join(
				new Replacement[playerReplacements.length + protectionReplacements.length + customReplacements.length],
				playerReplacements, protectionReplacements, customReplacements));
		setTitleReplacements(getReplacements());
	}

	@ItemPregenerator("Non-members-button")
	@ItemPregenerator("Members-button")
	@ItemPregenerator("Owners-button")
	private static void pregenerateToggleBlockButton(Item item) {
		item.getItems().put(TRUE_ITEM_PATH, ItemBuilder.inst().fromMap(item.getData(), TRUE_ITEM_PATH).build());
		item.getItems().put(FALSE_PATH, ItemBuilder.inst().fromMap(item.getData(), FALSE_PATH).build());
		item.getItems().put(DISABLED_PATH, ItemBuilder.inst().fromMap(item.getData(), DISABLED_PATH).build());
	}

	@ItemGenerator("Permission-item")
	private ItemStack generatePermissionItem() {
		return permission.getDisplayItem();
	}

	@ItemGenerator("Non-members-button")
	private ItemStack generateNonMembersButton(Item item) {
		return generateItem(item, PermissionGroup.NON_MEMBERS);
	}

	@ItemGenerator("Members-button")
	private ItemStack generateMembersButton(Item item) {
		return generateItem(item, PermissionGroup.MEMBERS);
	}

	@ItemGenerator("Owners-button")
	private ItemStack generateOwnersButton(Item item) {
		return generateItem(item, PermissionGroup.OWNERS);
	}

	@ItemExecutor("Non-members-button")
	private void clickNonMembersButton() {
		if (this.permission.isNonMembersValueEditable()) {
			this.protection.setPermissionValue(permission, PermissionGroup.NON_MEMBERS,
					Boolean.FALSE.equals(this.protection.getPermissionValue(permission, PermissionGroup.NON_MEMBERS)));
			updateInventory();
		}
	}

	@ItemExecutor("Members-button")
	private void clickMembersButton() {
		if (this.permission.isMembersValueEditable()) {
			this.protection.setPermissionValue(permission, PermissionGroup.MEMBERS,
					Boolean.FALSE.equals(this.protection.getPermissionValue(permission, PermissionGroup.MEMBERS)));
			updateInventory();
		}
	}

	@ItemExecutor("Owners-button")
	private void clickOwnersButton() {
		if (this.permission.isOwnersValueEditable()) {
			this.protection.setPermissionValue(permission, PermissionGroup.OWNERS,
					Boolean.FALSE.equals(this.protection.getPermissionValue(permission, PermissionGroup.OWNERS)));
			updateInventory();
		}
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

	private ItemStack generateItem(Item item, PermissionGroup permissionGroup) {
		boolean allowed = Boolean.TRUE.equals(this.protection.getPermissionValue(permission, permissionGroup));
		boolean editable = permissionGroup == PermissionGroup.NON_MEMBERS ? permission.isNonMembersValueEditable()
				: (permissionGroup == PermissionGroup.MEMBERS ? permission.isMembersValueEditable()
						: permission.isOwnersValueEditable());

		return ItemBuilder.inst()
				.fromItem(item.getItems().get(!editable ? DISABLED_PATH : (allowed ? TRUE_ITEM_PATH : FALSE_PATH)))
				.build();
	}

}
