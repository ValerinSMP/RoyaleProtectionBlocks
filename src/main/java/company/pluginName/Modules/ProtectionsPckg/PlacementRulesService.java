package company.pluginName.Modules.ProtectionsPckg;

import company.pluginName.MainPluginClass;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.LoadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.ReloadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.UnloadMethod;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.*;
import org.bukkit.block.EndGateway;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.*;
import royale.RoyaleProtectionBlocks.Plugin.API.Events.Protection.ProtectionCreationAttemptEvent;

/** Fail-closed placement checks; no disk access or chunk loading in the event handler. */
@PandaService(priority = 1000)
public class PlacementRulesService implements Listener {
    @PandaInject private MainPluginClass plugin;
    private final AtomicLong generation = new AtomicLong();
    private volatile Snapshot snapshot;
    private record Rule(Set<String> worlds, double x, double z, double radius, double gatewayDistance) {}
    private record Snapshot(Map<String, Rule> rules, int maximumChunks) {}

    @LoadMethod private void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        reload();
    }
    @ReloadMethod private void reload() {
        long ticket = generation.incrementAndGet();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                var path = plugin.getDataFolder().toPath().resolve("placement-rules.yml");
                Snapshot next;
                if (!Files.exists(path)) {
                    if (snapshot != null && !snapshot.rules().isEmpty()) throw new IllegalArgumentException("Missing rules file");
                    next = new Snapshot(Map.of(), 128);
                }
                else {
                    if (Files.size(path) > 65536) throw new IllegalArgumentException("File too large");
                    var yaml = new YamlConfiguration();
                    yaml.loadFromString(Files.readString(path));
                    if (yaml.getInt("schema-version") != 1) throw new IllegalArgumentException("Invalid schema");
                    int limit = yaml.getInt("maximum-check-chunks", 128);
                    if (limit < 1 || limit > 256) throw new IllegalArgumentException("Invalid chunk limit");
                    var section = yaml.getConfigurationSection("rules");
                    if (section == null || section.getKeys(false).size() > 64)
                        throw new IllegalArgumentException("Invalid rules");
                    Map<String, Rule> rules = new HashMap<>();
                    for (String id : section.getKeys(false)) {
                        var entry = section.getConfigurationSection(id);
                        if (entry == null || !id.matches("[a-z0-9_-]{1,64}")) throw new IllegalArgumentException("Invalid rule ID");
                        Set<String> worlds = Set.copyOf(entry.getStringList("worlds"));
                        if (worlds.isEmpty() || worlds.size() > 16) throw new IllegalArgumentException("Invalid worlds");
                        double x = entry.getDouble("main-island.center-x", Double.NaN);
                        double z = entry.getDouble("main-island.center-z", Double.NaN);
                        double radius = entry.getDouble("main-island.radius", Double.NaN);
                        double distance = entry.getDouble("gateway-distance", Double.NaN);
                        new PlacementGeometry.Footprint(0, 0, 0, 0).intersects(x, z, radius);
                        if (!Double.isFinite(distance) || distance < 0 || distance > 256 || radius > 30000000)
                            throw new IllegalArgumentException("Invalid distance");
                        rules.put(id, new Rule(worlds, x, z, radius, distance));
                    }
                    next = new Snapshot(Map.copyOf(rules), limit);
                }
                synchronized (this) {
                    if (generation.get() == ticket) snapshot = next;
                }
            } catch (Exception error) {
                // Preserve the last validated snapshot. Never print file contents or secrets.
                plugin.getLogger().warning("Placement rules rejected; previous restrictions retained (" + error.getClass().getSimpleName() + ")");
            }
        });
    }
    @UnloadMethod private synchronized void unload() {
        generation.incrementAndGet();
        HandlerList.unregisterAll(this);
        snapshot = null;
    }
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreation(ProtectionCreationAttemptEvent event) {
        Snapshot current = snapshot;
        if (current == null) { deny(event, "Las restricciones de protección todavía no están disponibles."); return; }
        Rule rule = current.rules().get(event.getProtection().getProtectionBlockId().toLowerCase(Locale.ROOT));
        if (rule == null) return;
        // Unexpected provider/world exceptions must not accidentally authorize placement.
        event.setCancelled(true);
        var protection = event.getProtection();
        Location location = protection.getBukkitLocation();
        World world = location.getWorld();
        if (world == null || !rule.worlds().contains(world.getName())) {
            deny(event, "Esta protección no se puede colocar en este mundo."); return;
        }
        var block = protection.getProtectionBlock();
        var area = new PlacementGeometry.Footprint(location.getBlockX() - block.getBlocksX(),
                location.getBlockZ() - block.getBlocksZ(), location.getBlockX() + block.getBlocksX() + 1.0,
                location.getBlockZ() + block.getBlocksZ() + 1.0);
        if (area.intersects(rule.x(), rule.z(), rule.radius())) {
            deny(event, "Toda la protección debe quedar fuera del radio de " + (int) rule.radius() + " bloques desde 0,0 del End."); return;
        }
        double distance = rule.gatewayDistance();
        // Circle-only rules are pure geometry, also valid for Royale's async creation event.
        if (distance == 0) { event.setCancelled(false); return; }
        if (!Bukkit.isPrimaryThread()) {
            deny(event, "No se pudo comprobar la zona de gateways de forma segura."); return;
        }
        if (area.chunks(distance) > current.maximumChunks()) {
            deny(event, "No se pudo comprobar de forma segura la zona de esta protección."); return;
        }
        // First verify coverage; never force-load or generate chunks for a placement attempt.
        for (int x = area.minChunkX(distance); x <= area.maxChunkX(distance); x++)
            for (int z = area.minChunkZ(distance); z <= area.maxChunkZ(distance); z++)
                if (!world.isChunkLoaded(x, z)) {
                    deny(event, "La zona cercana debe estar cargada para comprobar los gateways. Explórala y vuelve a intentar."); return;
                }
        for (int x = area.minChunkX(distance); x <= area.maxChunkX(distance); x++)
            for (int z = area.minChunkZ(distance); z <= area.maxChunkZ(distance); z++)
                for (var state : world.getChunkAt(x, z).getTileEntities(b -> b.getType() == Material.END_GATEWAY, false)) {
                    var gateway = (EndGateway) state;
                    Location source = gateway.getLocation(), exit = gateway.getExitLocation();
                    if (area.intersects(source.getX(), source.getZ(), distance)
                            || (exit != null && world.equals(exit.getWorld()) && area.intersects(exit.getX(), exit.getZ(), distance))) {
                        deny(event, "Toda la protección debe quedar a más de " + (int) distance + " bloques de los gateways y sus salidas."); return;
                    }
                }
        event.setCancelled(false);
    }
    private void deny(ProtectionCreationAttemptEvent event, String message) {
        event.setCancelled(true);
        if (event.getExecutor() == null) return;
        Runnable feedback = () -> event.getExecutor().sendMessage(net.kyori.adventure.text.Component.text(message,
                net.kyori.adventure.text.format.TextColor.color(0xFF3300)));
        if (Bukkit.isPrimaryThread()) feedback.run();
        else Bukkit.getScheduler().runTask(plugin, feedback);
    }
}
