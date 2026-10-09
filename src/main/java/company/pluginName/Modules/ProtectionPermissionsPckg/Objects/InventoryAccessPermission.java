package company.pluginName.Modules.ProtectionPermissionsPckg.Objects;

import java.util.Arrays;

import org.bukkit.Material;

import company.pluginName.Modules.ProtectionPermissionsPckg.Objects.Templates.PermissionImpl;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemBuilder;
import darkpanda73.PandaUtils.Services.PandaFilesModule.Objects.Fields.PandaBooleanField;

public class InventoryAccessPermission extends PermissionImpl {

	private PandaBooleanField preventHoppersIfNonMembersAccessDeniedField = new PandaBooleanField(
			"Permissions.chestaccess.Prevent-hoppers-if-non-members-access-denied", false);
	private PandaBooleanField dropHoppersIfNonMembersAccessDeniedField = new PandaBooleanField(
			"Permissions.chestaccess.Drop-hoppers-if-non-members-access-denied", false);

	public InventoryAccessPermission() {
		super("chestaccess", true, null, null, "Acceso a inventarios", false, true, true, true, true, true,
				ItemBuilder.inst().setMaterial(Material.CHEST).setName("&#FFD166Abrir inventarios")
						.setLore(Arrays.asList("&7Permite abrir cofres, barriles, hornos", "&7y otros bloques con inventario.", "", "&7No requiere activar Interactuar."))
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

	public boolean isPreventHoppersIfNonMembersAccessDenied() {
		return preventHoppersIfNonMembersAccessDeniedField.isTrue();
	}

	public boolean isDropHoppersIfNonMembersAccessDenied() {
		return dropHoppersIfNonMembersAccessDeniedField.isTrue();
	}

}
