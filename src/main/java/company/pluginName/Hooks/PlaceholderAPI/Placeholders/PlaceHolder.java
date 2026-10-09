package company.pluginName.Hooks.PlaceholderAPI.Placeholders;

import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import company.pluginName.Hooks.WorldGuard.WorldGuardAPI;
import company.pluginName.Modules.FilePckg.Messages;
import company.pluginName.Modules.PermissionsPckg.PermissionsService;
import company.pluginName.Modules.PlayersDataPckg.PlayerDataService;
import company.pluginName.Modules.PlayersDataPckg.Objects.PlayerData;
import company.pluginName.Modules.ProtectionBlocksPckg.ProtectionBlocksService;
import company.pluginName.Modules.ProtectionBlocksPckg.Objects.ProtectionBlock;
import company.pluginName.Modules.ProtectionFlagsPckg.ProtectionFlagsService;
import company.pluginName.Modules.ProtectionPermissionsPckg.ProtectionPermissionsService;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionDisplayNames;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Annotation.RegisteredPandaField;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaStringField;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import royale.RoyaleProtectionBlocks.Plugin.API.RoyaleProtectionBlocksAPI;
import royale.RoyaleProtectionBlocks.Plugin.API.Exceptions.RoyaleProtectionBlocksException;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.ProtectionBlocks.IProtectionBlock;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtectionFlags.FlagRetrieveRequestInput;
import royale.RoyaleProtectionBlocks.Plugin.API.Objects.Permissions.PermissionInterface;

public class PlaceHolder extends PlaceholderExpansion {

	@RegisteredPandaField("lang")
	private static final PandaStringField PLACEHOLDER_COMMON_ALLOWED = new PandaStringField(
			"Placeholder.Common.Allowed", "Allowed");

	@RegisteredPandaField("lang")
	private static final PandaStringField PLACEHOLDER_COMMON_DENIED = new PandaStringField("Placeholder.Common.Denied",
			"Denied");

	@RegisteredPandaField("lang")
	private static final PandaStringField PLACEHOLDER_ROLES_FORAGER = new PandaStringField("Placeholder.Roles.Forager",
			"Forager");

	@RegisteredPandaField("lang")
	private static final PandaStringField PLACEHOLDER_ROLES_MEMBER = new PandaStringField("Placeholder.Common.Member",
			"Member");

	@RegisteredPandaField("lang")
	private static final PandaStringField PLACEHOLDER_ROLES_OWNER = new PandaStringField("Placeholder.Common.Owner",
			"Owner");

	@RegisteredPandaField("lang")
	private static final PandaStringField PLACEHOLDER_ROLES_MAINOWNER = new PandaStringField(
			"Placeholder.Common.Main-owner", "Main Owner");

	@RegisteredPandaField("lang")
	private static final PandaStringField PLACEHOLDER_TRUE = new PandaStringField("Placeholder.Common.True", "True");

	@RegisteredPandaField("lang")
	private static final PandaStringField PLACEHOLDER_FALSE = new PandaStringField("Placeholder.Common.False", "False");

	private static final String PROTECTION_FLAG_PREFIX = "protection_flag_";
	private static final String PROTECTION_PERMISSION_PREFIX = "protection_permission_";
	private static final String PROTECTIONBLOCK_CURRENT_PREFIX = "protectionblock_current_";
	private static final String PROTECTIONBLOCK_MAX_PREFIX = "protectionblock_max_";

	@PandaInject
	private static WorldGuardAPI worldGuardApi;

	@PandaInject
	private static ProtectionBlocksService protectionBlocksService;

	@PandaInject
	private static ProtectionFlagsService protectionFlagsService;

	@PandaInject
	private static PlayerDataService playerDataService;

	@PandaInject
	private static ProtectionPermissionsService protectionPermissionsService;

	@Override
	public String getAuthor() {
		return "DarkPanda73";
	}

	@Override
	public String getIdentifier() {
		return "protections";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier) {
		if (player != null) {
			if (identifier.toLowerCase().startsWith(PROTECTION_FLAG_PREFIX)) {
				IProtection protection = getProtectionIn(player);
				if (protection != null) {
					String flagName = identifier.substring(PROTECTION_FLAG_PREFIX.length());
					try {
						return protection.getFlags().getFlagValueAsString(FlagRetrieveRequestInput.inst(flagName));
					} catch (RoyaleProtectionBlocksException e) {
						return "";
					}
				}
			} else if (identifier.toLowerCase().startsWith(PROTECTION_PERMISSION_PREFIX)) {
				String permissionId = identifier.substring(PROTECTION_PERMISSION_PREFIX.length());
				if (!permissionId.isEmpty()) {
					IProtection protection = getProtectionIn(player);
					if (protection != null) {
						PermissionInterface permission = protectionPermissionsService.getPermission(permissionId);

						if (permission != null) {
							return Boolean.TRUE.equals(protection.getPermissionValue(permission, player))
									? PLACEHOLDER_COMMON_ALLOWED.getContent()
									: PLACEHOLDER_COMMON_DENIED.getContent();
						}
					}
				}
			} else if (identifier.toLowerCase().startsWith(PROTECTIONBLOCK_MAX_PREFIX)) {
				String blockId = identifier.substring(PROTECTIONBLOCK_MAX_PREFIX.length());
				if (!blockId.isEmpty()) {
					ProtectionBlock protectionBlock = protectionBlocksService.getProtectionBlockById(blockId);
					if (protectionBlock != null) {
						Integer blockCapacity = PermissionsService.getPerBlockMaxCapacity(player, protectionBlock);
						if (blockCapacity != null) {
							return blockCapacity.toString();
						}
					}
				}
			} else if (identifier.toLowerCase().startsWith(PROTECTIONBLOCK_CURRENT_PREFIX)) {
				String blockId = identifier.substring(PROTECTIONBLOCK_CURRENT_PREFIX.length());
				if (!blockId.isEmpty()) {
					ProtectionBlock protectionBlock = protectionBlocksService.getProtectionBlockById(blockId);
					if (protectionBlock != null) {
						return String.valueOf(RoyaleProtectionBlocksAPI.getInstance().getProtectionsService()
								.findProtectionsByOwner(player.getUniqueId()).stream()
								.filter(protection -> protection.getProtectionBlockId().equals(blockId)).count());
					}
				}
			} else {
				IProtection protection;
				switch (identifier.toLowerCase()) {
				case "protection_owner":
					protection = getProtectionIn(player);
					if (protection != null) {
						return protection.getOwnerName();
					}
					break;
				case "protection_id":
					protection = getProtectionIn(player);
					if (protection != null) {
						return protection.getProtectionId();
					}
					break;
				case "protection_name":
					protection = getProtectionIn(player);
					if (protection != null) {
						return ProtectionDisplayNames.forGui(protection);
					}
					break;
				case "protection_public_access":
					protection = getProtectionIn(player);
					if (protection != null) {
						return protection.isPublicAccess() ? PLACEHOLDER_TRUE.toString() : PLACEHOLDER_FALSE.toString();
					}
					break;
				case "protection_size":
					protection = getProtectionIn(player);
					if (protection != null) {
						IProtectionBlock block = protection.getProtectionBlock();
						String blocksX = String.valueOf((block.getBlocksX() * 2) + 1);
						String blocksY = block.getBlocksY() == -1 ? Messages.MESSAGE_GENERAL_NOLIMIT.toString()
								: String.valueOf((block.getBlocksY() * 2) + 1);
						String blocksZ = String.valueOf((block.getBlocksZ() * 2) + 1);
						return String.format("%sx%sx%s", blocksX, blocksY, blocksZ);
					}
					break;
				case "protection_role":
					protection = getProtectionIn(player);
					if (protection != null) {
						if (protection.isMainOwner(player.getUniqueId())) {
							return PLACEHOLDER_ROLES_MAINOWNER.getContent();
						} else if (protection.isOwner(player.getUniqueId())) {
							return PLACEHOLDER_ROLES_OWNER.getContent();
						} else if (protection.isMember(player.getUniqueId())) {
							return PLACEHOLDER_ROLES_MEMBER.getContent();
						} else {
							return PLACEHOLDER_ROLES_FORAGER.getContent();
						}
					}
					break;
				case "player_current":
					return String.valueOf(RoyaleProtectionBlocksAPI.getInstance().getProtectionsService()
							.findProtectionsByOwner(player.getUniqueId()).size());
				case "player_max":
					Integer maxAmount = PermissionsService.getGeneralMaxCapacity(player);
					return maxAmount != null ? String.valueOf(maxAmount) : "---";
				}
			}
		}
		return "";
	}

	private List<? extends IProtection> getProtectionsIn(Player player) {
		PlayerData playerData = playerDataService.getPlayerData(player);
		if (playerData != null) {
			return playerData.getCurrentProtections();
		}
		return Collections.emptyList();
	}

	private IProtection getProtectionIn(Player player) {
		return getProtectionsIn(player).stream().findFirst().orElse(null);
	}

}
