package company.pluginName.Modules.ProtectionPermissionsPckg.Objects;

import java.util.Arrays;

import org.bukkit.Material;

import company.pluginName.Modules.ProtectionPermissionsPckg.Objects.Templates.PermissionImpl;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;

public class PlacePermission extends PermissionImpl {

	public PlacePermission() {
		super("place", true, null, null, "Colocar bloques", false, true, true, true, true, true,
				ItemBuilder.inst().setMaterial(Material.STONE).setName("&#FFD166Colocar bloques")
						.setLore(Arrays.asList("&7Permite colocar bloques dentro", "&7de la protección."))
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
