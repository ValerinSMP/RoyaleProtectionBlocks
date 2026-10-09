package company.pluginName.Modules.ProtectionsPckg.Utils;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.entity.Player;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection;
public final class InteractionPermissionRoute {
 private InteractionPermissionRoute() {}
 public static boolean opensInventory(Action action, Block block) {
  return block != null && action == Action.RIGHT_CLICK_BLOCK
    && block.getState() instanceof BlockInventoryHolder;
 }
 public static boolean allowed(IProtection protection, Player player, boolean inventory) {
  return inventory ? protection.canOpenInventories(player) : protection.canInteract(player);
 }
}
