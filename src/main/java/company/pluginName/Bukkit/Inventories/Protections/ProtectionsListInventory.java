package company.pluginName.Bukkit.Inventories.Protections;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.Protections.Invitations.ProtectionInvitationsInventory;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import company.pluginName.Modules.FilePckg.Settings;
import company.pluginName.Modules.PlayerInteractionsPckg.PlayerInteractionsServiceImpl;
import company.pluginName.Modules.PlayersDataPckg.PlayerDataService;
import company.pluginName.Modules.ProtectionsPckg.ProtectionsServiceImpl;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionDisplayNames;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException.Type;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Inventories.OpenProtectionManagementInventoryRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Inventories.OpenProtectionRemovalInventoryRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionTeleportHomeRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Objects.SimpleLocation;

@Inventory("protections_list")
public class ProtectionsListInventory extends PagedChestInventoryObject<Protection> {

	public static final String ENTITY_TELEPORTLORELINE_PATH = "Entity.Teleport-lore-line";
	public static final String ENTITY_TELEPORTNOTSETLORELINE_PATH = "Entity.Teleport-not-set-lore-line";
	public static final String ENTITY_EDITLORELINE_PATH = "Entity.Edit-lore-line";
	public static final String ENTITY_DELETELORELINE_PATH = "Entity.Delete-lore-line";

	@AllArgsConstructor
	@Getter
	private static enum Filter {
		ALL((player, protection) -> true),
		OWN((player, protection) -> protection.getOwnerUuid().equals(player.getUniqueId())),
		OTHERS((player, protection) -> !protection.getOwnerUuid().equals(player.getUniqueId()));

		private BiFunction<Player, Protection, Boolean> filterFunction;

		public Filter previous() {
			return values()[(values().length + ordinal() - 1) % values().length];
		}

		public Filter next() {
			return values()[(values().length + ordinal() + 1) % values().length];
		}
	}

	@AllArgsConstructor
	@Getter
	private static enum Sort {
		CREATION_DATE(
				(protection1, protection2) -> Long.compare(protection1.getCreatedDate(), protection2.getCreatedDate())),
		NAME((protection1, protection2) -> protection1.getDisplayName().compareTo(protection2.getDisplayName())),
		WORLD((protection1, protection2) -> protection1.getWorldName().compareTo(protection2.getWorldName()));

		private Comparator<Protection> sortFunction;

		public Sort previous() {
			return values()[(values().length + ordinal() - 1) % values().length];
		}

		public Sort next() {
			return values()[(values().length + ordinal() + 1) % values().length];
		}
	}

	@PandaInject
	private static ProtectionsServiceImpl protectionsService;

	@PandaInject
	private static PlaceholdersService placeholdersService;

	@PandaInject
	private static PlayerInteractionsServiceImpl playerInteractionsService;

	@PandaInject
	private static PlayerDataService playerDataService;

	private Filter currentFilter = Filter.ALL;
	private Sort currentSort = Sort.CREATION_DATE;

	public ProtectionsListInventory(Player player) {
		super(player);

		Replacement[] playerReplacements = placeholdersService.getPlayerReplacements(player);
		setReplacements(playerReplacements);
	}

	@Override
	protected List<Protection> getEntityList() {
		return protectionsService.findAllowedParentProtectionsByPlayer(getPlayer())
				.filter(protection -> this.currentFilter.getFilterFunction().apply(getPlayer(), protection))
				.sorted(this.currentSort.getSortFunction()).collect(Collectors.toList());

	}

	@Override
	protected ItemStack generateEntityItem(Protection protection) {
		final boolean viewOnly = Settings.SETTINGS_NETWORK_VIEWONLY.isTrue();
		final boolean canTeleport = protection.canTeleport(getPlayer());
		final boolean canManage = !viewOnly && ProtectionUtilities.canManage(protection, getPlayer());
		final boolean canDelete = !viewOnly && ProtectionUtilities.canDelete(protection, getPlayer());

		Location homeLocation = viewOnly ? null : protection.getHome();
		SimpleLocation loc = protection.getSimpleLocation();
		Replacement[] replacements = {
				new Replacement("{protection}", () -> ProtectionDisplayNames.forGui(protection)),
				new Replacement("{protection_owner}",
						() -> protection.getOwnerName() != null ? protection.getOwnerName() : "???"),
				new Replacement("{world}", () -> protection.getWorldName()),
				new Replacement("{location_x}", () -> loc != null ? String.valueOf(loc.getBlockX()) : "???"),
				new Replacement("{location_y}", () -> loc != null ? String.valueOf(loc.getBlockY()) : "???"),
				new Replacement("{location_z}", () -> loc != null ? String.valueOf(loc.getBlockZ()) : "???") };
		Replacement[] protectionReplacements = placeholdersService.getProtectionReplacements(protection);

		ItemBuilder itemBuilder = ItemBuilder.inst().fromMap(getChestInventoryData().getCustomFields(), "Entity")
				.setReplacements(
						ArrayUtilities.join(new Replacement[protectionReplacements.length + replacements.length],
								protectionReplacements, replacements));

		List<String> lore = new ArrayList<>(getChestInventoryData().getEntityLore());
		lore.add("&0");

		if ((viewOnly && canTeleport) || (homeLocation != null && canTeleport)
				|| (!viewOnly && protection.isMainOwner(getPlayer().getUniqueId()))) {
			lore.add(viewOnly || homeLocation != null
					? getChestInventoryData().getCustomFields().get(ENTITY_TELEPORTLORELINE_PATH).toString()
					: getChestInventoryData().getCustomFields().get(ENTITY_TELEPORTNOTSETLORELINE_PATH).toString());
		}

		if (canManage) {
			lore.add(getChestInventoryData().getCustomFields().get(ENTITY_EDITLORELINE_PATH).toString());
		}

		if (canDelete) {
			lore.add(getChestInventoryData().getCustomFields().get(ENTITY_DELETELORELINE_PATH).toString());
		}

		return itemBuilder.setLore(lore).apply(protection.getDisplayItemOrDefault().clone());
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, Protection protection) {
		boolean viewOnly = Settings.SETTINGS_NETWORK_VIEWONLY.isTrue();
		Location homeLocation = viewOnly ? null : protection.getHome();

		if (e.getClick() == ClickType.LEFT) {
			if ((viewOnly || homeLocation != null) && protection.canTeleport(getPlayer())) {
				try {
					playerInteractionsService.protectionTeleportHomeRequest(
							ProtectionTeleportHomeRequestInput.inst(getPlayer(), protection));
				} catch (RoyaleProtectionBlocksException e1) {
					e1.sendError(getPlayer());
				}
			}
		} else if (!viewOnly && e.getClick() == ClickType.RIGHT) {
			try {
				playerInteractionsService.openProtectionManagementInventoryRequest(
						OpenProtectionManagementInventoryRequestInput.inst(getPlayer(), protection));
			} catch (RoyaleProtectionBlocksExceptionImpl ex) {
				if (ex.getType() == Type.PROTECTIONS_BLOCKED) {
					ex.sendError(getPlayer());
				}
			}
		} else if (!viewOnly && e.getClick() == ClickType.SHIFT_RIGHT) {
			try {
				playerInteractionsService.openProtectionRemovalInventoryRequest(
						OpenProtectionRemovalInventoryRequestInput.inst(getPlayer(), protection));
			} catch (RoyaleProtectionBlocksExceptionImpl ex) {
			}
		}

	}

	@ItemGenerator("All-filter-button")
	private ItemStack generateAllFilterItem(Item item) {
		return this.currentFilter == Filter.ALL ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Invitations-button")
	private ItemStack generateInvitationsItem(Item item) {
		return Settings.SETTINGS_NETWORK_VIEWONLY.isTrue() ? null : item.getItems().get(Item.DISPLAYITEM_KEY);
	}

	@ItemGenerator("Own-filter-button")
	private ItemStack generateOwnFilterItem(Item item) {
		return this.currentFilter == Filter.OWN ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Others-filter-button")
	private ItemStack generateOthersFilterItem(Item item) {
		return this.currentFilter == Filter.OTHERS ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Creation-date-sort-button")
	private ItemStack generatePriceSortItem(Item item) {
		return this.currentSort == Sort.CREATION_DATE ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Name-sort-button")
	private ItemStack generateNameSortItem(Item item) {
		return this.currentSort == Sort.NAME ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("World-sort-button")
	private ItemStack generateWorldSortItem(Item item) {
		return this.currentSort == Sort.WORLD ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemExecutor("All-filter-button")
	@ItemExecutor("Own-filter-button")
	@ItemExecutor("Others-filter-button")
	private void executeFilter() {
		this.currentFilter = this.currentFilter.next();
		updateEntityList();
		updateInventory();
	}

	@ItemExecutor("Creation-date-sort-button")
	@ItemExecutor("Name-sort-button")
	@ItemExecutor("World-sort-button")
	private void onClickSortButton() {
		this.currentSort = this.currentSort.next();
		updateEntityList();
		updateInventory();
	}

	@ItemExecutor("Invitations-button")
	private void onClickInvitationsButton() {
		new ProtectionInvitationsInventory(getPlayer(), playerDataService.getPlayerData(getPlayer())).openInventory();
	}

	@ItemExecutor("Close-button")
	private void onClickCloseButton() {
		closeInventory();
	}

}
