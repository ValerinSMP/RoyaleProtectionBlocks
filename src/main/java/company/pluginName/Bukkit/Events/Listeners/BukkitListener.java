package company.pluginName.Bukkit.Events.Listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityMountEvent;

import company.pluginName.MainPluginClass;
import company.pluginName.API.RoyaleProtectionBlocksAPIImpl;
import company.pluginName.Exceptions.Exceptions;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Modules.FilePckg.Settings;
import company.pluginName.Modules.PermissionsPckg.PermissionsService;
import company.pluginName.Modules.PlayerInteractionsPckg.PlayerInteractionsServiceImpl;
import company.pluginName.Modules.PlayersDataPckg.PlayerDataService;
import company.pluginName.Modules.PlayersDataPckg.Objects.PlayerData;
import company.pluginName.Modules.ProtectionBlocksPckg.ProtectionBlocksService;
import company.pluginName.Modules.ProtectionBlocksPckg.Objects.ProtectionBlock;
import company.pluginName.Modules.ProtectionPermissionsPckg.ProtectionPermissionsService;
import company.pluginName.Modules.ProtectionsPckg.ProtectionsServiceImpl;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionDisplayNames;
import company.pluginName.Modules.SQLPckg.SQLService;
import company.pluginName.Modules.SettingsPckg.SettingsService;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaCraftableItems.Utils.RecipeUtilities;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaListener;
import darkpanda73.PandaUtils.PandaPlugin.Utils.TasksUtils;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemStackData.ItemStackDataUtilities;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import darkpanda73.PandaUtils.Utilities.Java.Objects.Pair;
import royale.RoyaleProtectionBlocks.Plugin.API.RoyaleProtectionBlocksAPI;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.Objects.ProtectionRemovalData;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionTeleportHomeRequestInput;

@PandaListener
public class BukkitListener implements Listener {

	@PandaInject
	private ProtectionBlocksService protectionBlocksService;

	@PandaInject
	private SettingsService protectionSettingsService;

	@PandaInject
	private ProtectionsServiceImpl protectionsService;

	@PandaInject
	private PlayerDataService playerDataService;

	@PandaInject
	private SQLService sqlService;

	@PandaInject
	private PlayerInteractionsServiceImpl playerInteractionsService;

	@PandaInject
	private MainPluginClass plugin;

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (Settings.SETTINGS_NETWORK_VIEWONLY.isTrue()) {
			return;
		}
		if (!e.getPlayer().hasPlayedBefore() && !Settings.SETTINGS_PROTECTION_STARTERBLOCK.toString().isEmpty()) {
			ProtectionBlock block = protectionBlocksService
					.getProtectionBlockById(Settings.SETTINGS_PROTECTION_STARTERBLOCK.toString());
			if (block != null) {
				ItemStack item = block.generateItem();

				if (item != null) {
					e.getPlayer().getInventory().addItem(item);
				}
			}
		}

		List<Protection> ownedProtections = new ArrayList<>(
				protectionsService.findProtectionsByOwner(e.getPlayer().getUniqueId()));
		ownedProtections.forEach(IProtection::updateOwnerData);
		migrateLegacyProtectionNames(e.getPlayer(), ownedProtections);

		PlayerData playerData = playerDataService.getPlayerData(e.getPlayer());
		if (playerData != null) {
			playerData.setStaffMode(PermissionsService.STAFFMODE.hasPermission(e.getPlayer()));
		}

		processPendingProtectionTeleport(e.getPlayer());
	}

	private void processPendingProtectionTeleport(Player player) {
		TasksUtils.executeWithDelay(() -> TasksUtils.executeOnAsync(() -> {
			try {
				Pair<String, Long> pending = sqlService.getPendingProtectionTeleport(player.getUniqueId());
				if (pending == null) {
					return;
				}

				long expirationMillis = Math.max(1L,
						Settings.SETTINGS_NETWORK_PENDINGTELEPORTEXPIRATIONSECONDS.getContent()) * 1000L;
				if (isPendingTeleportExpired(pending.getSecond(), System.currentTimeMillis(), expirationMillis)) {
					sqlService.deletePendingProtectionTeleport(player.getUniqueId());
					return;
				}

				TasksUtils.execute(() -> consumePendingProtectionTeleport(player, pending.getFirst()));
			} catch (RoyaleProtectionBlocksExceptionImpl exception) {
				exception.sendError(Bukkit.getConsoleSender());
			}
		}), 10L);
	}

	private void consumePendingProtectionTeleport(Player player, String protectionId) {
		if (!player.isOnline()) {
			return;
		}
		Protection protection = protectionsService.findProtectionById(protectionId);
		TasksUtils.executeOnAsync(() -> {
			try {
				sqlService.deletePendingProtectionTeleport(player.getUniqueId());
			} catch (RoyaleProtectionBlocksExceptionImpl exception) {
				exception.sendError(Bukkit.getConsoleSender());
				return;
			}

			TasksUtils.execute(() -> {
				if (!player.isOnline()) {
					return;
				}
				if (protection == null) {
					Exceptions.Protections.NOTFOUND.generateException().sendError(player);
					return;
				}
				try {
					playerInteractionsService.protectionTeleportHomeRequest(
							ProtectionTeleportHomeRequestInput.inst(player, protection));
				} catch (RoyaleProtectionBlocksException exception) {
					exception.sendError(player);
				}
			});
		});
	}

	static boolean isPendingTeleportExpired(long createdAt, long now, long expirationMillis) {
		return createdAt <= 0L || now - createdAt > expirationMillis;
	}

	private void migrateLegacyProtectionNames(Player player, List<Protection> protections) {
		protections.sort(Comparator.comparingLong(Protection::getCreatedDate));
		Set<Integer> usedNumbers = new HashSet<>();
		protections.stream().map(ProtectionDisplayNames::automaticNumber).filter(number -> number != null)
				.forEach(usedNumbers::add);

		int nextNumber = 1;
		for (Protection protection : protections) {
			if (!ProtectionDisplayNames.isLegacyAutomaticName(protection)) {
				continue;
			}
			while (usedNumbers.contains(nextNumber)) {
				nextNumber++;
			}
			try {
				protection.setDisplayName(ProtectionDisplayNames.automaticName(player.getName(), nextNumber));
				usedNumbers.add(nextNumber++);
			} catch (RoyaleProtectionBlocksExceptionImpl exception) {
				exception.sendError(Bukkit.getConsoleSender());
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerExit(PlayerQuitEvent e) {
		if (Settings.SETTINGS_NETWORK_VIEWONLY.isTrue()) {
			return;
		}
		TasksUtils.execute(() -> RoyaleProtectionBlocksAPI.getInstance().getProtectionsService()
				.findProtectionsByOwner(e.getPlayer().getUniqueId()).forEach(IProtection::updateOwnerData));
	}

	@EventHandler
	public void onMoveEvent(PlayerMoveEvent e) {
		if (!e.getFrom().getBlock().equals(e.getTo().getBlock())) {
			PlayerData playerData = playerDataService.getPlayerData(e.getPlayer());

			if (playerData != null && playerData.isTeleporting()) {
				playerData.cancelTeleport();
				Exceptions.Protections.Teleport.CANCELLEDDUEMOVEMENT.generateException().sendError(e.getPlayer());
			}
		}
	}

	@EventHandler
	public void onWorldLoad(WorldLoadEvent e) {
		protectionsService.getProtectionsByWorld().getOrDefault(e.getWorld().getName(), Collections.emptyList())
				.stream().forEach(protection -> {
					try {
						protection.getOrRegenerateProtectedRegion();
					} catch (RoyaleProtectionBlocksExceptionImpl e1) {
						MessageTemplate
								.inst(PandaPrefixedStringField
										.applyPrefix("&7Removing protection '" + protection.getProtectionId()
												+ "' as it couldn't be found on '" + protection.getWorldName()
												+ "' and were problems trying to regenerate the region."))
								.process().sendMessage(Bukkit.getConsoleSender());

						try {
							RoyaleProtectionBlocksAPI.getInstance().getProtectionsService()
									.delete(ProtectionRemovalData.inst(null, protection.getProtectionId()));
						} catch (RoyaleProtectionBlocksException e2) {
							e2.sendError(Bukkit.getConsoleSender());
						}
					}
				});
	}

	@EventHandler
	public void onPlayerMount(EntityMountEvent e) {
		if (ProtectionPermissionsService.RIDEVEHICLES_PERMISSION.isEnabled() && e.getEntity() instanceof Player) {
			Player player = (Player) e.getEntity();

			Protection protection = RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
					.findProtectionByLocation(e.getEntity().getLocation());

			if (protection != null && Boolean.FALSE.equals(
					protection.getPermissionValue(ProtectionPermissionsService.RIDEVEHICLES_PERMISSION, player))) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onPlayerConsumeOnFurnace(FurnaceBurnEvent event) {
		Furnace furnace = (Furnace) event.getBlock().getState();

		String protectionBlockId = null;

		try {
			protectionBlockId = ItemStackDataUtilities.getPersistentData(furnace.getInventory().getSmelting(), plugin,
					ProtectionBlock.PROTECTION_BLOCK_ID_KEY, String.class);
		} catch (Exception e) {
			MessageTemplate.inst(PandaPrefixedStringField.applyPrefix(
					String.format("An error has ocurred trying to retrieve the Protection Block ID from an item: %s",
							e.getMessage())))
					.process().sendMessage(Bukkit.getConsoleSender());
			e.printStackTrace();
		}

		if (protectionBlockId != null && !protectionBlockId.isEmpty()) {
			event.setCancelled(true);
			return;
		}

		try {
			protectionBlockId = ItemStackDataUtilities.getPersistentData(furnace.getInventory().getFuel(), plugin,
					ProtectionBlock.PROTECTION_BLOCK_ID_KEY, String.class);
		} catch (Exception e) {
			MessageTemplate.inst(PandaPrefixedStringField.applyPrefix(
					String.format("An error has ocurred trying to retrieve the Protection Block ID from an item: %s",
							e.getMessage())))
					.process().sendMessage(Bukkit.getConsoleSender());
			e.printStackTrace();
		}

		if (protectionBlockId != null && !protectionBlockId.isEmpty()) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler
	public void onPlayerConsumeOnFurnace(PrepareItemCraftEvent event) {
		String recipeKey = event.getRecipe() != null ? RecipeUtilities.getKey(event.getRecipe()) : null;

		if (recipeKey != null && !recipeKey.isEmpty() && !recipeKey.startsWith("royaleprotectionblocks:")
				&& Arrays.stream(event.getInventory().getMatrix()).anyMatch(item -> {
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

					return protectionBlockId != null && !protectionBlockId.isEmpty();
				})) {
			event.getInventory().setResult(null);
		}
	}

}
