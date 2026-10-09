package company.pluginName.Modules.ProtectionsPckg.Utils;

import java.lang.reflect.Proxy;
import java.util.List;

import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection;

public final class ProtectionDisplayNamesTest {

	public static void main(String[] args) {
		assert "mtynnn_1".equals(ProtectionDisplayNames.automaticName("Mtynnn", 1));
		assert "Mtynnn #1".equals(ProtectionDisplayNames.forGui("mtynnn_1", "Mtynnn", "world_0_64_0"));
		assert "Casa Roja".equals(ProtectionDisplayNames.forGui("Casa Roja", "Mtynnn", "world_0_64_0"));
		assert ProtectionDisplayNames.nextAutomaticNumber(List.of(protection("mtynnn_1"), protection("Casa Roja"),
				protection("mtynnn_3"))) == 2;
	}

	private static IProtection protection(String displayName) {
		return (IProtection) Proxy.newProxyInstance(IProtection.class.getClassLoader(), new Class<?>[] { IProtection.class },
				(proxy, method, args) -> switch (method.getName()) {
				case "getDisplayName" -> displayName;
				case "getOwnerName" -> "Mtynnn";
				default -> null;
				});
	}
}
