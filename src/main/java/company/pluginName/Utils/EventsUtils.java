package company.pluginName.Utils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import company.pluginName.MainPluginClass;
import company.pluginName.API.RoyaleProtectionBlocksAPIImpl;
import company.pluginName.Exceptions.Exceptions.Protections;
import company.pluginName.Hooks.PlaceholderAPI.PlaceholderAPI;
import company.pluginName.Modules.FilePckg.Messages;
import company.pluginName.Modules.FilePckg.Settings;
import company.pluginName.Modules.PlayerInteractionsPckg.PlayerInteractionsServiceImpl;
import company.pluginName.Modules.PlayersDataPckg.PlayerDataService;
import company.pluginName.Modules.ProtectionBlocksPckg.ProtectionBlocksService;
import company.pluginName.Modules.ProtectionBlocksPckg.Objects.ProtectionBlock;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionUtilities;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Utils.TasksUtils;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemStackData.ItemStackDataUtilities;
import darkpanda73.PandaUtils.PandaUtilities.Player.Inventory.InventoryUtilities;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import lombok.AllArgsConstructor;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.ItemType;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.ProtectionBlocks.IProtectionBlock;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionCreationRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionRemovalRequestInput;

public class EventsUtils {

	@PandaInject
	private static MainPluginClass plugin;

	@PandaInject
	private static ProtectionBlocksService protectionBlocksService;

	@PandaInject
	private static PlayerInteractionsServiceImpl playerInteractionsService;

	@PandaInject
	private static PlayerDataService playerDataService;

	@PandaInject
	private static PlaceholderAPI placeholderApi;

	public static BlockPlaceResult onVanillaBlockPlaceEvent(Player player, Block block, EquipmentSlot hand,
			ItemStack defaultItem) {
		return onBlockPlaceEvent(EventOrigin.VANILLA, player, block, hand, defaultItem);
	}

	public static BlockPlaceResult onOraxenBlockPlaceEvent(Player player, Block block, EquipmentSlot hand,
			ItemStack defaultItem) {
		return onBlockPlaceEvent(EventOrigin.ORAXEN, player, block, hand, defaultItem);
	}

	public static BlockPlaceResult onNexoBlockPlaceEvent(Player player, Block block, EquipmentSlot hand,
			ItemStack defaultItem) {
		return onBlockPlaceEvent(EventOrigin.NEXO, player, block, hand, defaultItem);
	}

	public static BlockPlaceResult onItemsAdderBlockPlaceEvent(Player player, Block block, EquipmentSlot hand,
			ItemStack defaultItem) {
		return onBlockPlaceEvent(EventOrigin.ITEMS_ADDER, player, block, hand, defaultItem);
	}

	private static BlockPlaceResult onBlockPlaceEvent(EventOrigin eventOrigin, Player player, Block block,
			EquipmentSlot hand, ItemStack defaultItem) {
		ItemStack item = InventoryUtilities.getItemByHand(player, hand, defaultItem);

		if (item != null) {
			String protectionBlockId = null;

			try {
				protectionBlockId = ItemStackDataUtilities.getPersistentData(item, plugin,
						ProtectionBlock.PROTECTION_BLOCK_ID_KEY, String.class);
			} catch (Exception e) {
				MessageTemplate.inst(PandaPrefixedStringField.applyPrefix(String.format(
						"An error has ocurred trying to retrieve the Protection Block ID from an item: %s",
						e.getMessage()))).process().sendMessage(Bukkit.getConsoleSender());
				e.printStackTrace();
			}

			Protection prot = RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
					.findProtectionBySourceBlock(block);

			if (protectionBlockId != null && !protectionBlockId.isEmpty()) {
				ProtectionBlock protectionBlock = protectionBlocksService.getProtectionBlockById(protectionBlockId);

				if (protectionBlock == null) {
					return BlockPlaceResult.IGNORE;
				}

				if (Settings.SETTINGS_NETWORK_VIEWONLY.isTrue()) {
					Protections.PERMISSIONDENIED.generateException().sendError(player);
					return BlockPlaceResult.CANCEL;
				}

				if (prot != null) {
					Protections.Save.ALREADYEXISTS.generateException().sendError(player);
					return BlockPlaceResult.CANCEL;
				}

				Location playerLocation = player.getLocation();

				if (player.getGameMode() != GameMode.CREATIVE) {
					if (item.getAmount() > 1) {
						item.setAmount(item.getAmount() - 1);
					} else {
						if (hand == EquipmentSlot.OFF_HAND) {
							InventoryUtilities.setItemInOffHand(player, null);
						} else {
							InventoryUtilities.setItemInMainHand(player, null);
						}
					}
				}

				TasksUtils.executeOnAsync(() -> {
					try {
						playerInteractionsService.protectionCreationRequest(ProtectionCreationRequestInput.inst(player,
								player.getUniqueId(), protectionBlock, block.getLocation()));
					} catch (RoyaleProtectionBlocksException e2) {
						if (player.isOnline()) {
							e2.sendError(player);
						}

						TasksUtils.execute(() -> {
							ItemStack protectionBlockItem = protectionBlock != null ? protectionBlock.generateItem()
									: null;

							if (protectionBlockItem != null) {
								if (player.isOnline()) {
									if (player.getGameMode() != GameMode.CREATIVE) {
										player.getInventory().addItem(protectionBlockItem)
												.forEach((index, missingItem) -> player.getLocation().getWorld()
														.dropItem(player.getLocation(), missingItem));
									}
								} else {
									playerLocation.getWorld().dropItem(playerLocation, protectionBlockItem);
								}
							}
						});
					}
				});

				return BlockPlaceResult.SUCCESS;

			}

			if (prot != null) {
				if (!prot.getUtils().isProtectionBlockShown()) {
					IProtectionBlock protectionBlock = prot.getProtectionBlock();
					if (protectionBlock == null || protectionBlock.isSameType(item)) {
						MessageTemplate.inst(Messages.ERROR_PROTECTIONS_SAMEITEMASPROTECTION.applyPrefix()).process()
								.sendMessage(player);
						return BlockPlaceResult.CANCEL;
					}
				}
			}
		}
		return BlockPlaceResult.IGNORE;
	}

	public static BlockBreakResult onVanillaBlockBreakEvent(Player player, Block block) {
		return onBlockBreakEvent(EventOrigin.VANILLA, player, block);
	}

	public static BlockBreakResult onOraxenBlockBreakEvent(Player player, Block block) {
		return onBlockBreakEvent(EventOrigin.ORAXEN, player, block);
	}

	public static BlockBreakResult onNexoBlockBreakEvent(Player player, Block block) {
		return onBlockBreakEvent(EventOrigin.NEXO, player, block);
	}

	public static BlockBreakResult onItemsAdderBlockBreakEvent(Player player, Block block) {
		return onBlockBreakEvent(EventOrigin.ITEMS_ADDER, player, block);
	}

	private static BlockBreakResult onBlockBreakEvent(EventOrigin eventOrigin, Player player, Block block) {
		Protection protection = RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
				.findProtectionBySourceBlock(block);
		if (protection != null) {
			IProtectionBlock protectionBlock = protection.getProtectionBlock();

			if (protectionBlock != null && protectionBlock.getItemType() != eventOrigin.itemType) {
				return BlockBreakResult.IGNORE;
			}

			ProtectionUtilities.hideBlock(block);

			TasksUtils.executeOnAsync(() -> {
				try {
					playerInteractionsService
							.protectionRemovalRequest(ProtectionRemovalRequestInput.inst(player, protection));
				} catch (RoyaleProtectionBlocksException e) {
					if (e.getType() != RoyaleProtectionBlocksException.Type.PROTECTIONS_DELETE_CANCELLED) {
						e.sendError(player);
					}

					TasksUtils.execute(() -> ProtectionUtilities.showBlock(block, protectionBlock.getItem()));
				}
			});

			return BlockBreakResult.SUCCESS;
		}
		return BlockBreakResult.IGNORE;
	}

	@AllArgsConstructor
	public static enum EventOrigin {
		VANILLA(ItemType.VANILLA), ORAXEN(ItemType.ORAXEN), ITEMS_ADDER(ItemType.ITEMS_ADDER), NEXO(ItemType.NEXO);

		private ItemType itemType;
	}

	public static enum BlockPlaceResult {
		IGNORE, SUCCESS, CANCEL
	}

	public static enum BlockBreakResult {
		IGNORE, SUCCESS, CANCEL
	}

	public static enum BlockInteractResult {
		IGNORE, SUCCESS, CANCEL
	}

}
