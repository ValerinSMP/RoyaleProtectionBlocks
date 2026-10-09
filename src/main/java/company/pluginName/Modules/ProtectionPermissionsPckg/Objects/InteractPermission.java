package company.pluginName.Modules.ProtectionPermissionsPckg.Objects;

import java.util.Arrays;

import org.bukkit.Material;

import company.pluginName.Modules.ProtectionPermissionsPckg.Objects.Templates.PermissionImpl;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;

public class InteractPermission extends PermissionImpl {

	public InteractPermission() {
		super("interact", true, null, null, "Interactuar", false, true, true, true, true, true,
				ItemBuilder.inst().setMaterial(Material.LEVER).setName("&#FFD166Interactuar")
						.setLore(Arrays.asList("&7Permite usar puertas, botones, palancas", "&7y objetos sin inventario.", "", "&7Para cofres y contenedores, configura", "&7Acceso a inventarios por separado."))
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
