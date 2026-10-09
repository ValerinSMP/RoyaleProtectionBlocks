package company.pluginName.Bukkit.Inventories.Protections.Banneds;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import company.pluginName.Modules.PlayersDataPckg.PlayerNameLookupService;
import org.bukkit.Material;
import org.bukkit.Bukkit;
import company.pluginName.Bukkit.Inventories.Shared.SearchResultGuard;
import royale.RoyaleProtectionBlocks.Plugin.API.RoyaleProtectionBlocksAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.Shared.ConfirmationInventory;
import company.pluginName.Exceptions.Exceptions;
import company.pluginName.MainPluginClass;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionDisplayNames;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionUtilities;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.PandaCachedPlayersService;
import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.Objects.PandaCachedPlayer;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryData;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import de.rapha149.signgui.exception.SignGUIVersionException;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.PlayerInteractionsService;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.Banneds.ProtectionBannedAddRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.Banneds.ProtectionBannedRemoveRequestInput;

@Inventory("protections_banneds")
public class ProtectionBannedsInventory extends PagedChestInventoryObject<UUID> {
	private static void compileExceptionBridge() throws RoyaleProtectionBlocksException { }

	private static final String ENTITY_DELETELORELINE_PATH = "Entity.Delete-lore-line";

	private enum Filter {
		ALL, ONLINE, OFFLINE;

		private Filter next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	@PandaInject
	private static PlayerInteractionsService playerInteractionsService;

	@PandaInject
	private static PandaCachedPlayersService cachedPlayersService;
	@PandaInject
	private static PlayerNameLookupService playerNameLookup;

	@PandaInject
	private static MainPluginClass plugin;

	private Protection protection;
	private Filter currentFilter = Filter.ALL;
	private String lastSearch = "Ninguna";
	private long searchRequest;

	public ProtectionBannedsInventory(Player player, Protection protection) {
		super(player);

		this.protection = protection;

		setReplacements(new Replacement[] {
				new Replacement("{protection}", () -> ProtectionDisplayNames.forGui(protection)),
				new Replacement("{search}", () -> lastSearch),
				new Replacement("{current_page}", () -> Integer.toString(getCurrentPage())),
				new Replacement("{max_page}", () -> Integer.toString(getCurrentMaxPage())) });
		setTitleReplacements(getReplacements());
	}

	@Override
	protected List<UUID> getEntityList() {
		return protection.getBanneds().stream().filter(uuid -> currentFilter == Filter.ALL
				|| (currentFilter == Filter.ONLINE) == (Bukkit.getPlayer(uuid) != null)).collect(Collectors.toList());
	}

	@ItemGenerator("All-filter-button")
	private ItemStack generateAllFilterButton(Item item) {
		return currentFilter == Filter.ALL ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Online-filter-button")
	private ItemStack generateOnlineFilterButton(Item item) {
		return currentFilter == Filter.ONLINE ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Offline-filter-button")
	private ItemStack generateOfflineFilterButton(Item item) {
		return currentFilter == Filter.OFFLINE ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemExecutor("All-filter-button")
	@ItemExecutor("Online-filter-button")
	@ItemExecutor("Offline-filter-button")
	private void executeFilterButton() {
		currentFilter = currentFilter.next();
		updateEntityList();
		updateInventory();
	}

	@ItemGenerator("Search-button")
	private ItemStack generateSearchButton(Item item) {
		return ProtectionUtilities.canAddBanned(protection, getPlayer()) ? item.getItems().get(Item.DISPLAYITEM_KEY)
				: null;
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
		getPlayer().closeInventory();
		try {
			SignGUI.builder().setLines("", "Escribe arriba", "el nombre", "a bloquear")
					.setType(Material.OAK_SIGN).callHandlerSynchronously(plugin).setHandler((player, result) -> {
						String name = result.getLineWithoutColor(0).trim();
						if (name.isEmpty()) {
							return Collections.singletonList(
									SignGUIAction.displayNewLines("", "Escribe arriba", "el nombre", "a bloquear"));
						}
						lastSearch = name;
						long request = ++searchRequest;
						playerNameLookup.find(name).whenComplete((target, error) -> {
							if (!plugin.isEnabled()) return;
							Bukkit.getScheduler().runTask(plugin, () -> {
								if (!SearchResultGuard.isCurrent(request, searchRequest,
										Bukkit.getPlayer(player.getUniqueId()) == player, player.isOnline(),
										RoyaleProtectionBlocksAPI.getInstance().getProtectionsService()
												.findProtectionById(protection.getProtectionId()) == protection,
										ProtectionUtilities.canAddBanned(protection, player))) return;
								if (error != null || target == null) {
									Exceptions.Protections.UNKNOWN.generateException(error).sendError(player);
								} else if (!protection.isMainOwner(target.getUuid())) {
									try {
										compileExceptionBridge();
										playerInteractionsService.protectionBannedAddRequest(
												ProtectionBannedAddRequestInput.inst(player, protection, target.getUuid()));
									} catch (RoyaleProtectionBlocksException failure) {
										failure.sendError(player);
									}
								}
								updateEntityList();
								openInventory();
							});
						});
						return Collections.emptyList();
				}).build().open(getPlayer());
		} catch (SignGUIVersionException e) {
			Exceptions.Protections.UNKNOWN.generateException(e).sendError(getPlayer());
			openInventory();
		}
	}

	@Override
	protected ItemStack generateEntityItem(UUID entity) {
		PandaCachedPlayer pl = cachedPlayersService.getCachedPlayer(entity);

		final boolean canRemove = ProtectionUtilities.canRemoveMember(protection, getPlayer());

		ItemBuilder builder = ItemBuilder.inst().setMaterial(Material.PLAYER_HEAD)
				.fromMap(getChestInventoryData().getCustomFields(), PagedChestInventoryData.ENTITY_PATH)
				.setReplacements(new Replacement("{player}", () -> pl != null ? pl.getName() : "???"));

		List<String> lore = builder.getLore();
		if (canRemove) {
			lore.add(" ");
			lore.add(getChestInventoryData().getCustomFields().get(ENTITY_DELETELORELINE_PATH).toString());
		}
		builder.setLore(lore);

		return processPlayerHead(builder, entity);
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, UUID entity) {
		final boolean canRemove = ProtectionUtilities.canRemoveBanned(protection, getPlayer());

		if (canRemove) {
			new ConfirmationInventory(getPlayer(), () -> {
				try {
					compileExceptionBridge();
					playerInteractionsService.protectionBannedRemoveRequest(
							ProtectionBannedRemoveRequestInput.inst(getPlayer(), protection, entity));
				} catch (RoyaleProtectionBlocksException e1) {
					e1.sendError(getPlayer());
				}
				updateEntityList();
			}).openInventory();
		}
	}

}
