package company.pluginName.Modules.ProtectionBlocksPckg;

import company.pluginName.MainPluginClass;
import company.pluginName.Utils.ProtectionBlocksUtils;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.LoadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.UnloadMethod;
import darkpanda73.PandaUtils.PandaUtilities.ItemStack.ItemStackUtilities;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/** Explicit TEST fixture import. Bundled only in the TEST overlay, never production. */
@PandaService(priority = 1001)
public final class TestBlockImportService {
    @PandaInject private MainPluginClass plugin;
    @PandaInject private ProtectionBlocksService blocks;
    private final AtomicLong generation = new AtomicLong();

    @LoadMethod private void load() {
        long ticket = generation.incrementAndGet();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Path directory = plugin.getDataFolder().toPath().toAbsolutePath();
                Path file = directory.resolve("test-block-import.yml");
                if (!Files.isRegularFile(file)) return;
                if (Files.size(file) > 16384 || Files.isSymbolicLink(file)) throw new IllegalArgumentException();
                var yaml = new YamlConfiguration();
                yaml.loadFromString(Files.readString(file));
                if (!yaml.getBoolean("enabled", false)) return;
                var config = new YamlConfiguration();
                config.loadFromString(Files.readString(directory.resolve("Config.yml")));
                if (config.getBoolean("MySQL.Enable", true)) throw new IllegalArgumentException("SQLite-only fixture");
                Path database = directory.resolve("DB.sqlite");
                if (!Files.isRegularFile(database) || Files.isSymbolicLink(database)) throw new IllegalArgumentException();
                var section = yaml.getConfigurationSection("blocks");
                if (yaml.getInt("schema-version") != 1 || section == null || section.getKeys(false).isEmpty() || section.getKeys(false).size() > 8)
                    throw new IllegalArgumentException();
                List<Runnable> imports = new ArrayList<>();
                for (String id : section.getKeys(false)) {
                var entry = section.getConfigurationSection(id);
                if (!id.matches("[a-z0-9_-]{1,32}") || entry == null || entry.get("Price") != null
                        || entry.getStringList("Allowed-worlds").isEmpty()) throw new IllegalArgumentException();
                for (String dimension : List.of("Blocks-x", "Blocks-z")) {
                    int size = entry.getInt(dimension, -1);
                    if (size < 1 || size > 51 || size % 2 == 0) throw new IllegalArgumentException();
                }
                if (entry.getInt("Blocks-y", 0) != -1) throw new IllegalArgumentException();
                Map<String, Object> values = entry.getValues(true);
                imports.add(() -> prepare(ticket, database, id, values));
                }
                Bukkit.getScheduler().runTask(plugin, () -> imports.forEach(Runnable::run));
            } catch (Exception error) { report(error); }
        });
    }
    private void prepare(long ticket, Path database, String id, Map<String, Object> values) {
        if (generation.get() != ticket) return;
        try {
            // Existing definitions are never overwritten or duplicated by the fixture.
            if (blocks.getProtectionBlockById(id) != null) {
                plugin.getLogger().info("TEST protection definition already registered: " + id);
                return;
            }
            var block = ProtectionBlocksUtils.mapToProtectionBlock(id, values);
            if (block.getItem() == null || !block.getItem().getType().isBlock()) throw new IllegalArgumentException();
            byte[] item = ItemStackUtilities.itemsParse(new ItemStack[]{block.getItem()});
            if (item == null || item.length == 0) throw new IllegalArgumentException("Item serialization failed");
            int x = block.getBlocksX(), y = block.getBlocksY(), z = block.getBlocksZ();
            String permission = block.getPermission();
            Set<String> worlds = Set.copyOf(block.getAllowedWorlds());
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (generation.get() != ticket) return;
                try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database)) {
                    connection.setAutoCommit(false);
                    try {
                        try (var insert = connection.prepareStatement("INSERT INTO ProtectionBlocks (Id,Item,BlocksX,BlocksY,BlocksZ,Permission,Price) VALUES (?,?,?,?,?,?,NULL)")) {
                            insert.setString(1, id); insert.setBytes(2, item);
                            insert.setInt(3, x); insert.setInt(4, y); insert.setInt(5, z);
                            insert.setString(6, permission); insert.executeUpdate();
                        }
                        try (var insert = connection.prepareStatement("INSERT INTO ProtectionBlockAllowedWorlds (ProtectionBlockId,WorldName) VALUES (?,?)")) {
                            for (String world : worlds) {
                                insert.setString(1, id); insert.setString(2, world); insert.addBatch();
                            }
                            insert.executeBatch();
                        }
                        connection.commit();
                    } catch (Exception error) { connection.rollback(); throw error; }
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (generation.get() != ticket) return;
                        blocks.registerProtectionBlock(block);
                        plugin.getLogger().info("TEST protection definition registered: " + id + " (no money sale)");
                    });
                } catch (Exception error) { report(error); }
            });
        } catch (Exception error) { report(error); }
    }
    @UnloadMethod private void unload() { generation.incrementAndGet(); }
    private void report(Exception error) {
        plugin.getLogger().warning("TEST protection import failed; no existing definitions changed (" + error.getClass().getSimpleName() + ")");
    }
}
