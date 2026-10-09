package company.pluginName.Modules.ProtectionsPckg.Utils;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.bukkit.Location;
import org.bukkit.ExplosionResult;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import royale.RoyaleProtectionBlocks.Plugin.API.RoyaleProtectionBlocksAPI;
import royale.RoyaleProtectionBlocks.Plugin.API.Interfaces.Protections.IProtection;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.FlightControl.FlightControlService;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.PlayerInteractions.PlayerInteractionsService;
import royale.RoyaleProtectionBlocks.Plugin.API.Services.Protections.ProtectionsService;
import royale.RoyaleProtectionBlocks.Plugin.Modules.ProtectionsPckg.Listeners.ProtectionsListener;

/** Calls the real compiled listener against a fake region index; does not start Bukkit. */
public final class BlockDestructionListenerTest {
    private static int cases;
    public static void main(String[] args) {
        IProtection region=proxy(IProtection.class,(method,values) -> null);
        ProtectionsService<?> service=proxy(ProtectionsService.class,(method,values) -> switch(method) {
            case "findProtectionByLocation" -> ((Location)values[0]).getX()<10 ? region : null;
            case "findProtectionBySourceBlock" -> ((Block)values[0]).getLocation().getX()==30 ? region : null;
            default -> throw new AssertionError(method);
        });
        new RoyaleProtectionBlocksAPI() {
            public FlightControlService getFlightControlService() { return null; }
            public PlayerInteractionsService getPlayerInteractionsService() { return null; }
            public ProtectionsService<?> getProtectionsService() { return service; }
        };
        ProtectionsListener listener=new ProtectionsListener();
        Block inside=block(0,Material.STONE), outside=block(20,Material.STONE);
        for(EntityType type : new EntityType[]{EntityType.WITHER,EntityType.WITHER_SKULL,EntityType.CREEPER,
                EntityType.FIREBALL,EntityType.END_CRYSTAL,EntityType.ENDER_DRAGON}) {
            var affected=new ArrayList<>(List.of(inside,outside,block(30,Material.STONE)));
            var explosion=new EntityExplodeEvent(entity(type),new Location(null,20,64,0),affected,1,
                    ExplosionResult.DESTROY);
            listener.onExplodeEntity(explosion);
            if(affected.size()!=1 || affected.get(0)!=outside || explosion.isCancelled())
                throw new AssertionError("Mixed explosion blocks " + type);
            cases++;
        }
        var affected=new ArrayList<>(List.of(inside,outside));
        var blockExplosion=new BlockExplodeEvent(outside,proxy(BlockState.class,(m,v)->null),affected,1,
                ExplosionResult.DESTROY);
        listener.onExplodeBlock(blockExplosion);
        if(affected.size()!=1 || affected.get(0)!=outside) throw new AssertionError("Bed/anchor explosion");
        cases++;
        BlockData air=proxy(BlockData.class,(method,values)->method.equals("getMaterial") ? Material.AIR : null);
        for(EntityType type : new EntityType[]{EntityType.WITHER,EntityType.ENDER_DRAGON,EntityType.ENDERMAN,
                EntityType.SILVERFISH,EntityType.RAVAGER,EntityType.RABBIT}) {
            for(Block block : new Block[]{inside,outside}) {
                var event=new EntityChangeBlockEvent(entity(type),block,air);
                listener.onMobGrief(event);
                check(block==inside,event,type + " claim boundary");
            }
        }
        for(EntityType type : new EntityType[]{EntityType.VILLAGER,EntityType.SHEEP,EntityType.FALLING_BLOCK}) {
            var event=new EntityChangeBlockEvent(entity(type),inside,air);
            listener.onMobGrief(event); check(false,event,"normal behavior " + type);
        }
        var sourceEvent=new EntityChangeBlockEvent(entity(EntityType.FALLING_BLOCK),block(30,Material.STONE),air);
        listener.onMobGrief(sourceEvent); check(true,sourceEvent,"PB source always protected");
        LivingEntity zombie=proxy(LivingEntity.class,(m,v)->m.equals("getType") ? EntityType.ZOMBIE : null);
        for(Block block : new Block[]{inside,outside}) {
            var door=new EntityBreakDoorEvent(zombie,block,air);
            listener.onMobBreakDoor(door); check(block==inside,door,"zombie breaks door");
        }
        var trample=new EntityInteractEvent(entity(EntityType.COW),block(0,Material.FARMLAND));
        listener.onMobTrample(trample); check(true,trample,"animal farmland");
        var otherBlock=new EntityInteractEvent(entity(EntityType.COW),inside);
        listener.onMobTrample(otherBlock); check(false,otherBlock,"other animal interaction");
        Player player=proxy(Player.class,(method,values)->method.equals("getType") ? EntityType.PLAYER : null);
        var playerTrample=new EntityInteractEvent(player,block(0,Material.FARMLAND));
        listener.onMobTrample(playerTrample); check(false,playerTrample,"player handled by permissions");
        for(Block block : new Block[]{inside,outside}) {
            var ignite=new BlockIgniteEvent(block,BlockIgniteEvent.IgniteCause.FIREBALL,entity(EntityType.FIREBALL));
            listener.onEntityIgnite(ignite); check(block==inside,ignite,"fireball ignition");
            var burn=new BlockBurnEvent(block,null);
            listener.onFireBurn(burn); check(block==inside,burn,"fire damage");
            BlockState state=proxy(BlockState.class,(method,values)->null);
            var spread=new BlockSpreadEvent(block,block(20,Material.FIRE),state);
            listener.onFireSpread(spread); check(block==inside,spread,"fire crossing border");
        }
        var playerIgnite=new BlockIgniteEvent(inside,BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL,player);
        listener.onEntityIgnite(playerIgnite); check(false,playerIgnite,"player ignition unchanged");
        var vegetation=new BlockSpreadEvent(inside,block(0,Material.GRASS_BLOCK),proxy(BlockState.class,(m,v)->null));
        listener.onFireSpread(vegetation); check(false,vegetation,"non-fire spread unchanged");
        for(var method : ProtectionsListener.class.getDeclaredMethods()) {
            EventHandler handler=method.getAnnotation(EventHandler.class);
            if(handler!=null && (method.getName().startsWith("onMob") || method.getName().startsWith("onFire")
                    || method.getName().equals("onEntityIgnite"))) {
                if(!handler.ignoreCancelled() || handler.priority()!=EventPriority.HIGHEST)
                    throw new AssertionError("Handler ordering " + method.getName());
                cases++;
            }
        }
        System.out.println("Real block listeners: " + cases + " checks passed (simulated events)");
    }
    private static Block block(double x,Material type) {
        return proxy(Block.class,(method,values)->switch(method) {
            case "getLocation" -> new Location(null,x,64,0);
            case "getType" -> type;
            default -> null;
        });
    }
    private static Entity entity(EntityType type) {
        return proxy(Entity.class,(method,values)->method.equals("getType") ? type : null);
    }
    private static <T> T proxy(Class<T> type,BiFunction<String,Object[],Object> invocation) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},
                (object,method,args)->invocation.apply(method.getName(),args)));
    }
    private static void check(boolean expected,Cancellable event,String label) {
        if(event.isCancelled()!=expected) throw new AssertionError(label);
        cases++;
    }
}
