package company.pluginName.Modules.ProtectionsPckg.Utils;

import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/** The visible per-role permission is authoritative after an explicit PvP edit. */
public final class ProtectionPvpBridge {
    private ProtectionPvpBridge() {}

    public static void synchronize(ProtectedRegion region, boolean visitors, boolean members, boolean owners) {
        if (region == null) return;
        // WorldGuard cannot express arbitrary PB roles. The existing PB damage guards
        // enforce both participants; WG must not add a second hidden denial.
        region.setFlag(Flags.PVP, visitors || members || owners ? State.ALLOW : State.DENY);
        region.setFlag(Flags.PVP.getRegionGroupFlag(), RegionGroup.ALL);
    }
}
