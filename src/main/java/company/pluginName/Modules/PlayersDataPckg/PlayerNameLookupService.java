package company.pluginName.Modules.PlayersDataPckg;

import company.pluginName.MainPluginClass;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaInject;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.LoadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Annotations.PandaService.UnloadMethod;
import darkpanda73.PandaUtils.PandaPlugin.Enums.LoadStep;
import darkpanda73.PandaUtils.PandaSQLModule.v2.Services.PandaSQLService;
import darkpanda73.PandaUtils.PandaSQLModule.v2.SQL.Objects.Statements.SelectStatement;
import darkpanda73.PandaUtils.PandaSQLModule.v2.SQL.Objects.Statements.Conditions.EqualsCondition;
import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.PandaCachedPlayersService;
import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.Objects.PandaCachedPlayer;
import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.Utils.AsyncNameLookup;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

/** GUI lookup; no scans, network requests, Bukkit access or main-thread locks in SQL work. */
@PandaService(loadOn = LoadStep.ENABLE, priority = -989)
public class PlayerNameLookupService {
    @PandaInject private MainPluginClass plugin;
    @PandaInject private PandaSQLService sqlService;
    private AsyncNameLookup<PandaCachedPlayer> lookup;

    @LoadMethod
    private void load() {
        lookup = new AsyncNameLookup<>(task -> Bukkit.getScheduler().runTaskAsynchronously(plugin, task), 32);
    }

    @UnloadMethod
    private void unload() { if (lookup != null) lookup.close(); }

    public CompletableFuture<PandaCachedPlayer> find(String name) {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Start lookup on main thread");
        if (name == null || !name.matches("[A-Za-z0-9_.]{1,32}")) return CompletableFuture.completedFuture(null);
        OfflinePlayer online = Bukkit.getPlayerExact(name);
        if (online != null) return CompletableFuture.completedFuture(
                new PandaCachedPlayer(online.getUniqueId(), online.getName()));
        // Read UUID from Paper's existing profile cache without reading playerdata/name.
        OfflinePlayer local = Bukkit.getOfflinePlayerIfCached(name);
        UUID localUuid = local == null ? null : local.getUniqueId();
        return lookup.lookup(name, queryName -> {
            try (ResultSet rows = sqlService.getSqlConnection().executeQuery(SelectStatement.inst()
                    .addTable(PandaCachedPlayersService.CACHED_PLAYERS_TABLE).setCondition(EqualsCondition.inst(
                            PandaCachedPlayersService.CACHED_PLAYERS_TABLE.getColumn("Name"), queryName)));
                    java.sql.Statement statement = rows.getStatement()) {
                if (rows.next()) return new PandaCachedPlayer(UUID.fromString(rows.getString("Uuid")), rows.getString("Name"));
            } catch (SQLException error) { throw new CompletionException(error); }
            return localUuid == null ? null : new PandaCachedPlayer(localUuid, queryName);
        });
    }
}
