package company.pluginName.Modules.ProtectionsPckg.Objects.Components.Permissions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import company.pluginName.Exceptions.RoyaleProtectionBlocksExceptionImpl;
import company.pluginName.Modules.PlayersDataPckg.PlayerDataService;
import company.pluginName.Modules.PlayersDataPckg.Objects.PlayerData;
import company.pluginName.Modules.ProtectionPermissionsPckg.ProtectionPermissionsService;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import company.pluginName.Modules.ProtectionsPckg.Utils.ProtectionPvpBridge;
import company.pluginName.Modules.SQLPckg.SQLService;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Utils.TasksUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.PermissionGroup;
import royale.RoyaleProtectionBlocks.Plugin.API.Objects.Permissions.PermissionInterface;

@Data
@AllArgsConstructor
public class ProtectionPermissionManager {

	@PandaInject
	private static ProtectionPermissionsService protectionPermissionsService;

	@PandaInject
	private static SQLService sqlService;

	@PandaInject
	private static PlayerDataService playerDataService;

	private Protection protection;

	private Map<String, ProtectionPermission> permissions = new HashMap<>();

	public ProtectionPermissionManager(Protection protection) {
		this.protection = protection;
	}

	public void resetPermissions() {
		this.permissions.clear();
	}

	public void setValue(PermissionInterface permission, PermissionGroup group, Boolean value) {
		ProtectionPermission protectionPermission = permissions.computeIfAbsent(permission.getId(),
				(id) -> new ProtectionPermission(id));

		switch (group) {
		case NON_MEMBERS:
			protectionPermission.setNonMembersValue(value);
			break;
		case MEMBERS:
			protectionPermission.setMembersValue(value);
			break;
		case OWNERS:
			protectionPermission.setOwnersValue(value);
			break;
		default:
			return;
		}

		if ("pvp".equals(permission.getId())) {
			boolean visitors = Boolean.TRUE.equals(getValue(permission, PermissionGroup.NON_MEMBERS));
			boolean members = Boolean.TRUE.equals(getValue(permission, PermissionGroup.MEMBERS));
			boolean owners = Boolean.TRUE.equals(getValue(permission, PermissionGroup.OWNERS));
			try {
				protection.performAllProtections(child -> ProtectionPvpBridge.synchronize(
						((Protection) child).getProtectedRegion(), visitors, members, owners));
			} catch (Throwable failure) {
				throw new IllegalStateException("Could not synchronize protection PvP flag", failure);
			}
		}

		TasksUtils.executeOnAsync(() -> {
			try {
				sqlService.saveProtectionPermission(protection.getProtectionId(), protectionPermission);
			} catch (Exception e) {
				Bukkit.getLogger().log(java.util.logging.Level.SEVERE,
						"Could not save protection permission", e);
			}
		});
	}

	public Boolean getValue(PermissionInterface permission, Player player) {
		PermissionGroup permissionGroup = PermissionGroup.NON_MEMBERS;

		if (player != null) {
			if (protection.isMainOwner(player.getUniqueId())) {
				permissionGroup = PermissionGroup.MAIN_OWNER;
			} else if (hasStaffMode(player)) {
				permissionGroup = PermissionGroup.STAFF;
			} else if (protection.isOwner(player.getUniqueId())) {
				permissionGroup = PermissionGroup.OWNERS;
			} else if (protection.isMember(player.getUniqueId())) {
				permissionGroup = PermissionGroup.MEMBERS;
			}
		}

		return getValue(permission, permissionGroup);
	}

	public Boolean getValue(PermissionInterface permission, UUID playerUuid) {
		PermissionGroup permissionGroup = PermissionGroup.NON_MEMBERS;

		if (playerUuid != null) {
			if (protection.isMainOwner(playerUuid)) {
				permissionGroup = PermissionGroup.MAIN_OWNER;
			} else if (protection.isOwner(playerUuid)) {
				permissionGroup = PermissionGroup.OWNERS;
			} else if (protection.isMember(playerUuid)) {
				permissionGroup = PermissionGroup.MEMBERS;
			}
		}

		return getValue(permission, permissionGroup);
	}

	public Boolean getValue(PermissionInterface permission, PermissionGroup group) {
		ProtectionPermission protectionPermission = permissions.get(permission.getId());

		switch (group) {
		case NON_MEMBERS:
			return protectionPermission != null && protectionPermission.getNonMembersValue() != null
					? protectionPermission.getNonMembersValue()
					: permission.getNonMembersValue();
		case MEMBERS:
			return protectionPermission != null && protectionPermission.getMembersValue() != null
					? protectionPermission.getMembersValue()
					: permission.getMembersValue();
		case OWNERS:
			return protectionPermission != null && protectionPermission.getOwnersValue() != null
					? protectionPermission.getOwnersValue()
					: permission.getOwnersValue();
		case MAIN_OWNER:
			return permission.getMainOwnerValue();
		case STAFF:
			return permission.getStaffValue();
		default:
			return permission.getNonMembersValue();
		}
	}

	private boolean hasStaffMode(Player pl) {
		PlayerData playerData = playerDataService.getPlayerData(pl);
		return playerData != null && playerData.isStaffMode();
	}

}
