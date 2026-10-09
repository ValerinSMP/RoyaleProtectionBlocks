package company.pluginName.Bukkit.Inventories.Shared;

/** Reject delayed search results after a new request, disconnect, deletion or permission change. */
public final class SearchResultGuard {
    private SearchResultGuard() { }

    public static boolean isCurrent(long request, long latest, boolean sameSession,
            boolean online, boolean sameProtection, boolean allowed) {
        return request == latest && sameSession && online && sameProtection && allowed;
    }
}
