package company.pluginName.Bukkit.Inventories.Protections.Settings;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.Protections.Flags.ProtectionFlagsInventory;
import company.pluginName.Bukkit.Inventories.Protections.Permissions.ProtectionsPermissionsListInventory;
import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import company.pluginName.Modules.ProtectionSettingsPckg.ProtectionSettingsService;
import company.pluginName.Modules.ProtectionSettingsPckg.Objects.Settings.GreetingFarewellMessagesSetting;
import company.pluginName.Modules.ProtectionSettingsPckg.Objects.Settings.Templates.BooleanSettingImpl;
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
import royale.RoyaleProtectionBlocks.Plugin.API.RoyaleProtectionBlocksAPI;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.PermissionGroup;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Objects.Settings.SettingInterface;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionSwitchSettingRequestInput;

@Inventory("protections_settings_list")
public class ProtectionsSettingsListInventory extends PagedChestInventoryObject<SettingInterface<?>> {

	@PandaInject
	private static ProtectionsServiceImpl protectionsService;

	@PandaInject
	private static ProtectionSettingsService protectionSettingsService;

	@PandaInject
	private static PlaceholdersService placeholdersService;

	private Protection protection;

	public ProtectionsSettingsListInventory(Player player, Protection protection) {
		super(player);

		this.protection = protection;

		Replacement[] protectionReplacements = placeholdersService.getProtectionReplacements(protection);
		Replacement[] playerReplacements = placeholdersService.getPlayerReplacements(player);
		setReplacements(ArrayUtilities.join(new Replacement[playerReplacements.length + protectionReplacements.length],
				playerReplacements, protectionReplacements));
		setTitleReplacements(getReplacements());
	}

	@Override
	protected List<SettingInterface<?>> getEntityList() {
		return protectionSettingsService.getSettings().stream()
				.filter(setting -> setting.isEnabled() && setting.isEditable()
						&& (setting.getPermission() == null || getPlayer().hasPermission(setting.getPermission())))
				.collect(Collectors.toList());

	}

	@Override
	protected ItemStack generateEntityItem(SettingInterface<?> setting) {
		return ItemBuilder.inst().fromItem(setting.getDisplayItem())
				.setReplacements(
						new Replacement("{value}",
								() -> protection.getSettingValueAsStringSafely(setting, PermissionGroup.GENERIC)),
						new Replacement("{non_members_value}",
								() -> protection.getSettingValueAsStringSafely(setting, PermissionGroup.NON_MEMBERS)),
						new Replacement("{members_value}",
								() -> protection.getSettingValueAsStringSafely(setting, PermissionGroup.MEMBERS)),
						new Replacement("{owners_value}",
								() -> protection.getSettingValueAsStringSafely(setting, PermissionGroup.OWNERS)))
				.build();
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, SettingInterface<?> entity) {
		if (ProtectionUtilities.canSwitchSettings(protection, getPlayer())) {
			if (entity instanceof GreetingFarewellMessagesSetting) {
				new ProtectionSettingsGreetingFarewellMessagesInventory(getPlayer(), this.protection).openInventory();
			} else if (entity instanceof BooleanSettingImpl) {
				try {
					RoyaleProtectionBlocksAPI.getInstance().getPlayerInteractionsService()
							.protectionSwitchSettingRequest(ProtectionSwitchSettingRequestInput.inst(getPlayer(),
									protection, (BooleanSettingImpl) entity, PermissionGroup.GENERIC,
									Boolean.FALSE.equals(this.protection.getSettingValue((BooleanSettingImpl) entity,
											PermissionGroup.GENERIC))));

					updateInventory();
				} catch (RoyaleProtectionBlocksException e1) {
					e1.sendError(getPlayer());
				}
			}
		}
	}

	@ItemGenerator("Protection-info")
	private ItemStack generateProtectionInfo(Item item) {
		return ItemBuilder.inst().fromMap(item.getData(), Item.DISPLAYITEM_KEY)
				.apply(protection.getDisplayItemOrDefault().clone());
	}

	@ItemExecutor("Close-button")
	private void executeCloseButton() {
		closeInventory();
	}

	@ItemGenerator("Back-button")
	private ItemStack generateBackButton(Item item) {
		return getPreviousInventory() != null ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemExecutor("Back-button")
	private void executeBackButton() {
		goToPreviousInventory();
	}

	@ItemExecutor("Permissions-tab")
	private void executePermissionsTab() {
		new ProtectionsPermissionsListInventory(getPlayer(), protection).openInventory();
	}

	@ItemExecutor("Environment-tab")
	private void executeEnvironmentTab() {
		new ProtectionFlagsInventory(getPlayer(), protection).openInventory();
	}

}
