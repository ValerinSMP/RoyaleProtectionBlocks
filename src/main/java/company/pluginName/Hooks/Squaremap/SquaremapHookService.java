package company.pluginName.Hooks.Squaremap;

import company.pluginName.MainPluginClass;
import company.pluginName.Modules.ProtectionsPckg.ProtectionsServiceImpl;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.*;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.*;
import org.bukkit.event.server.*;
import org.bukkit.scheduler.BukkitTask;
import royale.RoyaleProtectionBlocks.Plugin.API.RoyaleProtectionBlocksAPI;
import royale.RoyaleProtectionBlocks.Plugin.API.Events.Protection.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@PandaService(priority = 1100)
public final class SquaremapHookService implements Listener {
    @PandaInject private MainPluginClass plugin;
    private OutlineRenderer renderer;
    private BukkitTask task;
    private final AtomicBoolean dirty = new AtomicBoolean(true);
    private Set<String> worlds = Set.of();
    private Set<String> excluded = Set.of();
    private int interval, batch, ticks;
    private boolean debug;
    private String title;
    private long lastWarning;

    @LoadMethod public void load() {
        try {
            File file = new File(plugin.getDataFolder(), "squaremap.yml");
            if (!file.exists()) plugin.saveResource("squaremap.yml", false);
            var config = YamlConfiguration.loadConfiguration(file);
            if (!config.getBoolean("enabled", true)) return;
            worlds = Set.copyOf(config.getStringList("worlds"));
            excluded = Set.copyOf(config.getStringList("exclude-protection-ids"));
            interval = Math.max(5, Math.min(3600, config.getInt("refresh-seconds", 30))) * 20;
            batch = Math.max(1, Math.min(200, config.getInt("markers-per-tick", 25)));
            debug = config.getBoolean("debug", false);
            title = config.getString("layer-name", "Protecciones");
            Bukkit.getPluginManager().registerEvents(this, plugin);
            for (Class<? extends Event> type : List.of(ProtectionCreationEvent.class,
                    ProtectionRemovalEvent.class, ProtectionMergeEvent.class, ProtectionSplitEvent.class)) {
                Bukkit.getPluginManager().registerEvent(type, this, EventPriority.MONITOR,
                        (listener, event) -> dirty.set(true), plugin, true);
            }
            start();
        } catch (RuntimeException | LinkageError ex) { warning(ex); unload(); }
    }
    private void start() {
        if (task != null || !Bukkit.getPluginManager().isPluginEnabled("squaremap")) return;
        renderer = new SquaremapRenderer(title);
        dirty.set(true); ticks = interval;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20, 1);
        plugin.getLogger().info("Squaremap: hook habilitado; límites y nombres, sin propietarios.");
    }
    private void tick() {
        try {
            ticks++;
            if (renderer.busy()) { renderer.drain(batch); return; }
            if (!dirty.getAndSet(false) && ticks < interval) return;
            ticks = 0;
            var desired = new LinkedHashMap<String, ClaimOutline>();
            var visible = new HashSet<String>();
            for (String name : worlds) {
                var world = Bukkit.getWorld(name);
                if (world != null && renderer.attach(world)) visible.add(name);
            }
            // Snapshot first: a failed cache read must never erase the last valid public layer.
            var api = RoyaleProtectionBlocksAPI.getInstance();
            if (api == null) throw new IllegalStateException("API de protecciones no disponible");
            // The public interface has no all-claims method; this internal cache accessor does.
            var cache = (ProtectionsServiceImpl) api.getProtectionsService();
            try (var stream = cache.findAllProtections()) {
                for (var claim : stream.toList()) {
                    if (!visible.contains(claim.getWorldName()) || claim.isDeleted()
                            || claim.isCreationInProgress() || excluded.contains(claim.getProtectionId())) continue;
                    var world = Bukkit.getWorld(claim.getWorldName());
                    var border = world.getWorldBorder();
                    var area = claim.getProtectionArea();
                    var min = area.getMinLocation(); var max = area.getMaxLocation();
                    var outline = ClaimOutline.clipped(claim.getProtectionId(), claim.getWorldName(),
                            claim.getDisplayNameWithoutFormat(), min.getX(), min.getZ(), max.getX(), max.getZ(),
                            border.getCenter().getX(), border.getCenter().getZ(), border.getSize());
                    if (outline != null) desired.put(outline.key(), outline);
                }
            }
            renderer.reconcile(desired);
            int changed = renderer.drain(batch);
            if (debug) plugin.getLogger().info("Squaremap: claims=" + desired.size() + " primer-lote=" + changed);
        } catch (RuntimeException | LinkageError ex) {
            warning(ex);
            // Freeze the last successful layer; only explicit reload/unload removes it.
            if (task != null) { task.cancel(); task = null; }
        }
    }
    private void warning(Throwable ex) {
        long now = System.currentTimeMillis();
        if (now - lastWarning > 60000) {
            lastWarning = now;
            plugin.getLogger().warning("Squaremap: hook detenido sin modificar protecciones. "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }
    @EventHandler public void enabled(PluginEnableEvent event) {
        if (event.getPlugin().getName().equalsIgnoreCase("squaremap")) {
            try { start(); } catch (RuntimeException | LinkageError ex) { warning(ex); }
        }
    }
    @EventHandler public void disabled(PluginDisableEvent event) {
        if (event.getPlugin().getName().equalsIgnoreCase("squaremap")) stop();
    }
    private void stop() {
        if (task != null) { task.cancel(); task = null; }
        if (renderer != null) {
            try { renderer.close(); } catch (RuntimeException | LinkageError ex) { warning(ex); }
            renderer = null;
        }
    }
    @UnloadMethod public void unload() { stop(); HandlerList.unregisterAll(this); }
    @ReloadMethod public void reload() { unload(); load(); }
}
