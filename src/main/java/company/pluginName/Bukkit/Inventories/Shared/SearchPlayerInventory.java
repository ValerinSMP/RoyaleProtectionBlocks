package company.pluginName.Bukkit.Inventories.Shared;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.PandaCachedPlayersService;
import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.Objects.PandaCachedPlayer;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Exceptions.PlayerAlreadyListeningException;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Services.PandaMessageListenerService;

@Inventory("searchplayer")
public class SearchPlayerInventory extends PagedChestInventoryObject<PandaCachedPlayer> {

	private static final String MESSAGE_PLAYERSEARCHINFO_PATH = "Messages.Player-search-info";
	private static final String MESSAGE_ALREADYINPROMPT_PATH = "Messages.Already-in-prompt";
	private static final String MESSAGE_PLAYERNOTFOUND_PATH = "Messages.Player-not-found";

	@PandaInject
	private static PandaMessageListenerService messageListenerService;

	@PandaInject
	private static PandaCachedPlayersService cachedPlayersService;

	private Consumer<PandaCachedPlayer> action;
	private Function<PandaCachedPlayer, Boolean> filterFunction;

	public SearchPlayerInventory(Player player, Consumer<PandaCachedPlayer> action) {
		super(player);
		this.action = action;
	}

	public SearchPlayerInventory(Player player, Consumer<PandaCachedPlayer> action,
			Function<PandaCachedPlayer, Boolean> filterFunction) {
		super(player);
		this.action = action;
		this.filterFunction = filterFunction;
	}

	@Override
	protected List<? extends PandaCachedPlayer> getEntityList() {
		return Bukkit.getOnlinePlayers().stream().filter(p -> getPlayer().canSee(p))
				.map(p -> cachedPlayersService.getCachedPlayer(p))
				.filter(p -> p != null && (filterFunction == null || filterFunction.apply(p)))
				.collect(Collectors.toList());
	}

	@Override
	protected ItemStack generateEntityItem(PandaCachedPlayer entity) {
		String name = getChestInventoryData().getEntityName() != null
				&& !getChestInventoryData().getEntityName().isEmpty() ? getChestInventoryData().getEntityName() : null;

		return processPlayerHead(ItemBuilder.inst().fromItem(new ItemStack(Material.PLAYER_HEAD)).setName(name)
				.setReplacements(new Replacement("{playername}", () -> entity.getName())), entity.getUuid());
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, PandaCachedPlayer entity) {
		closeInventory();
		action.accept(entity);
	}

	@ItemExecutor("Back-button")
	private void onClickBack() {
		goToPreviousInventory();
	}

	@ItemGenerator("Back-button")
	private ItemStack generateBackButton(Item item) {
		return getPreviousInventory() != null ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemExecutor("Close-button")
	private void onClickClose() {
		closeInventory();
	}

	@ItemExecutor("Search-specific-player")
	private void onClickSearchSpecificPlayer() {
		try {
			messageListenerService.getListener().startListening(getPlayer().getUniqueId(), (message) -> {
				if (!message.equalsIgnoreCase("cancel")) {
					PandaCachedPlayer cachedPlayer = cachedPlayersService.getCachedPlayer(message);
					if (cachedPlayer == null) {
						MessageTemplate
								.inst(PandaPrefixedStringField.applyPrefix(getChestInventoryData().getCustomFields()
										.get(MESSAGE_PLAYERNOTFOUND_PATH).toString()))
								.process().sendMessage(getPlayer());
					}
					action.accept(cachedPlayer);
				} else {
					openInventory();
				}
				return true;
			});
			closeInventory();
			MessageTemplate
					.inst(PandaPrefixedStringField.applyPrefix(
							getChestInventoryData().getCustomFields().get(MESSAGE_PLAYERSEARCHINFO_PATH).toString()))
					.process().sendMessage(getPlayer());
		} catch (PlayerAlreadyListeningException ex) {
			MessageTemplate
					.inst(PandaPrefixedStringField.applyPrefix(
							getChestInventoryData().getCustomFields().get(MESSAGE_ALREADYINPROMPT_PATH).toString()))
					.process().sendMessage(getPlayer());
		}
	}

}
