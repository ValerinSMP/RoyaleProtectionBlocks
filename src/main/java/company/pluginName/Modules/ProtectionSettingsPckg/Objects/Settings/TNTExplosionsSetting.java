package company.pluginName.Modules.ProtectionSettingsPckg.Objects.Settings;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;

import company.pluginName.Modules.ProtectionSettingsPckg.Objects.Settings.Templates.BooleanSettingImpl;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.PermissionGroup;

public class TNTExplosionsSetting extends BooleanSettingImpl {

	public TNTExplosionsSetting() {
		super("tnt-explosions", true, null, null, "Explosiones de TNT", true, null, null, null,
				ItemBuilder.inst().setMaterial(Material.TNT).setName("&#FFD166Explosiones de TNT: &f{value}")
						.setLore(Arrays.asList("&7Define si la TNT puede destruir", "&7bloques dentro de la protección."))
						.build());
	}

	@Override
	public List<PermissionGroup> getManagedGroups() {
		return Arrays.asList(PermissionGroup.GENERIC);
	}

}
