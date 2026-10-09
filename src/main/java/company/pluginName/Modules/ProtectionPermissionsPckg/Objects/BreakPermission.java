package company.pluginName.Modules.ProtectionPermissionsPckg.Objects;

import java.util.Arrays;

import org.bukkit.Material;

import company.pluginName.Modules.ProtectionPermissionsPckg.Objects.Templates.PermissionImpl;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;

public class BreakPermission extends PermissionImpl {

	public BreakPermission() {
		super("break", true, null, null, "Romper bloques", false, true, true, true, true, true, ItemBuilder.inst()
				.setMaterial(Material.DIAMOND_PICKAXE).setName("&#FFD166Romper bloques")
				.setLore(Arrays.asList("&7Permite destruir bloques dentro", "&7de la protección.")).build());
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
