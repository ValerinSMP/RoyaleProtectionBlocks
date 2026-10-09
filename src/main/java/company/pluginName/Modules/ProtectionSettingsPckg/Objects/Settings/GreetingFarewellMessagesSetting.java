package company.pluginName.Modules.ProtectionSettingsPckg.Objects.Settings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.Material;

import company.pluginName.Modules.ProtectionSettingsPckg.Objects.Settings.GreetingFarewellMessagesSetting.ConfiguredMessages;
import company.pluginName.Modules.ProtectionSettingsPckg.Objects.Settings.Templates.SettingImpl;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaIntegerField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaStringField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaStringListField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.PermissionGroup;

public class GreetingFarewellMessagesSetting extends SettingImpl<ConfiguredMessages> {

	private ConfiguredMessages defaultConfiguration;

	private PandaStringListField settingDefaultGreetingMessage;
	private PandaStringListField settingDefaultFarewellMessage;
	private PandaStringField settingDefaultMessageType;
	private PandaStringListField settingEditableGroups;
	private PandaStringListField settingAvailableMessageTypes;
	private PandaIntegerField settingMessageMaximumLengthInChars;
	private PandaIntegerField settingActionBarMaximumLengthInChars;
	private PandaIntegerField settingTitleMaximumLengthInChars;
	private PandaIntegerField settingSubtitleMaximumLengthInChars;
	private PandaIntegerField settingBossBarMaximumLengthInChars;

	public GreetingFarewellMessagesSetting() {
		super("greeting-farewell-messages", true, null, null, "Mensajes de entrada y salida", null, null, null, null,
				ItemBuilder.inst().setMaterial(Material.OAK_SIGN).setName("&#FFD166Mensajes de entrada y salida")
						.setLore(Arrays.asList("&7Personaliza los avisos mostrados", "&7al entrar o salir de la protección."))
						.build());

		settingDefaultGreetingMessage = new PandaStringListField("Settings." + getId() + ".Default-greeting-message",
				Arrays.asList("&eWelcome &7{player_name} &eto &a{protection_name}&e!")) {
			@Override
			public PandaField<List<String>> setContent(List<String> content) {
				PandaField<List<String>> result = super.setContent(content);
				defaultConfiguration.setGreetingMessage(getDefaultGreetingMessage());
				return result;
			}
		};

		settingDefaultFarewellMessage = new PandaStringListField("Settings." + getId() + ".Default-farewell-message",
				Arrays.asList("&eSee you later! :D")) {
			@Override
			public PandaField<List<String>> setContent(List<String> content) {
				PandaField<List<String>> result = super.setContent(content);
				defaultConfiguration.setFarewellMessage(getDefaultFarewellMessage());
				return result;
			}
		};

		settingDefaultMessageType = new PandaStringField("Settings." + getId() + ".Default-message-type",
				MessageType.MESSAGE.name()) {
			@Override
			public PandaField<String> setContent(String content) {
				PandaField<String> result = super.setContent(content);
				defaultConfiguration.setMessageType(getDefaultMessageType());
				return result;
			}
		};

		settingEditableGroups = new PandaStringListField(
				"Settings." + getId() + ".Editable-groups", Arrays
						.asList(PermissionGroup.NON_MEMBERS, PermissionGroup.MEMBERS, PermissionGroup.OWNERS,
								PermissionGroup.MAIN_OWNER)
						.stream().map(PermissionGroup::name).collect(Collectors.toList()));

		settingAvailableMessageTypes = new PandaStringListField("Settings." + getId() + ".Available-message-types",
				Arrays.asList(MessageType.MESSAGE, MessageType.ACTION_BAR, MessageType.TITLE, MessageType.BOSS_BAR)
						.stream().map(MessageType::name).collect(Collectors.toList()));

		settingMessageMaximumLengthInChars = new PandaIntegerField(
				"Settings." + getId() + ".Message-maximum-length-in-chars", 512);
		settingActionBarMaximumLengthInChars = new PandaIntegerField(
				"Settings." + getId() + ".Action-bar-maximum-length-in-chars", 128);
		settingTitleMaximumLengthInChars = new PandaIntegerField(
				"Settings." + getId() + ".Title-maximum-length-in-chars", 64);
		settingSubtitleMaximumLengthInChars = new PandaIntegerField(
				"Settings." + getId() + ".Subtitle-maximum-length-in-chars", 128);
		settingBossBarMaximumLengthInChars = new PandaIntegerField(
				"Settings." + getId() + ".Boss-bar-maximum-length-in-chars", 128);

		defaultConfiguration = new ConfiguredMessages(null, getDefaultGreetingMessage(), getDefaultFarewellMessage(),
				getDefaultMessageType());

		this.defaultGenericValue = defaultConfiguration;
		this.defaultNonMembersValue = defaultConfiguration;
		this.defaultMembersValue = defaultConfiguration;
		this.defaultOwnersValue = defaultConfiguration;
	}

	@Override
	public ConfiguredMessages getMainOwnerValue() {
		return this.defaultOwnersValue;
	}

	@Override
	public ConfiguredMessages getStaffValue() {
		return this.defaultNonMembersValue;
	}

	public List<String> getDefaultGreetingMessage() {
		return this.settingDefaultGreetingMessage.getContent();
	}

	public List<String> getDefaultFarewellMessage() {
		return this.settingDefaultFarewellMessage.getContent();
	}

	public MessageType getDefaultMessageType() {
		try {
			return MessageType.valueOf(this.settingDefaultMessageType.getContent().toUpperCase());
		} catch (IllegalArgumentException e) {
			return MessageType.MESSAGE;
		}
	}

	public List<PermissionGroup> getEditableGroups() {
		return this.settingEditableGroups.getContent().stream().map(group -> {
			try {
				return PermissionGroup.valueOf(group);
			} catch (Throwable e) {
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public List<MessageType> getAvailableMessageTypes() {
		return this.settingAvailableMessageTypes.getContent().stream().map(type -> {
			try {
				return MessageType.valueOf(type);
			} catch (Throwable e) {
				return null;
			}
		}).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public int getMessageMaximumLengthInChars() {
		return this.settingMessageMaximumLengthInChars.getContent();
	}

	public int getActionBarMaximumLengthInChars() {
		return this.settingActionBarMaximumLengthInChars.getContent();
	}

	public int getTitleMaximumLengthInChars() {
		return this.settingTitleMaximumLengthInChars.getContent();
	}

	public int getSubtitleMaximumLengthInChars() {
		return this.settingSubtitleMaximumLengthInChars.getContent();
	}

	public int getBossBarMaximumLengthInChars() {
		return this.settingBossBarMaximumLengthInChars.getContent();
	}

	@Override
	public List<PermissionGroup> getManagedGroups() {
		return Arrays.asList(PermissionGroup.NON_MEMBERS, PermissionGroup.MEMBERS, PermissionGroup.OWNERS);
	}

	@Override
	public ConfiguredMessages parseStringToValue(String value) {
		throw new UnsupportedOperationException();
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ConfiguredMessages implements Serializable, Cloneable {

		private static final long serialVersionUID = 536081477069083264L;

		private PermissionGroup permissionGroup;
		private List<String> greetingMessage;
		private List<String> farewellMessage;
		private MessageType messageType = MessageType.MESSAGE;

		@Override
		public ConfiguredMessages clone() {
			ConfiguredMessages clone = new ConfiguredMessages();

			clone.permissionGroup = this.permissionGroup;
			clone.greetingMessage = new ArrayList<>(this.greetingMessage);
			clone.farewellMessage = new ArrayList<>(this.farewellMessage);
			clone.messageType = this.messageType;

			return clone;
		}

		public ConfiguredMessages clone(PermissionGroup group) {
			ConfiguredMessages clone = this.clone();

			clone.permissionGroup = group;

			return clone;
		}

	}

	public static enum MessageType {
		MESSAGE, ACTION_BAR, TITLE, BOSS_BAR;
	}

}
