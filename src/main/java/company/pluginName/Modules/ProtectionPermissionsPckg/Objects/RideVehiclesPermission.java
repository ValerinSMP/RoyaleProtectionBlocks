package company.pluginName.Modules.ProtectionPermissionsPckg.Objects;

import java.util.Arrays;

import org.bukkit.Material;

import company.pluginName.Modules.ProtectionPermissionsPckg.Objects.Templates.PermissionImpl;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;

public class RideVehiclesPermission extends PermissionImpl {

	public RideVehiclesPermission() {
		super("ride-vehicles", true, null, null, "Usar vehículos", false, true, true, true, true, true, ItemBuilder
				.inst().setMaterial(Material.LEATHER_HORSE_ARMOR)
				.setName("&#FFD166Usar vehículos")
				.setLore(Arrays.asList("&7Permite subir a caballos, botes,", "&7vagonetas y otros vehículos."))
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
