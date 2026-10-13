package company.pluginName.Modules.ProtectionParticlesPckg;

import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.*;
import darkpanda73.PandaUtils.PandaColors.Messages.Objects.MessageTemplate;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import company.pluginName.MainPluginClass;
import company.pluginName.API.RoyaleProtectionBlocksAPIImpl;
import company.pluginName.Modules.ProtectionsPckg.ProtectionsServiceImpl;
import company.pluginName.Modules.ProtectionsPckg.Objects.Protection;
import darkpanda73.PandaUtils.PandaPlugin.Utils.TasksUtils;

/** Runs the real patched service against a finite scheduler, with no DB or server. */
public final class ProtectionViewLifecycleTest {
    static final List<Task> tasks = new ArrayList<>();
    static final Map<UUID, Player> online = new HashMap<>();
    static int checks, notices, searches, registrations, bars;
    static class Task implements BukkitTask {
        final Runnable runnable; final int id = tasks.size()+1; boolean cancelled;
        Task(Runnable runnable) { this.runnable=runnable; tasks.add(this); }
        public int getTaskId(){return id;} public Plugin getOwner(){return null;}
        public boolean isSync(){return true;} public boolean isCancelled(){return cancelled;}
        public void cancel(){cancelled=true;} void tick(){if(!cancelled)runnable.run();}
    }
    static class Regions extends ProtectionsServiceImpl {
        @Override public Stream<Protection> findProtectionsByArea(Location min, Location max) {
            searches++; return Stream.empty();
        }
    }
    static final class Session {
        final UUID id; boolean connected=true; World world; Player player;
        Session(UUID id, World world) {
            this.id=id;this.world=world;
            player=proxy(Player.class,(p,m,a)->switch(m.getName()) {
                case "getUniqueId" -> id; case "getWorld" -> this.world;
                case "getLocation" -> new Location(this.world, 10,64,10);
                case "isOnline" -> connected;
                case "sendMessage" -> {notices++;yield null;}
                case "sendActionBar" -> {bars++;yield null;}
                case "spigot" -> new Player.Spigot() {
                    @Override public void sendMessage(ChatMessageType type, BaseComponent component) {
                        if (type == ChatMessageType.ACTION_BAR) { bars++; } else { notices++; }
                    }
                    @Override public void sendMessage(ChatMessageType type, BaseComponent... components) {
                        if (type == ChatMessageType.ACTION_BAR) { bars++; } else { notices++; }
                    }
                };
                default -> fallback(p,m,a);
            });
            online.put(id,player);
        }
    }
    static World world(String name) {
        UUID id=UUID.randomUUID();
        return proxy(World.class,(w,m,a)->switch(m.getName()) {
            case "getName" -> name; case "getUID" -> id;
            case "getBlockAt" -> proxy(Block.class,(b,bm,ba)->bm.getName().equals("getLocation")
                ? (a[0] instanceof Location ? ((Location)a[0]).clone() : new Location((World)w,(int)a[0],(int)a[1],(int)a[2])) : fallback(b,bm,ba));
            default -> fallback(w,m,a);
        });
    }
    static Object fallback(Object p,Method m,Object[] a) {
        if(m.getName().equals("equals"))return p==a[0];
        if(m.getName().equals("hashCode"))return System.identityHashCode(p);
        if(m.getName().equals("toString"))return "fake-"+m.getDeclaringClass().getSimpleName();
        if(m.getReturnType()==boolean.class)return false;
        if(m.getReturnType()==int.class)return 0;
        if(m.getReturnType()==long.class)return 0L;
        return null;
    }
    @SuppressWarnings("unchecked") static <T> T proxy(Class<T> type,InvocationHandler h) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},h);
    }
    static Field field(Class<?> type,String name)throws Exception {
        Field f=type.getDeclaredField(name);f.setAccessible(true);return f;
    }
    static void invoke(Object service,String name)throws Exception {
        Method m=service.getClass().getDeclaredMethod(name);m.setAccessible(true);m.invoke(service);
    }
    static Object container(ProtectionParticlesService s,UUID id)throws Exception {
        return ((Map<?,?>)field(s.getClass(),"containers").get(s)).get(id);
    }
    static Task latest(){return tasks.get(tasks.size()-1);}
    static long live(){return tasks.stream().filter(t->!t.cancelled).count();}
    static void require(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        BukkitScheduler scheduler=proxy(BukkitScheduler.class,(p,m,a)->{
            if(m.getName().contains("Asynchronously"))throw new AssertionError("Bukkit task was async");
            if(m.getName().equals("runTaskTimer"))return new Task((Runnable)a[1]);
            return fallback(p,m,a);
        });
        PluginManager manager=proxy(PluginManager.class,(p,m,a)->{
            if(m.getName().equals("registerEvents"))registrations++;
            return fallback(p,m,a);
        });
        field(Bukkit.class,"server").set(null,proxy(Server.class,(p,m,a)->switch(m.getName()) {
            case "getScheduler" -> scheduler;case "getPluginManager" -> manager;
            case "getPlayer" -> online.get(a[0]);case "getLogger" -> Logger.getLogger("view-test");
            case "getBukkitVersion" -> "26.2-R0.1-SNAPSHOT";
            case "getVersion" -> "Leaf (MC: 26.2)";case "getName" -> "Leaf";
            default -> fallback(p,m,a);
        }));
        // Bypass JavaPlugin's real server loader. No lifecycle or persistent storage is used.
        Field uf=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");uf.setAccessible(true);
        Object unsafe=uf.get(null);
        MainPluginClass plugin=(MainPluginClass)unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,MainPluginClass.class);
        field(JavaPlugin.class,"isEnabled").set(plugin,true);
        field(TasksUtils.class,"plugin").set(null,plugin);
        RoyaleProtectionBlocksAPIImpl api=new RoyaleProtectionBlocksAPIImpl();
        field(RoyaleProtectionBlocksAPIImpl.class,"protectionsService").set(api,new Regions());
        ProtectionParticlesService service=new ProtectionParticlesService();
        field(service.getClass(),"plugin").set(service,plugin);
        invoke(service,"load");
        require(live()==1&&registrations==1,"one expiry checker and listener registration");
        World first=world("first"),second=world("second");
        Session session=new Session(UUID.randomUUID(),first);
        require(MessageTemplate.getMessageBuilder().getClass().getName().contains("NMS.Modern"),"26.2 selects correct channel builder");
        MessageTemplate.inst("Activation probe").process().sendActionBar(session.player);
        require(notices==0 && bars==1,"26.2 action bar is not delivered to chat");
        notices=0;
        service.toggleView(session.player,false); Task original=latest();
        require(service.isActive(session.player)&&live()==2&&notices==1,"activation once");
        for(int i=0;i<200;i++)original.tick();
        require(notices==1&&!original.cancelled,"200 refresh ticks never repeat notice; view stays active");
        int before=searches;session.world=second;
        service.onPlayerChangedWorld(new PlayerChangedWorldEvent(session.player,first));original.tick();
        Object c=container(service,session.id);
        require(((Location)field(c.getClass(),"currentLocation").get(c)).getWorld()==second,"world cache refreshed");
        require(searches>before&&live()==2&&latest()==original&&notices==1,"world change keeps same task without notice");
        service.toggleView(session.player,false);
        require(original.cancelled&&!service.isActive(session.player)&&live()==1,"second command deactivates");
        for(int i=0;i<50;i++)service.toggleView(session.player,false);
        require(live()==1&&!service.isActive(session.player)&&notices==26,"50 repeated commands leave no duplicates");
        service.toggleView(session.player,false);Task quitting=latest();
        service.onPlayerQuit(new PlayerQuitEvent(session.player,""));session.connected=false;online.remove(session.id);
        require(quitting.cancelled&&live()==1,"quit immediately cancels task");
        Session reconnected=new Session(session.id,second);
        require(!service.isActive(reconnected.player),"reconnect starts inactive");
        service.toggleView(reconnected.player,false);Task replacement=latest();
        quitting.runnable.run();
        require(service.isActive(reconnected.player)&&!replacement.cancelled,"stale callback cannot cancel replacement");
        // Reconnect without a quit event must also invalidate the old session.
        Session silentReconnect=new Session(session.id,first);replacement.tick();
        require(replacement.cancelled&&!service.isActive(silentReconnect.player),"session identity guard");
        service.toggleView(silentReconnect.player,false);Task expiring=latest();
        c=container(service,session.id);field(c.getClass(),"taskStart").set(c,System.currentTimeMillis()-61000);
        tasks.get(0).tick();
        require(expiring.cancelled&&live()==1&&!service.isActive(silentReconnect.player),"60 second expiry");
        service.toggleView(silentReconnect.player,true);Task unloading=latest();invoke(service,"reload");
        require(unloading.cancelled&&live()==1&&!service.isActive(silentReconnect.player),"reload cancels views and replaces checker");
        service.toggleView(silentReconnect.player,false);Task noChecker=latest();
        field(service.getClass(),"checkerTask").set(service,null);invoke(service,"unload");
        require(noChecker.cancelled&&!service.isActive(silentReconnect.player),"unload clears views even without checker");
        tasks.forEach(Task::cancel);
        require(live()==0,"no residual tasks");
        System.out.println("ProtectionViewLifecycleTest: "+checks+" checks passed on actual service (simulated Leaf 26.2)");
    }
}
