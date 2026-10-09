package company.pluginName.Bukkit.Inventories.v2.Protections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.Protections.Banneds.ProtectionBannedsInventory;
import company.pluginName.Bukkit.Inventories.Protections.Members.ProtectionMembersInventory;
import company.pluginName.Bukkit.Inventories.Protections.Merge.ProtectionMergeInventory;
import company.pluginName.Bukkit.Inventories.Protections.Permissions.ProtectionsPermissionsListInventory;
import company.pluginName.Bukkit.Inventories.Protections.Split.ProtectionSplitInventory;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.MainPluginClass;
import company.pluginName.Modules.FilePckg.Messages;
import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import company.pluginName.Modules.PlayersDataPckg.PlayerDataService;
import company.pluginName.Modules.PlayersDataPckg.Objects.PlayerData;
import company.pluginName.Modules.ProtectionParticlesPckg.ProtectionParticlesService;
import company.pluginName.Modules.ProtectionsPckg.ProtectionsServiceImpl;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionUtilities;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemPregenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.ChestInventoryObject;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.GeneratedItem;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Utilities.Java.Arrays.ArrayUtilities;
import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import de.rapha149.signgui.exception.SignGUIVersionException;
import royale.RoyaleProtectionBlocks.Plugin.API.RoyaleProtectionBlocksAPI;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.PlayerInteractionsService;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Inventories.OpenProtectionRemovalInventoryRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionChangeDisplayItemRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionHideBlockRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionRenameRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionShowBlockRequestInput;

@Inventory("protections_manage_v2")
public class ProtectionsManageInventory extends ChestInventoryObject {

	private static final String PROTECTIONINFO_CHANGEDISPLAYITEMLORELINE = "Change-display-item-lore-line";
	private static final String PROTECTIONINFO_RESETDISPLAYITEMLORELINE = "Reset-display-item-lore-line";

	private static final String TOGGLEBLOCKBUTTON_SHOWBLOCKITEM_PATH = "Show-block-item";

	private static final String TOGGLEBOUNDARYBUTTON_HIDEBOUNDARYITEM_PATH = "Hide-boundary-item";

	@PandaInject
	private static MainPluginClass plugin;

	@PandaInject
	private static ProtectionsServiceImpl protectionsService;

	@PandaInject
	private static PlayerInteractionsService playerInteractionsService;

	@PandaInject
	private static ProtectionParticlesService protectionParticlesService;

	@PandaInject
	private static PlayerDataService playerDataService;

	@PandaInject
	private static PlaceholdersService placeholdersService;

	private Protection protection;

	public ProtectionsManageInventory(Player player, Protection protection) {
		super(player);

		this.protection = protection;

		Replacement[] playerReplacements = placeholdersService.getPlayerReplacements(getPlayer());
		Replacement[] protectionReplacements = placeholdersService.getProtectionReplacements(protection);
		Replacement[] protectionBlockReplacements = placeholdersService
				.getProtectionBlockReplacements(protection.getProtectionBlock());

		setReplacements(ArrayUtilities.join(
				new Replacement[playerReplacements.length + protectionReplacements.length
						+ protectionBlockReplacements.length],
				playerReplacements, protectionReplacements, protectionBlockReplacements));
		setTitleReplacements(getReplacements());
	}

	@ItemPregenerator("Toggle-block-button")
	private static void pregenerateToggleBlockButton(Item item) {
		item.getItems().put(Item.DISPLAYITEM_KEY,
				ItemBuilder.inst().fromMap(item.getData(), Item.DISPLAYITEM_KEY).build());
		item.getItems().put(TOGGLEBLOCKBUTTON_SHOWBLOCKITEM_PATH,
				ItemBuilder.inst().fromMap(item.getData(), TOGGLEBLOCKBUTTON_SHOWBLOCKITEM_PATH).build());
	}

	@ItemPregenerator("Toggle-boundary-button")
	private static void pregenerateToggleBoundaryButton(Item item) {
		item.getItems().put(Item.DISPLAYITEM_KEY,
				ItemBuilder.inst().fromMap(item.getData(), Item.DISPLAYITEM_KEY).build());
		item.getItems().put(TOGGLEBOUNDARYBUTTON_HIDEBOUNDARYITEM_PATH,
				ItemBuilder.inst().fromMap(item.getData(), TOGGLEBOUNDARYBUTTON_HIDEBOUNDARYITEM_PATH).build());
	}

	@ItemGenerator("Protection-info")
	private ItemStack generateDisplayItemButton(Item item) {
		ItemBuilder builder = ItemBuilder.inst().fromMap(item.getData(), Item.DISPLAYITEM_KEY);

		if (ProtectionUtilities.canChangeDisplayItem(protection, getPlayer())) {
			List<String> extraLore = new ArrayList<>();

			Object changeLine = item.getData().get(PROTECTIONINFO_CHANGEDISPLAYITEMLORELINE);
			if (changeLine != null) {
				extraLore.add(changeLine.toString());
			}

			if (this.protection.getDisplayItem() != null) {
				Object resetLine = item.getData().get(PROTECTIONINFO_RESETDISPLAYITEMLORELINE);
				if (resetLine != null) {
					extraLore.add(resetLine.toString());
				}
			}

			if (!extraLore.isEmpty()) {
				builder.getLore().add("&0");
				builder.getLore().addAll(extraLore);
			}
		}

		return builder.apply(this.protection.getDisplayItemOrDefault().clone());
	}

	@ItemGenerator("Toggle-block-button")
	private ItemStack generateToggleBlockButton(Item item) {
		return protection.canToggleBlockVisibility(getPlayer())
				? item.getItems().get(protection.getUtils().isProtectionBlockShown() ? Item.DISPLAYITEM_KEY
						: TOGGLEBLOCKBUTTON_SHOWBLOCKITEM_PATH)
				: null;
	}

	@ItemGenerator("Merge-button")
	private ItemStack generateMergeButton(Item item) {
		return ProtectionUtilities.canMerge(protection, getPlayer()) ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Delete-button")
	private ItemStack generateDeleteButton(Item item) {
		return ProtectionUtilities.canDelete(protection, getPlayer()) ? item.getItems().get(Item.DISPLAYITEM_KEY)
				: null;
	}

	@ItemGenerator("Toggle-boundary-button")
	private ItemStack generateToggleBoundaryButton(Item item) {
		return item.getItems()
				.get(protectionParticlesService.isActive(getPlayer()) ? TOGGLEBOUNDARYBUTTON_HIDEBOUNDARYITEM_PATH
						: Item.DISPLAYITEM_KEY);
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

	@ItemExecutor("Protection-info")
	private void executeChangeDisplayItemButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		if (ProtectionUtilities.canChangeDisplayItem(protection, getPlayer())) {
			try {
				if (e.getCursor() != null && e.getCursor().getType() != Material.AIR) {
					RoyaleProtectionBlocksAPI.getInstance().getPlayerInteractionsService()
							.protectionChangeDisplayItemRequest(ProtectionChangeDisplayItemRequestInput
									.inst(getPlayer(), protection, e.getCursor()));
					updateInventory();
				} else if (protection.getDisplayItem() != null) {
					RoyaleProtectionBlocksAPI.getInstance().getPlayerInteractionsService()
							.protectionChangeDisplayItemRequest(
									ProtectionChangeDisplayItemRequestInput.inst(getPlayer(), protection, null));
					updateInventory();
				}
			} catch (RoyaleProtectionBlocksException e1) {
				e1.sendError(getPlayer());
			}
		}
	}

	@ItemExecutor("Members-button")
	private void executeMembersButton() {
		new ProtectionMembersInventory(getPlayer(), protection).openInventory();
	}

	@ItemExecutor("Banneds-button")
	private void executeBannedsButton() {
		new ProtectionBannedsInventory(getPlayer(), protection).openInventory();
	}

	@ItemExecutor("Rename-button")
	private void executeRenameButton() {
		getPlayer().closeInventory();
		try {
			SignGUI.builder().setLines("", "Escribe arriba", "el nuevo nombre", "de la protección")
					.setType(Material.OAK_SIGN).callHandlerSynchronously(plugin).setHandler((player, result) -> {
						String name = result.getLineWithoutColor(0).trim();
						if (name.isEmpty()) {
							return Collections.singletonList(SignGUIAction.displayNewLines("", "Escribe arriba",
									"el nuevo nombre", "de la protección"));
						}
						try {
							playerInteractionsService.protectionRenameRequest(
									ProtectionRenameRequestInput.inst(getPlayer(), protection, name));
							MessageTemplate.inst(Messages.MESSAGE_PROTECTIONS_RENAMEDSUCCESSFULLY.applyPrefix())
									.process().sendMessage(getPlayer());
						} catch (RoyaleProtectionBlocksException e) {
							e.sendError(getPlayer());
						}
						return Collections.singletonList(SignGUIAction.runSync(plugin, this::openInventory));
					}).build().open(getPlayer());
		} catch (SignGUIVersionException e) {
			e.printStackTrace();
			openInventory();
		}
	}

	@ItemExecutor("Toggle-block-button")
	private void executeToggleBlockButton() {
		try {
			if (protection.getUtils().isProtectionBlockShown()) {
				playerInteractionsService
						.protectionHideBlockRequest(ProtectionHideBlockRequestInput.inst(getPlayer(), protection));
				updateInventory();
			} else {
				playerInteractionsService
						.protectionShowBlockRequest(ProtectionShowBlockRequestInput.inst(getPlayer(), protection));
				updateInventory();
			}
		} catch (RoyaleProtectionBlocksException e) {
			e.sendError(getPlayer());
		}
	}

	@ItemExecutor("Merge-button")
	private void executeMergeButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		try {
			if (e.isLeftClick()) {
				new ProtectionMergeInventory(getPlayer(), protection).openInventory();
			} else if (e.isRightClick()) {
				new ProtectionSplitInventory(getPlayer(), protection).openInventory();
			}
		} catch (RoyaleProtectionBlocksExceptionImpl e1) {
			e1.sendError(getPlayer());
		}
	}

	@ItemExecutor("Delete-button")
	private void executeDeleteButton() {
		try {
			playerInteractionsService.openProtectionRemovalInventoryRequest(
					OpenProtectionRemovalInventoryRequestInput.inst(getPlayer(), protection));
		} catch (RoyaleProtectionBlocksException e) {
			e.sendError(getPlayer());
		}
	}

	@ItemExecutor("Toggle-boundary-button")
	private void executeToggleBoundaryButton() {
		PlayerData pd = playerDataService.getPlayerData(getPlayer());
		protectionParticlesService.toggleView(getPlayer(), pd.isStaffMode());
		updateInventory();
	}

	@ItemExecutor("Rules-button")
	private void executeRulesButton() {
		new ProtectionsPermissionsListInventory(getPlayer(), protection).openInventory();
	}

}
