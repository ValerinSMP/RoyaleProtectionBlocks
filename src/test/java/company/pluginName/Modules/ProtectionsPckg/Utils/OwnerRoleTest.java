package company.pluginName.Modules.ProtectionsPckg.Utils;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.UUID;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection;
import royale.RoyaleProtectionBlocks.Plugin.API.Enums.PermissionGroup;

/** Execute the installed API's actual default method, not a copy of its logic. */
public final class OwnerRoleTest {
    public static void main(String[] args) {
        int cases = 0;
        for(int bits=0; bits<8; bits++) {
            final int roles = bits;
            IProtection protection = (IProtection) Proxy.newProxyInstance(IProtection.class.getClassLoader(),
                    new Class<?>[]{IProtection.class}, (proxy,method,arguments) -> {
                        if(method.isDefault()) return InvocationHandler.invokeDefault(proxy,method,arguments);
                        return switch(method.getName()) {
                            case "isMainOwner" -> (roles & 4)!=0;
                            case "isOwner" -> (roles & 2)!=0;
                            case "isMember" -> (roles & 1)!=0;
                            default -> throw new AssertionError("Unexpected " + method);
                        };
                    });
            PermissionGroup expected = (bits & 4)!=0 ? PermissionGroup.MAIN_OWNER
                    : (bits & 2)!=0 ? PermissionGroup.OWNERS
                    : (bits & 1)!=0 ? PermissionGroup.MEMBERS : PermissionGroup.NON_MEMBERS;
            if(protection.getGroup(UUID.randomUUID()) != expected) throw new AssertionError("Role priority " + bits);
            if(protection.getGroup((UUID)null) != PermissionGroup.GENERIC) throw new AssertionError("Null role");
            cases += 2;
        }
        System.out.println("Installed owner-role API: " + cases + " cases passed (not a gameplay promotion test)");
    }
}
