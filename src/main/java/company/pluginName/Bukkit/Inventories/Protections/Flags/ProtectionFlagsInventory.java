package company.pluginName.Bukkit.Inventories.Protections.Flags;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.sk89q.worldguard.protection.flags.StateFlag.State;

import company.pluginName.Bukkit.Inventories.Protections.Permissions.ProtectionsPermissionsListInventory;
import company.pluginName.Bukkit.Inventories.Protections.Settings.ProtectionsSettingsListInventory;
import company.pluginName.Exceptions.Exceptions;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Modules.ProtectionFlagsPckg.ProtectionFlagsService;
import company.pluginName.Modules.ProtectionFlagsPckg.Objects.ProtectionFlag;
import company.pluginName.Modules.ProtectionFlagsPckg.Utils.ProtectionFlagUtilities;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionDisplayNames;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Listeners.PandaMessageListener.Callback;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Services.PandaMessageListenerService;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtectionFlags.FlagModificationRequestInput;

@Inventory("protections_flags")
public class ProtectionFlagsInventory extends PagedChestInventoryObject<ProtectionFlag> {

	private static final String MESSAGES_FLAGSTRINGSPECIFYINFO_PATH = "Messages.Flag-string-specify-info";

	@PandaInject
	private static PandaMessageListenerService messageListenerService;

	@PandaInject
	private static ProtectionFlagsService protectionFlagsService;

	private Protection protection;

	public ProtectionFlagsInventory(Player player, Protection protection) {
		super(player);

		this.protection = protection;

		setReplacements(new Replacement[] {
				new Replacement("{protection}", () -> ProtectionDisplayNames.forGui(protection)) });
		setTitleReplacements(getReplacements());
	}

	@Override
	protected List<ProtectionFlag> getEntityList() {
		return protectionFlagsService.getFlags().stream()
				.filter(flag -> !flag.isHidden() && flag.getDisplayItem() != null
						&& (!flag.isHideIfNoValue() || flag.retrieveValue(protection.getProtectedRegion()) != null)
						&& (!flag.isHideIfNoPermission() || getPlayer().hasPermission(flag.getPermission())))
				.collect(Collectors.toList());
	}

	@Override
	protected ItemStack generateEntityItem(ProtectionFlag entity) {
		return ItemBuilder
				.inst().fromItem(
						entity.getDisplayItem())
				.setReplacements(new Replacement("{value}", () -> ProtectionFlagUtilities
						.valueToString(entity.retrieveValue(protection.getProtectedRegion()))))
				.build();
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, ProtectionFlag entity) {
		try {
			if (entity.isEditable()) {
				if (ProtectionFlagUtilities.isStateFlag(entity.getWorldGuardFlag())) {
					FlagModificationRequestInput<State> request = FlagModificationRequestInput.inst(getPlayer(),
							entity.getWorldGuardFlag().getName(),
							entity.retrieveValue(protection.getProtectedRegion()) == State.ALLOW ? State.DENY
									: State.ALLOW);
					protection.performAllProtections(prot -> {
						try {
							((Protection) prot).getWorldGuardFlags().setFlag(request);
						} catch (RoyaleProtectionBlocksExceptionImpl ex) {
							ex.sendError(Bukkit.getConsoleSender());
						}
					});
					updateInventory();
				} else {
					messageListenerService.getListener().replaceListening(getPlayer().getUniqueId(), new Callback() {

						@Override
						public boolean message(String message) {
							try {
								FlagModificationRequestInput<Object> request = FlagModificationRequestInput.inst(
										getPlayer(), entity.getWorldGuardFlag().getName(),
										ProtectionFlagUtilities.stringToValue(entity.getWorldGuardFlag(), message));

								protection.performAllProtections(prot -> {
									try {
										((Protection) prot).getWorldGuardFlags().setFlag(request);
									} catch (RoyaleProtectionBlocksExceptionImpl ex) {
										ex.sendError(Bukkit.getConsoleSender());
									}
								});
							} catch (RoyaleProtectionBlocksExceptionImpl ex) {
								ex.sendError(getPlayer());
							} catch (Throwable ex) {
								Exceptions.Protections.UNKNOWN.generateException(ex).sendError(getPlayer());
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
									.get(MESSAGES_FLAGSTRINGSPECIFYINFO_PATH).toString()))
							.process().sendMessage(e.getWhoClicked());
				}
			}
		} catch (RoyaleProtectionBlocksExceptionImpl ex) {
			ex.sendError(getPlayer());
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

	@ItemExecutor("Permissions-tab")
	private void executePermissionsTab() {
		new ProtectionsPermissionsListInventory(getPlayer(), protection).openInventory();
	}

	@ItemExecutor("Settings-tab")
	private void executeSettingsTab() {
		new ProtectionsSettingsListInventory(getPlayer(), protection).openInventory();
	}

}
