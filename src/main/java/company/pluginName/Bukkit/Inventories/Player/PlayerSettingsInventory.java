package company.pluginName.Bukkit.Inventories.Player;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import company.pluginName.Modules.PlayersDataPckg.Objects.PlayerData;
import company.pluginName.Modules.SQLPckg.SQLService;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Utils.TasksUtils;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.PlayerHeadCacheService;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.ChestInventoryObject;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.GeneratedItem;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Utilities.Java.Arrays.ArrayUtilities;

@Inventory("player_settings")
public class PlayerSettingsInventory extends ChestInventoryObject {

	@PandaInject
	private static PlaceholdersService placeholdersService;

	@PandaInject
	private static PlayerHeadCacheService playerHeadCacheService;

	@PandaInject
	private static SQLService sqlService;

	private static final String MESSAGES_INVITATIONREQUIREMENT_ALLACCEPTED_PATH = "Messages.Invitation-requirement.All-accepted";
	private static final String MESSAGES_INVITATIONREQUIREMENT_ALLDENIED_PATH = "Messages.Invitation-requirement.All-denied";
	private static final String MESSAGES_INVITATIONREQUIREMENT_INVITATIONREQUIRED_PATH = "Messages.Invitation-requirement.Invitation-required";

	private static final String MESSAGES_COMMON_ENABLED_PATH = "Messages.Common.Enabled";
	private static final String MESSAGES_COMMON_DISABLED_PATH = "Messages.Common.Disabled";

	private PlayerData playerData;

	private boolean modified = false;

	public PlayerSettingsInventory(Player player, PlayerData playerData) {
		super(player);

		this.playerData = playerData;

		Replacement[] playerReplacements = placeholdersService.getPlayerReplacements(player);
		Replacement[] customReplacements = new Replacement[] { new Replacement("{invitationrequirement_value}", () -> {
			switch (playerData.getInvitationRequirement()) {
			case ALL_ACCEPTED:
				return getChestInventoryData().getCustomFields().get(MESSAGES_INVITATIONREQUIREMENT_ALLACCEPTED_PATH)
						.toString();
			case ALL_DENIED:
				return getChestInventoryData().getCustomFields().get(MESSAGES_INVITATIONREQUIREMENT_ALLDENIED_PATH)
						.toString();
			case INVITATION_REQUIRED:
				return getChestInventoryData().getCustomFields()
						.get(MESSAGES_INVITATIONREQUIREMENT_INVITATIONREQUIRED_PATH).toString();
			default:
				break;
			}
			return "";
		}).cacheText(false),
				new Replacement("{autoflight_value}", () -> getChestInventoryData().getCustomFields()
						.get(playerData.isAutoFlight() ? MESSAGES_COMMON_ENABLED_PATH : MESSAGES_COMMON_DISABLED_PATH)
						.toString()).cacheText(false) };

		setReplacements(ArrayUtilities.join(new Replacement[playerReplacements.length + customReplacements.length],
				playerReplacements, customReplacements));
		setTitleReplacements(getReplacements());
	}

	@ItemGenerator("Player-info")
	private ItemStack generateToggleBoundaryButton(Item item) {
		ItemBuilder builder = ItemBuilder.inst().fromItem(item.getItems().get(Item.DISPLAYITEM_KEY));

		if (builder.getMaterial().name().equals("PLAYER_HEAD")) {
			return playerHeadCacheService.processPlayerHead(builder, getPlayer().getUniqueId()).getFirst();
		}

		return builder.build();
	}

	@ItemExecutor("Auto-flight-button")
	private void onClickAutoFlightButton() {
		this.playerData.setAutoFlight(!this.playerData.isAutoFlight());
		modified = true;
		updateInventory();
	}

	@ItemExecutor("Invitation-requirement-button")
	private void onClickInvitationRequirementButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		this.playerData.setInvitationRequirement(e.isLeftClick() ? this.playerData.getInvitationRequirement().next()
				: this.playerData.getInvitationRequirement().previous());
		modified = true;
		updateInventory();
	}

	@ItemExecutor("Close-button")
	private void onClickCloseButton() {
		closeInventory();
	}

	@Override
	public void onClose(InventoryCloseEvent e) {
		if (modified) {
			TasksUtils.executeOnAsync(() -> {
				try {
					sqlService.savePlayerData(playerData);
				} catch (RoyaleProtectionBlocksExceptionImpl e1) {
					e1.sendError(getPlayer());
				}
			});
		}
	}

}
