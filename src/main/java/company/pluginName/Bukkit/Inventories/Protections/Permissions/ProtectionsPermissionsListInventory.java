package company.pluginName.Bukkit.Inventories.Protections.Permissions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.sk89q.worldguard.protection.flags.StateFlag.State;

import company.pluginName.Bukkit.Inventories.Protections.Settings.ProtectionSettingsGreetingFarewellMessagesInventory;
import company.pluginName.Bukkit.Inventories.Protections.Settings.ProtectionsSettingsListInventory;
import company.pluginName.Exceptions.Exceptions;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import company.pluginName.Modules.ProtectionFlagsPckg.ProtectionFlagsService;
import company.pluginName.Modules.ProtectionFlagsPckg.Objects.ProtectionFlag;
import company.pluginName.Modules.ProtectionFlagsPckg.Utils.ProtectionFlagUtilities;
import company.pluginName.Modules.ProtectionPermissionsPckg.ProtectionPermissionsService;
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
import royale.RoyaleProtectionBlocks.Plugin.API.Objects.Permissions.PermissionInterface;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.PermissionGroup;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtectionFlags.FlagModificationRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Objects.Settings.SettingInterface;
import royale.RoyaleProtectionBlocks.Plugin.API.RoyaleProtectionBlocksAPI;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionSwitchSettingRequestInput;

@Inventory("protections_permissions_list")
public class ProtectionsPermissionsListInventory extends PagedChestInventoryObject<Object> {
	private static final List<String> DUPLICATE_FLAGS = Arrays.asList("tnt", "chest-access", "pvp");

	@PandaInject
	private static ProtectionsServiceImpl protectionsService;

	@PandaInject
	private static ProtectionPermissionsService protectionPermissionsService;

	@PandaInject
	private static ProtectionFlagsService protectionFlagsService;

	@PandaInject
	private static ProtectionSettingsService protectionSettingsService;

	@PandaInject
	private static PlaceholdersService placeholdersService;

	private Protection protection;

	public ProtectionsPermissionsListInventory(Player player, Protection protection) {
		super(player);

		this.protection = protection;

		Replacement[] protectionReplacements = placeholdersService.getProtectionReplacements(protection);
		Replacement[] playerReplacements = placeholdersService.getPlayerReplacements(player);
		setReplacements(ArrayUtilities.join(new Replacement[playerReplacements.length + protectionReplacements.length],
				playerReplacements, protectionReplacements));
		setTitleReplacements(getReplacements());
	}

	@Override
	protected List<Object> getEntityList() {
		List<Object> entries = new ArrayList<>();
		entries.addAll(protectionPermissionsService.getPermissions().stream()
				.filter(perm -> perm.isEnabled() && perm.isEditable()).collect(Collectors.toList()));
		entries.addAll(protectionFlagsService.getFlags().stream()
				.filter(flag -> !DUPLICATE_FLAGS.contains(flag.getWorldGuardFlag().getName()) && !flag.isHidden() && flag.isEditable()
						&& flag.getDisplayItem() != null
						&& (!flag.isHideIfNoValue() || flag.retrieveValue(protection.getProtectedRegion()) != null)
						&& (!flag.isHideIfNoPermission() || getPlayer().hasPermission(flag.getPermission())))
				.collect(Collectors.toList()));
		entries.addAll(protectionSettingsService.getSettings().stream()
				.filter(setting -> !(setting instanceof GreetingFarewellMessagesSetting) && setting.isEnabled()
						&& setting.isEditable()
						&& (setting.getPermission() == null || getPlayer().hasPermission(setting.getPermission())))
				.collect(Collectors.toList()));
		return entries;

	}

	@Override
	protected ItemStack generateEntityItem(Object entry) {
		if (entry instanceof PermissionInterface) {
			PermissionInterface permission = (PermissionInterface) entry;
			ItemBuilder builder = ItemBuilder.inst().fromItem(permission.getDisplayItem());
			builder.getLore().add("");
			builder.getLore().add("&fEstado actual:");
			builder.getLore().add("&7Visitantes: " + value(permission, PermissionGroup.NON_MEMBERS));
			builder.getLore().add("&7Miembros: " + value(permission, PermissionGroup.MEMBERS));
			builder.getLore().add("&7Propietarios: " + value(permission, PermissionGroup.OWNERS));
			builder.getLore().add("");
			builder.getLore().add("&#FFD166Izquierdo: &fmiembros");
			builder.getLore().add("&#FFD166Derecho: &fvisitantes");
			builder.getLore().add("&#FFD166Shift + izquierdo: &fpropietarios");
			return builder.build();
		}
		if (entry instanceof ProtectionFlag) {
			ProtectionFlag flag = (ProtectionFlag) entry;
			ItemBuilder builder = ItemBuilder.inst().fromItem(flag.getDisplayItem())
					.setReplacements(new Replacement("{value}", () -> ProtectionFlagUtilities
							.valueToString(flag.retrieveValue(protection.getProtectedRegion()))));
			builder.getLore().add("");
			builder.getLore().add("&#FFD166Click: &fcambiar estado");
			return builder.build();
		}
		SettingInterface<?> setting = (SettingInterface<?>) entry;
		ItemBuilder builder = ItemBuilder.inst().fromItem(setting.getDisplayItem())
				.setReplacements(new Replacement("{value}",
						() -> protection.getSettingValueAsStringSafely(setting, PermissionGroup.GENERIC)));
		builder.getLore().add("");
		builder.getLore().add("&#FFD166Click: &fcambiar estado");
		return builder.build();
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, Object entry) {
		if (entry instanceof PermissionInterface) {
			togglePermission((PermissionInterface) entry,
					e.isShiftClick() ? PermissionGroup.OWNERS
							: (e.isRightClick() ? PermissionGroup.NON_MEMBERS : PermissionGroup.MEMBERS));
		} else if (entry instanceof ProtectionFlag) {
			toggleFlag((ProtectionFlag) entry);
		} else if (entry instanceof BooleanSettingImpl && ProtectionUtilities.canSwitchSettings(protection, getPlayer())) {
			try {
				BooleanSettingImpl setting = (BooleanSettingImpl) entry;
				RoyaleProtectionBlocksAPI.getInstance().getPlayerInteractionsService().protectionSwitchSettingRequest(
						ProtectionSwitchSettingRequestInput.inst(getPlayer(), protection, setting, PermissionGroup.GENERIC,
								Boolean.FALSE.equals(protection.getSettingValue(setting, PermissionGroup.GENERIC))));
				updateInventory();
			} catch (RoyaleProtectionBlocksException ex) {
				ex.sendError(getPlayer());
			}
		}
	}

	private String value(PermissionInterface permission, PermissionGroup group) {
		return Boolean.TRUE.equals(protection.getPermissionValue(permission, group)) ? "&#00FB9A✔ Activado"
				: "&#FF3300✘ Desactivado";
	}

	@ItemExecutor("Messages-button")
	private void executeMessagesButton() {
		new ProtectionSettingsGreetingFarewellMessagesInventory(getPlayer(), protection).openInventory();
	}

	private void togglePermission(PermissionInterface permission, PermissionGroup group) {
		boolean editable = group == PermissionGroup.NON_MEMBERS ? permission.isNonMembersValueEditable()
				: (group == PermissionGroup.MEMBERS ? permission.isMembersValueEditable()
						: permission.isOwnersValueEditable());
		if (editable) {
			protection.setPermissionValue(permission, group,
					Boolean.FALSE.equals(protection.getPermissionValue(permission, group)));
			updateInventory();
		}
	}

	private void toggleFlag(ProtectionFlag flag) {
		if (!ProtectionFlagUtilities.isStateFlag(flag.getWorldGuardFlag())) {
			return;
		}
		try {
			FlagModificationRequestInput<State> request = FlagModificationRequestInput.inst(getPlayer(),
					flag.getWorldGuardFlag().getName(),
					flag.retrieveValue(protection.getProtectedRegion()) == State.ALLOW ? State.DENY : State.ALLOW);
			protection.performAllProtections(prot -> {
				try {
					((Protection) prot).getWorldGuardFlags().setFlag(request);
				} catch (RoyaleProtectionBlocksExceptionImpl ex) {
					ex.sendError(Bukkit.getConsoleSender());
				}
			});
			updateInventory();
		} catch (Throwable ex) {
			Exceptions.Protections.UNKNOWN.generateException(ex).sendError(getPlayer());
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

}
