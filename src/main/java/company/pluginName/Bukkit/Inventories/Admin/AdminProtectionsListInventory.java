package company.pluginName.Bukkit.Inventories.Admin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.API.RoyaleProtectionBlocksAPIImpl;
import company.pluginName.Bukkit.Inventories.Admin.AdminProtectionsListInventory.Filters.Direction;
import company.pluginName.Bukkit.Inventories.Admin.AdminProtectionsListInventory.Filters.SortedBy;
import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionDisplayNames;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionUtilities;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.Objects.PandaCachedPlayer;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.PlayerHeadCacheService;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.GeneratedItem;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import darkpanda73.PandaUtils.Utilities.Java.Arrays.ArrayUtilities;
import darkpanda73.PandaUtils.Utilities.Java.Objects.Pair;
import lombok.AllArgsConstructor;
import lombok.Data;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException.Type;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.ProtectionBlocks.IProtectionBlock;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Inventories.OpenProtectionManagementInventoryRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Inventories.OpenProtectionRemovalInventoryRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionTeleportHomeRequestInput;

@Inventory("admin_protections_list")
public class AdminProtectionsListInventory extends PagedChestInventoryObject<Protection> {

	public static final String TITLE_OTHER_PATH = "Title-other";
	public static final String ENTITY_TELEPORTLORELINE_PATH = "Entity.Teleport-lore-line";
	public static final String ENTITY_TELEPORTNOTSETLORELINE_PATH = "Entity.Teleport-not-set-lore-line";
	public static final String ENTITY_EDITLORELINE_PATH = "Entity.Edit-lore-line";
	public static final String ENTITY_DELETELORELINE_PATH = "Entity.Delete-lore-line";

	@PandaInject
	private static PlaceholdersService placeholdersService;

	@PandaInject
	private static PlayerHeadCacheService playerHeadCacheService;

	private Filters filters;

	private Replacement totalAmountReplacement = new Replacement("{total_amount}",
			() -> String.valueOf(this.getCurrentEntityList().size())).cacheText(false);

	public AdminProtectionsListInventory(Player player) {
		this(player, null);
	}

	public AdminProtectionsListInventory(Player player, PandaCachedPlayer owner) {
		super(player);
		this.filters = new Filters(player);
		this.filters.setFilteredPlayer(owner);
		this.filters.getSorts().add(Pair.of(SortedBy.CREATED_DATE, Direction.DESCENDANT));

		setReplacements(new Replacement[] { totalAmountReplacement });
		setTitleReplacements(getReplacements());
	}

	@Override
	protected List<Protection> getEntityList() {
		return (this.filters.filteredPlayer != null
				? RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService()
						.findAllowedParentProtectionsByCachedPlayer(this.filters.filteredPlayer)
				: RoyaleProtectionBlocksAPIImpl.getInstance().getProtectionsService().findAllProtections()).filter(
						prot -> this.filters.filteredWorld != null
								? prot.getWorldName().equals(this.filters.filteredWorld.getName())
								: true)
				.filter(prot -> this.filters.filteredRadius > 0
						? prot.getWorldName().equals(getPlayer().getWorld().getName()) && prot.getBukkitLocation()
								.distance(getPlayer().getLocation()) <= this.filters.filteredRadius
						: true)
				.filter(prot -> this.filters.filteredProtectionBlock != null
						? prot.getProtectionBlockId().equals(this.filters.filteredProtectionBlock.getId())
						: true)
				.sorted((prot1, prot2) -> {
					int value = 0;
					for (int i = 0; i < filters.sorts.size() && value == 0; i++) {
						value = filters.sorts.get(i).getFirst().sort(filters, prot1, prot2,
								filters.sorts.get(i).getSecond());
					}
					return value;
				}).collect(Collectors.toList());

	}

	@Override
	protected ItemStack generateEntityItem(Protection protection) {
		final boolean canTeleport = protection.canTeleport(getPlayer());
		final boolean canManage = ProtectionUtilities.canManage(protection, getPlayer());
		final boolean canDelete = ProtectionUtilities.canDelete(protection, getPlayer());

		Location homeLocation = protection.getHome();
		Location loc = protection.getBukkitLocation();
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

		if ((homeLocation != null && canTeleport) || protection.isMainOwner(getPlayer().getUniqueId())) {
			lore.add(homeLocation != null
					? getChestInventoryData().getCustomFields().get(ENTITY_TELEPORTLORELINE_PATH).toString()
					: getChestInventoryData().getCustomFields().get(ENTITY_TELEPORTNOTSETLORELINE_PATH).toString());
		}

		if (canManage) {
			lore.add(getChestInventoryData().getCustomFields().get(ENTITY_EDITLORELINE_PATH).toString());
		}

		if (canDelete) {
			lore.add(getChestInventoryData().getCustomFields().get(ENTITY_DELETELORELINE_PATH).toString());
		}

		return itemBuilder.setLore(lore).apply(protection.getDisplayItemOrDefault());
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, Protection protection) {
		Location homeLocation = protection.getHome();

		if (e.getClick() == ClickType.LEFT) {
			if (homeLocation != null && protection.canTeleport(getPlayer())) {
				try {
					RoyaleProtectionBlocksAPIImpl.getInstance().getPlayerInteractionsService()
							.protectionTeleportHomeRequest(
									ProtectionTeleportHomeRequestInput.inst(getPlayer(), protection));
				} catch (RoyaleProtectionBlocksException e1) {
					e1.sendError(getPlayer());
				}
			}
		} else if (e.getClick() == ClickType.RIGHT) {
			try {
				RoyaleProtectionBlocksAPIImpl.getInstance().getPlayerInteractionsService()
						.openProtectionManagementInventoryRequest(
								OpenProtectionManagementInventoryRequestInput.inst(getPlayer(), protection));
			} catch (RoyaleProtectionBlocksExceptionImpl ex) {
				if (ex.getType() == Type.PROTECTIONS_BLOCKED) {
					ex.sendError(getPlayer());
				}
			}
		} else if (e.getClick() == ClickType.SHIFT_RIGHT) {
			try {
				RoyaleProtectionBlocksAPIImpl.getInstance().getPlayerInteractionsService()
						.openProtectionRemovalInventoryRequest(
								OpenProtectionRemovalInventoryRequestInput.inst(getPlayer(), protection));
			} catch (RoyaleProtectionBlocksExceptionImpl ex) {
			}
		}

	}

	@ItemExecutor("Filters-button")
	private void onClickFiltersButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		new AdminProtectionsListFiltersInventory(getPlayer(), filters).openInventory();
	}

	@ItemExecutor("Refresh-button")
	private void onClickRefreshButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		updateEntityList();
		updateInventory();
	}

	@ItemExecutor("Close-button")
	private void onClickCloseButton() {
		closeInventory();
	}

	@Data
	public static final class Filters {

		@AllArgsConstructor
		public static enum SortedBy {
			CREATED_DATE((filters, protection1, protection2) -> Long.compare(protection1.getCreatedDate(),
					protection2.getCreatedDate())),
			NAME((filters, protection1, protection2) -> protection1.getDisplayName()
					.compareTo(protection2.getDisplayName())),
			OWNER((filters, protection1, protection2) -> protection1.getOwnerName()
					.compareTo(protection2.getOwnerName())),
			WORLD((filters, protection1, protection2) -> protection1.getWorldName()
					.compareTo(protection2.getWorldName())),
			DISTANCE((filters, protection1, protection2) -> {
				if (!protection1.getWorldName().equals(filters.getPlayer().getWorld().getName())) {
					return protection2.getWorldName().equals(filters.getPlayer().getWorld().getName()) ? -1 : 1;
				} else {
					return protection2.getWorldName().equals(filters.getPlayer().getWorld().getName()) ? Double.compare(
							protection1.getBukkitLocation().distance(filters.getPlayer().getLocation()),
							protection1.getBukkitLocation().distance(filters.getPlayer().getLocation())) : 1;
				}
			}), PROTECTION_BLOCK((filters, protection1, protection2) -> protection1.getProtectionBlockId()
					.compareTo(protection2.getProtectionBlockId()));

			private TriFunction<Filters, Protection, Protection, Integer> sortFunction;

			public int sort(Filters filters, Protection prot1, Protection prot2, Direction direction) {
				return direction == Direction.ASCENDANT ? sortFunction.accept(filters, prot1, prot2)
						: sortFunction.accept(filters, prot2, prot1);
			}
		}

		public static enum Direction {
			ASCENDANT, DESCENDANT;

			public Direction previous() {
				return values()[(values().length + ordinal() - 1) % values().length];
			}

			public Direction next() {
				return values()[(values().length + ordinal() + 1) % values().length];
			}
		}

		public Filters(Player player) {
			this.player = player;
		}

		private Player player;
		private PandaCachedPlayer filteredPlayer = null;
		private World filteredWorld = null;
		private int filteredRadius = -1;
		private IProtectionBlock filteredProtectionBlock = null;
		private List<Pair<SortedBy, Direction>> sorts = new ArrayList<>();

	}

	private static interface TriFunction<T, U, S, R> {

		public R accept(T arg1, U arg2, S arg3);

	}

}
