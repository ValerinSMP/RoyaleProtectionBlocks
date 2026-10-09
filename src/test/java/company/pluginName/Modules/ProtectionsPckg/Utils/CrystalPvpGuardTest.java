package company.pluginName.Modules.ProtectionsPckg.Utils;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public final class CrystalPvpGuardTest {
    private static int assertions;
    public static void main(String[] args) {
        Player victim = entity(Player.class, 0);
        Player attacker = entity(Player.class, 20);
        EnderCrystal crystal = entity(EnderCrystal.class, 10);
        Entity mob = entity(Entity.class, 30);
        BiPredicate<Location, Player> allow = (location, player) -> true;
        check(true, event(crystal, victim, crystal, attacker), (l,p) -> l.getX()!=0, "victim safe region");
        check(true, event(crystal, victim, crystal, attacker), (l,p) -> l.getX()!=10, "crystal inside safe region");
        check(true, event(crystal, victim, crystal, attacker), (l,p) -> l.getX()!=20, "attacker safe region");
        check(false, event(crystal, victim, crystal, attacker), allow, "PvP allowed");
        check(true, event(crystal, victim, crystal, null), (l,p) -> false, "unattributed crystal protected");
        check(false, event(crystal, victim, crystal, null), allow, "unattributed outside protection");
        check(true, event(attacker, victim, crystal, attacker), (l,p) -> false, "crystal from DamageSource");
        check(true, event(attacker, victim, null, crystal), (l,p) -> false, "crystal causing entity");
        check(true, event(crystal, victim, crystal, victim), (l,p) -> false, "self or same-clan crystal still protected");
        AtomicInteger calls = new AtomicInteger();
        BiPredicate<Location,Player> unexpected = (l,p) -> { calls.incrementAndGet(); return false; };
        check(false, event(attacker, victim, attacker, attacker), unexpected, "direct hit preserved");
        check(false, event(mob, victim, mob, null), unexpected, "mob damage preserved");
        check(false, event(crystal, mob, crystal, attacker), unexpected, "non-player preserved");
        EntityDamageByEntityEvent cancelled = event(crystal, victim, crystal, attacker);
        cancelled.setCancelled(true);
        check(false, cancelled, unexpected, "cancelled event preserved");
        if(calls.get()!=0) throw new AssertionError("Unnecessary permission queries");
        System.out.println("Crystal PvP: " + assertions + " regression cases passed");
    }
    private static void check(boolean expected, EntityDamageByEntityEvent event,
            BiPredicate<Location,Player> allowed, String label) {
        if(CrystalPvpGuard.shouldCancel(event, allowed)!=expected) throw new AssertionError(label);
        assertions++;
    }
    private static EntityDamageByEntityEvent event(Entity damager, Entity victim, Entity direct, Entity causing) {
        DamageSource source = (DamageSource) Proxy.newProxyInstance(DamageSource.class.getClassLoader(),
                new Class<?>[]{DamageSource.class}, (proxy, method, args) -> switch(method.getName()) {
                    case "getDirectEntity" -> direct;
                    case "getCausingEntity" -> causing;
                    default -> null;
                });
        return new EntityDamageByEntityEvent(damager, victim, DamageCause.ENTITY_EXPLOSION, source, 10);
    }
    private static <T extends Entity> T entity(Class<T> type, double x) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy,method,args) -> {
            if(method.getName().equals("getLocation")) return new Location(null,x,64,0);
            if(method.getName().equals("toString")) return type.getSimpleName();
            if(method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if(method.getName().equals("equals")) return proxy == args[0];
            return null;
        }));
    }
}
