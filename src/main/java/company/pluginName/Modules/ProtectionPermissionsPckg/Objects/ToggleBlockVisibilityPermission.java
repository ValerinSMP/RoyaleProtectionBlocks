package company.pluginName.Modules.ProtectionPermissionsPckg.Objects;

import java.util.Arrays;

import org.bukkit.Material;

import company.pluginName.Modules.ProtectionPermissionsPckg.Objects.Templates.PermissionImpl;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;

public class ToggleBlockVisibilityPermission extends PermissionImpl {

	public ToggleBlockVisibilityPermission() {
		super("toggle-block-visibility", true, null, null, "Visibilidad del bloque", false, false, false, true, true, true,
				ItemBuilder.inst().setMaterial(Material.GRASS_BLOCK)
						.setName("&#FFD166Visibilidad del bloque")
						.setLore(Arrays.asList("&7Permite mostrar u ocultar el bloque", "&7de protección desde el menú."))
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
