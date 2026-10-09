package company.pluginName.Bukkit.Inventories.Protections.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import company.pluginName.Modules.PlaceholdersPckg.PlaceholdersService;
import company.pluginName.Modules.ProtectionSettingsPckg.ProtectionSettingsService;
import company.pluginName.Modules.ProtectionSettingsPckg.Objects.Settings.GreetingFarewellMessagesSetting.ConfiguredMessages;
import company.pluginName.Modules.ProtectionSettingsPckg.Objects.Settings.GreetingFarewellMessagesSetting.MessageType;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Message;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageFragment.HoverEvent;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.Replacement;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaPrefixedStringField;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.Inventory;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemExecutor;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Annotations.ItemGenerator;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.ChestInventoryObject;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.GeneratedItem;
import darkpanda73.PandaUtils.Services.PandaInventoriesModule.v1.Objects.ChestInventory.Item;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Listeners.PandaMessageListener.Callback;
import darkpanda73.PandaUtils.Services.PandaMessageListenerModule.Services.PandaMessageListenerService;
import darkpanda73.PandaUtils.Utilities.Java.Collections.RotativeArray;
import lombok.Data;
import royale.RoyaleProtectionBlocks.Plugin.API.RoyaleProtectionBlocksAPI;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.PermissionGroup;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.Objects.Protections.ProtectionSwitchSettingRequestInput;

@Inventory("protections_settings_greeting_farewell_messages")
public class ProtectionSettingsGreetingFarewellMessagesInventory extends ChestInventoryObject {

	@Data
	private static class ConfiguredMessagesEntry {

		private ConfiguredMessages object;
		private boolean modified;

		public ConfiguredMessagesEntry(ConfiguredMessages object) {
			this.object = object;
		}

	}

	public static final String MESSAGES_MESSAGESPECIFYINFO_PATH = "Messages.Message-specify-info";
	public static final String MESSAGES_ACTIONBARSPECIFYINFO_PATH = "Messages.Action-bar-specify-info";
	public static final String MESSAGES_TITLESPECIFYINFO_PATH = "Messages.Title-specify-info";
	public static final String MESSAGES_SUBTITLESPECIFYINFO_PATH = "Messages.Subtitle-specify-info";
	public static final String MESSAGES_BOSSBARSPECIFYINFO_PATH = "Messages.Boss-bar-specify-info";
	public static final String MESSAGES_PLACEHOLDERSTOOLTIP_PATH = "Messages.Placeholders-tooltip";
	public static final String MESSAGES_EXCEEDEDMAXIMUMCHARACTERS_PATH = "Messages.Exceeded-maximum-characters";

	@PandaInject
	private static PandaMessageListenerService messageListenerService;

	private IProtection protection;

	private RotativeArray<ConfiguredMessagesEntry> groups;
	private RotativeArray<MessageType> messageTypes;

	private Replacement placeholdersReplacement;

	public ProtectionSettingsGreetingFarewellMessagesInventory(Player player, IProtection protection) {
		super(player);

		this.protection = protection;

		List<ConfiguredMessagesEntry> editableGroups = ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING
				.getEditableGroups().stream()
				.map(group -> new ConfiguredMessagesEntry(this.protection
						.getSettingValue(ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING, group)
						.clone(group)))
				.collect(Collectors.toList());
		List<MessageType> availableMessageTypes = ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING
				.getAvailableMessageTypes();

		this.groups = new RotativeArray<>(editableGroups.toArray(new ConfiguredMessagesEntry[editableGroups.size()]));
		this.messageTypes = new RotativeArray<>(
				availableMessageTypes.toArray(new MessageType[availableMessageTypes.size()]));

		this.messageTypes.setIndex(this.messageTypes.findIndex(this.groups.get().getObject().getMessageType()));

		List<String> placeholders = new ArrayList<>();
		PlaceholdersService.PLAYER_PLACEHOLDERS.forEach(pair -> placeholders.add("&7" + pair.getFirst()));
		PlaceholdersService.PROTECTION_PLACEHOLDERS.forEach(pair -> placeholders.add("&7" + pair.getFirst()));

		Message placeholdersMessage = MessageTemplate.inst(placeholders.stream().collect(Collectors.joining("&e, ")))
				.process();

		placeholdersReplacement = new Replacement("{placeholders_tooltip}",
				() -> getChestInventoryData().getCustomFields().get(MESSAGES_PLACEHOLDERSTOOLTIP_PATH).toString(),
				new HoverEvent(HoverEvent.Action.SHOW_TEXT, placeholdersMessage.asComponentsArray()));
	}

	@ItemGenerator("Message-type-button")
	private ItemStack onGenerateMessageTypeButton(Item item) {
		return messageTypes.get() == MessageType.MESSAGE ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Action-bar-type-button")
	private ItemStack onGenerateActionBarTypeButton(Item item) {
		return messageTypes.get() == MessageType.ACTION_BAR ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Title-type-button")
	private ItemStack onGenerateTitleTypeButton(Item item) {
		return messageTypes.get() == MessageType.TITLE ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Boss-bar-type-button")
	private ItemStack onGenerateBossBarTypeButton(Item item) {
		return messageTypes.get() == MessageType.BOSS_BAR ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemGenerator("Non-members-group-button")
	private ItemStack onGenerateNonMembersGroupButton(Item item) {
		return groups.get().getObject().getPermissionGroup() == PermissionGroup.NON_MEMBERS
				? item.getItems().get(Item.DISPLAYITEM_KEY)
				: null;
	}

	@ItemGenerator("Members-group-button")
	private ItemStack onGenerateMembersGroupButton(Item item) {
		return groups.get().getObject().getPermissionGroup() == PermissionGroup.MEMBERS
				? item.getItems().get(Item.DISPLAYITEM_KEY)
				: null;
	}

	@ItemGenerator("Owners-group-button")
	private ItemStack onGenerateOwnersGroupButton(Item item) {
		return groups.get().getObject().getPermissionGroup() == PermissionGroup.OWNERS
				? item.getItems().get(Item.DISPLAYITEM_KEY)
				: null;
	}

	@ItemGenerator("Main-owner-group-button")
	private ItemStack onGenerateMainOwnerGroupButton(Item item) {
		return groups.get().getObject().getPermissionGroup() == PermissionGroup.MAIN_OWNER ? processPlayerHead(
				ItemBuilder.inst().fromItem(item.getItems().get(Item.DISPLAYITEM_KEY)), this.protection.getOwnerUuid())
				: null;
	}

	@ItemGenerator("Greeting-message-button")
	private ItemStack onGenerateGreetingMessageButton(Item item) {
		ItemBuilder builder = ItemBuilder.inst().fromItem(item.getItems().get(Item.DISPLAYITEM_KEY));

		builder.getLore().addAll(MessageTemplate.inst(this.groups.get().getObject().getGreetingMessage().stream()
				.map(line -> "&f" + line).collect(Collectors.toList())).process().getStrings());

		return builder.build();
	}

	@ItemGenerator("Farewell-message-button")
	private ItemStack onGenerateFarewellMessageButton(Item item) {
		ItemBuilder builder = ItemBuilder.inst().fromItem(item.getItems().get(Item.DISPLAYITEM_KEY));

		builder.getLore().addAll(MessageTemplate.inst(this.groups.get().getObject().getFarewellMessage().stream()
				.map(line -> "&f" + line).collect(Collectors.toList())).process().getStrings());

		return builder.build();
	}

	@ItemExecutor("Non-members-group-button")
	@ItemExecutor("Members-group-button")
	@ItemExecutor("Owners-group-button")
	@ItemExecutor("Main-owner-group-button")
	private void onClickGroupButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		if (e.isLeftClick()) {
			groups.next();
		} else {
			groups.previous();
		}

		messageTypes.setIndex(messageTypes.findIndex(groups.get().getObject().getMessageType()));
		updateInventory();
	}

	@ItemExecutor("Message-type-button")
	@ItemExecutor("Action-bar-type-button")
	@ItemExecutor("Title-type-button")
	@ItemExecutor("Boss-bar-type-button")
	private void onClickMessageTypeButton(Item item, GeneratedItem generatedItem, InventoryClickEvent e) {
		if (e.isLeftClick()) {
			messageTypes.next();
		} else {
			messageTypes.previous();
		}

		groups.get().getObject().setMessageType(messageTypes.get());
		groups.get().setModified(true);
		updateInventory();
	}

	@ItemExecutor("Greeting-message-button")
	private void onClickGreetingMessageButton() {
		switch (messageTypes.get()) {
		case MESSAGE:
			onMessageSpecify(groups.get().getObject().getGreetingMessage());
			break;
		case ACTION_BAR:
			onActionBarSpecify(groups.get().getObject().getGreetingMessage());
			break;
		case TITLE:
			onTitleSpecify(groups.get().getObject().getGreetingMessage());
			break;
		case BOSS_BAR:
			onBossBarSpecify(groups.get().getObject().getGreetingMessage());
			break;
		}
	}

	@ItemExecutor("Farewell-message-button")
	private void onClickFarewellMessageButton() {
		switch (messageTypes.get()) {
		case MESSAGE:
			onMessageSpecify(groups.get().getObject().getFarewellMessage());
			break;
		case ACTION_BAR:
			onActionBarSpecify(groups.get().getObject().getFarewellMessage());
			break;
		case TITLE:
			onTitleSpecify(groups.get().getObject().getFarewellMessage());
			break;
		case BOSS_BAR:
			onBossBarSpecify(groups.get().getObject().getFarewellMessage());
			break;
		}
	}

	@ItemExecutor("Back-button")
	private void onClickBackButton() {
		goToPreviousInventory();
	}

	@ItemGenerator("Back-button")
	private ItemStack generateBackButton(Item item) {
		return getPreviousInventory() != null ? item.getItems().get(Item.DISPLAYITEM_KEY) : null;
	}

	@ItemExecutor("Close-button")
	private void onClickCloseButton() {
		closeInventory();
	}

	private void onMessageSpecify(List<String> list) {
		List<String> messageList = new ArrayList<>();

		messageListenerService.getListener().replaceListening(getPlayer().getUniqueId(), new Callback() {

			@Override
			public boolean message(String message) {
				if (message.startsWith(".")) {
					list.clear();
					list.addAll(messageList);
					groups.get().setModified(true);
					return true;
				}

				int length = messageList.stream().map(line -> line.length()).reduce(0, Integer::sum) + message.length();

				if (length > ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING
						.getMessageMaximumLengthInChars()) {
					MessageTemplate
							.inst(PandaPrefixedStringField.applyPrefix(getChestInventoryData().getCustomFields()
									.get(MESSAGES_EXCEEDEDMAXIMUMCHARACTERS_PATH).toString()))
							.setReplacements(new Replacement("{current_characters}", () -> String.valueOf(length)),
									new Replacement("{maximum_characters}",
											() -> String.valueOf(
													ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING
															.getMessageMaximumLengthInChars())))
							.process().sendMessage(getPlayer());
				} else {
					messageList.add(message);
					MessageTemplate.inst(message).process().sendMessage(getPlayer());
				}

				return false;
			}

			public void cancel() {
				openInventory();
			}

		});
		closeInventory();
		MessageTemplate
				.inst(PandaPrefixedStringField.applyPrefix(
						getChestInventoryData().getCustomFields().get(MESSAGES_MESSAGESPECIFYINFO_PATH).toString()))
				.setReplacements(placeholdersReplacement).process().sendMessage(getPlayer());
	}

	private void onActionBarSpecify(List<String> list) {
		messageListenerService.getListener().replaceListening(getPlayer().getUniqueId(), new Callback() {

			@Override
			public boolean message(String message) {
				if (message.length() > ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING
						.getActionBarMaximumLengthInChars()) {
					MessageTemplate
							.inst(PandaPrefixedStringField.applyPrefix(getChestInventoryData().getCustomFields()
									.get(MESSAGES_EXCEEDEDMAXIMUMCHARACTERS_PATH).toString()))
							.setReplacements(
									new Replacement("{current_characters}", () -> String.valueOf(message.length())),
									new Replacement("{maximum_characters}",
											() -> String.valueOf(
													ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING
															.getActionBarMaximumLengthInChars())))
							.process().sendMessage(getPlayer());
					return false;
				}

				list.clear();
				list.add(message);
				groups.get().setModified(true);

				return true;
			}

			public void cancel() {
				openInventory();
			}

		});
		closeInventory();
		MessageTemplate
				.inst(PandaPrefixedStringField.applyPrefix(
						getChestInventoryData().getCustomFields().get(MESSAGES_ACTIONBARSPECIFYINFO_PATH).toString()))
				.setReplacements(placeholdersReplacement).process().sendMessage(getPlayer());
	}

	private void onTitleSpecify(List<String> list) {
		List<String> messageList = new ArrayList<>();

		messageListenerService.getListener().replaceListening(getPlayer().getUniqueId(), new Callback() {

			@Override
			public boolean message(String message) {
				int maximumCharacters = messageList.isEmpty()
						? ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING.getTitleMaximumLengthInChars()
						: ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING
								.getSubtitleMaximumLengthInChars();

				if (message.length() > maximumCharacters) {
					MessageTemplate
							.inst(PandaPrefixedStringField.applyPrefix(getChestInventoryData().getCustomFields()
									.get(MESSAGES_EXCEEDEDMAXIMUMCHARACTERS_PATH).toString()))
							.setReplacements(
									new Replacement("{current_characters}", () -> String.valueOf(message.length())),
									new Replacement("{maximum_characters}", () -> String.valueOf(maximumCharacters)))
							.process().sendMessage(getPlayer());
					return false;
				}

				messageList.add(message);

				if (messageList.size() == 1) {
					MessageTemplate
							.inst(PandaPrefixedStringField.applyPrefix(getChestInventoryData().getCustomFields()
									.get(MESSAGES_SUBTITLESPECIFYINFO_PATH).toString()))
							.setReplacements(placeholdersReplacement).process().sendMessage(getPlayer());
					return false;
				} else {
					list.clear();
					list.addAll(messageList);
					groups.get().setModified(true);
					return true;
				}
			}

			public void cancel() {
				openInventory();
			}

		});
		closeInventory();
		MessageTemplate
				.inst(PandaPrefixedStringField.applyPrefix(
						getChestInventoryData().getCustomFields().get(MESSAGES_TITLESPECIFYINFO_PATH).toString()))
				.setReplacements(placeholdersReplacement).process().sendMessage(getPlayer());
	}

	private void onBossBarSpecify(List<String> list) {
		messageListenerService.getListener().replaceListening(getPlayer().getUniqueId(), new Callback() {

			@Override
			public boolean message(String message) {
				if (message.length() > ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING
						.getBossBarMaximumLengthInChars()) {
					MessageTemplate
							.inst(PandaPrefixedStringField.applyPrefix(getChestInventoryData().getCustomFields()
									.get(MESSAGES_EXCEEDEDMAXIMUMCHARACTERS_PATH).toString()))
							.setReplacements(
									new Replacement("{current_characters}", () -> String.valueOf(message.length())),
									new Replacement("{maximum_characters}",
											() -> String.valueOf(
													ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING
															.getBossBarMaximumLengthInChars())))
							.process().sendMessage(getPlayer());
					return false;
				}

				list.clear();
				list.add(message);
				groups.get().setModified(true);
				return true;
			}

			public void cancel() {
				openInventory();
			}

		});
		closeInventory();
		MessageTemplate
				.inst(PandaPrefixedStringField.applyPrefix(
						getChestInventoryData().getCustomFields().get(MESSAGES_BOSSBARSPECIFYINFO_PATH).toString()))
				.setReplacements(placeholdersReplacement).process().sendMessage(getPlayer());
	}

	@Override
	public void onClose(InventoryCloseEvent e) {
		super.onClose(e);

		this.groups.forEach((entry) -> {
			if (entry.isModified()) {
				try {
					RoyaleProtectionBlocksAPI.getInstance().getPlayerInteractionsService()
							.protectionSwitchSettingRequest(ProtectionSwitchSettingRequestInput.inst(getPlayer(),
									protection, ProtectionSettingsService.GREETING_FAREWELL_MESSAGES_SETTING,
									entry.getObject().getPermissionGroup(), entry.getObject()));
				} catch (RoyaleProtectionBlocksException e1) {
					e1.sendError(getPlayer());
				}
			}
		});
	}

}
