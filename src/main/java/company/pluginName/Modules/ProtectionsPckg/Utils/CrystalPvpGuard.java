package company.pluginName.Modules.ProtectionsPckg.Utils;

import java.util.function.BiPredicate;
import org.bukkit.Location;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Crystal damage is PvP even when Minecraft cannot attribute it to a player. */
public final class CrystalPvpGuard {
    private CrystalPvpGuard() { }

    public static boolean shouldCancel(EntityDamageByEntityEvent event,
            BiPredicate<Location, Player> allowed) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player victim)) return false;
        Entity direct = event.getDamageSource().getDirectEntity();
        Entity causing = event.getDamageSource().getCausingEntity();
        Entity crystal = event.getDamager() instanceof EnderCrystal ? event.getDamager()
                : direct instanceof EnderCrystal ? direct
                : causing instanceof EnderCrystal ? causing : null;
        if (crystal == null) return false;
        // No combat-tag bypass: a crystal must not circumvent a protected area's PvP setting.
        if (!allowed.test(victim.getLocation(), victim)) return true;
        Player actor = causing instanceof Player player ? player : victim;
        if (!allowed.test(crystal.getLocation(), actor)) return true;
        return causing instanceof Player attacker && !allowed.test(attacker.getLocation(), attacker);
    }
}
