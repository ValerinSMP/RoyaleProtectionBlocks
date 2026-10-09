package company.pluginName.Hooks.Squaremap;

import java.util.Map;
import org.bukkit.World;

/** API-free boundary, so service discovery also works when squaremap is absent. */
interface OutlineRenderer {
    boolean attach(World world);
    void reconcile(Map<String, ClaimOutline> desired);
    boolean busy();
    int drain(int limit);
    void close();
}
