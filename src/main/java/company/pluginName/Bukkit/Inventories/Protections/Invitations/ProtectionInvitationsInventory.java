package company.pluginName.Bukkit.Inventories.Protections.Invitations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Bukkit.Inventories.Shared.ConfirmationInventory;
import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import company.pluginName.Modules.PlayersDataPckg.PlayerDataService;
import company.pluginName.Modules.PlayersDataPckg.Objects.PlayerData;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.PandaCachedPlayersService;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Annotation.RegisteredPandaField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.GeneratedItem;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryData;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Paged.PagedChestInventoryObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.PlayerInteractionsService;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.Members.ProtectionMemberAcceptInvitationRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.Members.ProtectionMemberRemoveInvitationRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.Objects.ProtectionInvitation;

@Inventory("protections_invitations")
public class ProtectionInvitationsInventory extends PagedChestInventoryObject<ProtectionInvitation> {

	@RegisteredPandaField("lang")
	public static final PandaPrefixedStringField MESSAGE_PROTECTIONS_INVITATIONS_ACCEPTEDSUCCESSFULLY = new PandaPrefixedStringField(
			"Message.Protections.Invitations.Accepted-successfully",
			"&aAceptaste la invitación correctamente.");

	@RegisteredPandaField("lang")
	public static final PandaPrefixedStringField MESSAGE_PROTECTIONS_INVITATIONS_REJECTEDSUCCESSFULLY = new PandaPrefixedStringField(
			"Message.Protections.Invitations.Rejected-successfully",
			"&aRechazaste la invitación correctamente.");

	@RegisteredPandaField("lang")
	public static final PandaPrefixedStringField MESSAGE_PROTECTIONS_INVITATIONS_ALLREJECTEDSUCCESSFULLY = new PandaPrefixedStringField(
			"Message.Protections.Invitations.All-rejected-successfully",
			"&aRechazaste todas las invitaciones correctamente.");

	private static final String ENTITY_LEFTCLICKLORELINE_PATH = "Entity.Left-click-lore-line";
	private static final String ENTITY_RIGHTCLICKLORELINE_PATH = "Entity.Right-click-lore-line";
	private static final String ENTITY_ACCEPTACTIONLINE_PATH = "Entity.Accept-action-lore-line";
	private static final String ENTITY_REJECTACTIONLINE_PATH = "Entity.Reject-action-lore-line";

	@AllArgsConstructor
	@Getter
	private static enum OptionSet {
		PRIMARY_OPTION_SET((event) -> {
			new ConfirmationInventory(event.getInventory().getPlayer(), () -> {
				try {
					playerInteractionsService
							.protectionMemberAcceptInvitationRequest(ProtectionMemberAcceptInvitationRequestInput
									.inst(event.getInventory().getPlayer(), event.getEntity().getProtection()));
					MessageTemplate.inst(MESSAGE_PROTECTIONS_INVITATIONS_ACCEPTEDSUCCESSFULLY.applyPrefix()).process()
							.sendMessage(event.getInventory().getPlayer());
				} catch (RoyaleProtectionBlocksException e) {
					e.sendError(event.getInventory().getPlayer());
				}
				event.getInventory().updateInventory();
			}).openInventory();
		}), SECONDARY_OPTION_SET((event) -> {
			new ConfirmationInventory(event.getInventory().getPlayer(), () -> {
				try {
					playerInteractionsService.protectionMemberRemoveInvitationRequest(
							ProtectionMemberRemoveInvitationRequestInput.inst(event.getInventory().getPlayer(),
									event.getEntity().getProtection(), event.getInventory().getPlayer().getUniqueId()));
					MessageTemplate.inst(MESSAGE_PROTECTIONS_INVITATIONS_REJECTEDSUCCESSFULLY.applyPrefix()).process()
							.sendMessage(event.getInventory().getPlayer());
				} catch (RoyaleProtectionBlocksException e) {
					event.getInventory().playerData.getProtectionInvitations().remove(event.getEntity());
				}
				event.getInventory().updateInventory();
			}).openInventory();
		});

		@Data
		@AllArgsConstructor
		public static class OptionClickEvent {

			private ProtectionInvitationsInventory inventory;
			private InventoryClickEvent event;
			private ProtectionInvitation entity;

		}

		private Consumer<OptionClickEvent> function;

		public OptionSet previous() {
			return values()[(values().length + ordinal() - 1) % values().length];
		}

		public OptionSet next() {
			return values()[(values().length + ordinal() + 1) % values().length];
		}
	}

	@PandaInject
	private static PlayerInteractionsService playerInteractionsService;

	@PandaInject
	private static PlayerDataService playerDataService;

	@PandaInject
	private static PandaCachedPlayersService cachedPlayersService;

	@PandaInject
	private static PlaceholdersService placeholdersService;

	private PlayerData playerData;

	private OptionSet leftClickOptionSet = OptionSet.PRIMARY_OPTION_SET;
	private OptionSet rightClickOptionSet = OptionSet.SECONDARY_OPTION_SET;

	public ProtectionInvitationsInventory(Player player, PlayerData playerData) {
		super(player);

		this.playerData = playerData;
	}

	@Override
	protected List<ProtectionInvitation> getEntityList() {
		return playerData.getProtectionInvitations();
	}

	@Override
	protected ItemStack generateEntityItem(ProtectionInvitation entity) {
		ItemBuilder builder = ItemBuilder.inst().fromItem(entity.getProtection().getDisplayItemOrDefault())
				.fromMap(getChestInventoryData().getCustomFields(), PagedChestInventoryData.ENTITY_PATH);

		List<Replacement> replacements = new ArrayList<>();
		replacements.addAll(Arrays.asList(placeholdersService.getProtectionReplacements(entity.getProtection())));

		List<String> extraLore = new ArrayList<>();

		String leftClickAction = this.leftClickOptionSet == OptionSet.PRIMARY_OPTION_SET
				? getChestInventoryData().getCustomFields().get(ENTITY_ACCEPTACTIONLINE_PATH).toString()
				: getChestInventoryData().getCustomFields().get(ENTITY_REJECTACTIONLINE_PATH).toString();

		extraLore.add(getChestInventoryData().getCustomFields().get(ENTITY_LEFTCLICKLORELINE_PATH).toString());
		replacements.add(new Replacement("{left_click_action}", () -> leftClickAction));

		String rightClickAction = this.rightClickOptionSet == OptionSet.PRIMARY_OPTION_SET
				? getChestInventoryData().getCustomFields().get(ENTITY_ACCEPTACTIONLINE_PATH).toString()
				: getChestInventoryData().getCustomFields().get(ENTITY_REJECTACTIONLINE_PATH).toString();
		extraLore.add(getChestInventoryData().getCustomFields().get(ENTITY_RIGHTCLICKLORELINE_PATH).toString());
		replacements.add(new Replacement("{right_click_action}", () -> rightClickAction));

		extraLore.add(0, "&0");
		builder.getLore().addAll(extraLore);

		return builder.setReplacements(replacements.toArray(new Replacement[replacements.size()])).build();
	}

	@Override
	protected void onEntityClick(InventoryClickEvent e, ProtectionInvitation entity) {
		if (e.isLeftClick()) {
			this.leftClickOptionSet.function.accept(new OptionSet.OptionClickEvent(this, e, entity));
		} else if (e.isRightClick()) {
			this.rightClickOptionSet.function.accept(new OptionSet.OptionClickEvent(this, e, entity));
		}
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

	@ItemExecutor("Clear-invitations-button")
	private void executeClearInvitationsButton() {
		new ConfirmationInventory(getPlayer(), () -> {
			new ArrayList<>(this.playerData.getProtectionInvitations()).forEach(invitation -> {
				try {
					playerInteractionsService
							.protectionMemberRemoveInvitationRequest(ProtectionMemberRemoveInvitationRequestInput
									.inst(getPlayer(), invitation.getProtection(), getPlayer().getUniqueId()));
				} catch (RoyaleProtectionBlocksException e) {
					this.playerData.getProtectionInvitations().remove(invitation);
				}
			});
			MessageTemplate.inst(MESSAGE_PROTECTIONS_INVITATIONS_ALLREJECTEDSUCCESSFULLY.applyPrefix()).process()
					.sendMessage(getPlayer());
			updateInventory();
		}).openInventory();
	}

	@ItemExecutor("Alternate-options-button")
	private void executeAlternateOptionsButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		if (e.isLeftClick()) {
			this.leftClickOptionSet = this.leftClickOptionSet.next();
			this.rightClickOptionSet = this.rightClickOptionSet.next();
			this.updateInventory();
		} else if (e.isRightClick()) {
			this.leftClickOptionSet = this.leftClickOptionSet.previous();
			this.rightClickOptionSet = this.rightClickOptionSet.previous();
			this.updateInventory();
		}
	}

}
