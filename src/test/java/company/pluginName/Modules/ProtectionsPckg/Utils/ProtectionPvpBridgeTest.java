package company.pluginName.Modules.ProtectionsPckg.Utils;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;

public final class ProtectionPvpBridgeTest {
    public static void main(String[] args) {
        int checks = 0;
        for (int mask = 0; mask < 8; mask++) {
            var pb = region("pb");
            var unrelated = region("spawn");
            pb.setFlag(Flags.PVP, State.DENY);
            pb.setFlag(Flags.PVP.getRegionGroupFlag(), RegionGroup.NON_MEMBERS);
            pb.setFlag(Flags.TNT, State.DENY);
            unrelated.setFlag(Flags.PVP, State.DENY);
            ProtectionPvpBridge.synchronize(pb, (mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0);
            require(pb.getFlag(Flags.PVP) == (mask == 0 ? State.DENY : State.ALLOW), "roles " + mask);
            require(pb.getFlag(Flags.PVP.getRegionGroupFlag()) == RegionGroup.ALL, "stale group");
            require(pb.getFlag(Flags.TNT) == State.DENY, "TNT preserved");
            require(unrelated.getFlag(Flags.PVP) == State.DENY, "other region preserved");
            ProtectionPvpBridge.synchronize(pb, false, false, false);
            require(pb.getFlag(Flags.PVP) == State.DENY, "disable again");
            checks += 5;
        }
        ProtectionPvpBridge.synchronize(null, true, true, true);
        System.out.println("PB PvP bridge: " + (checks + 1) + " checks passed (real WG regions, no live server)");
    }
    private static ProtectedCuboidRegion region(String id) {
        return new ProtectedCuboidRegion(id, BlockVector3.at(0, 0, 0), BlockVector3.at(10, 10, 10));
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
