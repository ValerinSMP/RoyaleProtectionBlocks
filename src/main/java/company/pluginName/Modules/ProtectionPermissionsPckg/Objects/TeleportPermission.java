package company.pluginName.Modules.ProtectionPermissionsPckg.Objects;

import java.util.Arrays;

import org.bukkit.Material;

import company.pluginName.Modules.ProtectionPermissionsPckg.Objects.Templates.PermissionImpl;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;

public class TeleportPermission extends PermissionImpl {

	public TeleportPermission() {
		super("teleport", true, null, null, "Teletransporte", false, false, true, true, true, true,
				ItemBuilder.inst().setMaterial(Material.ENDER_PEARL)
						.setName("&#FFD166Teletransporte").setLore(Arrays
								.asList("&7Permite viajar al punto de inicio", "&7de la protección desde el menú."))
						.build());
	}

	@Override
	public Boolean getStaffValue() {
		return true;
	}

	@Override
	public Boolean getMainOwnerValue() {
		return true;
	}

}
