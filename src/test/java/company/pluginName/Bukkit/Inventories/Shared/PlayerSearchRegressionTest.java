package company.pluginName.Bukkit.Inventories.Shared;

import darkpanda73.PandaUtils.Services.PandaCachedPlayersModule.Utils.AsyncNameLookup;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/** Finite tests; no Minecraft process, database credentials or production state. */
public final class PlayerSearchRegressionTest {
    private static int checks;
    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Check " + checks);
    }
    public static void main(String[] args) {
        Queue<Runnable> tasks = new ArrayDeque<>();
        AtomicInteger calls = new AtomicInteger();
        AsyncNameLookup<String> lookup = new AsyncNameLookup<>(tasks::add, 1);
        CompletableFuture<String> first = lookup.lookup("Known_Player", name -> {
            calls.incrementAndGet(); return "shared-db-uuid";
        });
        check(!first.isDone() && calls.get() == 0); // no synchronous SQL
        CompletableFuture<String> duplicate = lookup.lookup("known_player", name -> "wrong");
        check(tasks.size() == 1); // case-insensitive single flight
        check(lookup.lookup("Another", name -> "other").isCompletedExceptionally());
        tasks.remove().run();
        check(first.join().equals("shared-db-uuid") && duplicate.join().equals(first.join()));
        check(calls.get() == 1);
        CompletableFuture<String> missing = lookup.lookup("Unknown", name -> null);
        tasks.remove().run();
        check(missing.join() == null);
        CompletableFuture<String> failure = lookup.lookup("Failed", name -> { throw new IllegalStateException(); });
        tasks.remove().run();
        check(failure.isCompletedExceptionally());
        CompletableFuture<String> retry = lookup.lookup("Failed", name -> "recovered");
        tasks.remove().run();
        check(retry.join().equals("recovered"));
        check(lookup.lookup("bad name", name -> "wrong").join() == null && tasks.isEmpty());
        CompletableFuture<String> bedrock = lookup.lookup(".Bedrock", name -> "bedrock-uuid");
        tasks.remove().run();
        check(bedrock.join().equals("bedrock-uuid"));
        CompletableFuture<String> cancelled = lookup.lookup("Closing", name -> { calls.incrementAndGet(); return "bad"; });
        lookup.close();
        tasks.remove().run();
        check(cancelled.isCompletedExceptionally() && calls.get() == 1);
        check(lookup.lookup("AfterClose", name -> "bad").isCompletedExceptionally());
        AsyncNameLookup<String> rejected = new AsyncNameLookup<>(task -> { throw new IllegalStateException(); }, 1);
        check(rejected.lookup("Player", name -> "bad").isCompletedExceptionally());
        rejected.close();
        check(SearchResultGuard.isCurrent(1, 1, true, true, true, true));
        check(!SearchResultGuard.isCurrent(1, 2, true, true, true, true));
        check(!SearchResultGuard.isCurrent(1, 1, false, true, true, true));
        check(!SearchResultGuard.isCurrent(1, 1, true, false, true, true));
        check(!SearchResultGuard.isCurrent(1, 1, true, true, false, true));
        check(!SearchResultGuard.isCurrent(1, 1, true, true, true, false));
        System.out.println("Player search regression: " + checks + " checks passed");
    }
}
