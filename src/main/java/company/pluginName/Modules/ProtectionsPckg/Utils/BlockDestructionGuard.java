package company.pluginName.Modules.ProtectionsPckg.Utils;

/** Decisions shared by explosion and mob-block listeners; no world scans or tasks. */
public final class BlockDestructionGuard {
    private BlockDestructionGuard() { }

    public static boolean isTnt(String type) {
        return "TNT".equals(type) || "TNT_MINECART".equals(type)
                || "PRIMED_TNT".equals(type) || "MINECART_TNT".equals(type);
    }

    public static boolean isDestructiveMob(String type) {
        return switch(type) {
            case "WITHER", "ENDER_DRAGON", "ENDERMAN", "SILVERFISH", "RAVAGER", "RABBIT" -> true;
            default -> false;
        };
    }

    public static boolean protectExplosion(boolean protectedArea, boolean sourceBlock,
            boolean tnt, boolean tntSettingEnabled, boolean tntDenied) {
        return sourceBlock || protectedArea && (!tnt || tntSettingEnabled && tntDenied);
    }

    public static boolean protectMobChange(String type, boolean protectedArea, boolean sourceBlock) {
        return sourceBlock || protectedArea && isDestructiveMob(type);
    }
}
