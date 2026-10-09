package company.pluginName.Modules.ProtectionsPckg.Utils;
import java.lang.reflect.Proxy;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Barrel;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.event.block.Action;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection;
public final class InteractionPermissionRouteTest {
 private static int checks;
 static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
  return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},handler));
 }
 static void check(boolean condition) { if(!condition)throw new AssertionError(); checks++; }
 public static void main(String[] args) {
  for(Class<?> type:new Class<?>[]{Chest.class,Barrel.class,Furnace.class,Hopper.class}) {
   Object state=proxy(type,(p,m,a)->null);
   Block block=proxy(Block.class,(p,m,a)->m.getName().equals("getState")?state:null);
   check(InteractionPermissionRoute.opensInventory(Action.RIGHT_CLICK_BLOCK,block));
   check(!InteractionPermissionRoute.opensInventory(Action.LEFT_CLICK_BLOCK,block));
   check(!InteractionPermissionRoute.opensInventory(Action.PHYSICAL,block));
  }
  Block plain=proxy(Block.class,(p,m,a)->m.getName().equals("getState")?proxy(BlockState.class,(q,n,b)->null):null);
  check(!InteractionPermissionRoute.opensInventory(Action.RIGHT_CLICK_BLOCK,plain));
  check(!InteractionPermissionRoute.opensInventory(Action.RIGHT_CLICK_AIR,null));
  for(int bits=0;bits<4;bits++) {
   final boolean inventory=(bits&1)!=0, interact=(bits&2)!=0;
   IProtection region=proxy(IProtection.class,(p,m,a)->switch(m.getName()) {
    case "canOpenInventories" -> inventory;
    case "canInteract" -> interact;
    default -> throw new AssertionError(m);
   });
   check(InteractionPermissionRoute.allowed(region,null,true)==inventory);
   check(InteractionPermissionRoute.allowed(region,null,false)==interact);
  }
  System.out.println("Interaction routing: "+checks+" checks passed");
 }
}
