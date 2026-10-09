package company.pluginName.Modules.ProtectionPermissionsPckg.Objects;

import java.util.Arrays;

import org.bukkit.Material;

import company.pluginName.Modules.ProtectionPermissionsPckg.Objects.Templates.PermissionImpl;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;

public class FlyPermission extends PermissionImpl {

	public FlyPermission() {
		super("fly", true, null, null, "Vuelo", false, true, false, true, true, true, ItemBuilder.inst()
				.setMaterial(Material.FEATHER).setName("&#FFD166Vuelo").setLore(Arrays
						.asList("&7Permite usar &#FFD166/pb fly &7dentro", "&7de la protección."))
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
