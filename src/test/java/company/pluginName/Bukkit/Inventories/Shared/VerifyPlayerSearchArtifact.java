package company.pluginName.Bukkit.Inventories.Shared;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** All unrelated installed classes and resources must remain byte-for-byte identical. */
public final class VerifyPlayerSearchArtifact {
    public static void main(String[] args) throws Exception {
        int preserved = 0, changed = 0, added = 0;
        try (ZipFile base = new ZipFile(args[0]); ZipFile candidate = new ZipFile(args[1])) {
            Enumeration<? extends ZipEntry> entries = base.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                ZipEntry updated = candidate.getEntry(entry.getName());
                if (updated == null) throw new AssertionError("Removed " + entry.getName());
                if (!Arrays.equals(base.getInputStream(entry).readAllBytes(), candidate.getInputStream(updated).readAllBytes())) {
                    if (!allowed(entry.getName())) throw new AssertionError("Unrelated change " + entry.getName());
                    changed++;
                } else preserved++;
            }
            entries = candidate.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (base.getEntry(entry.getName()) == null && !entry.isDirectory()) {
                    if (!allowed(entry.getName())) throw new AssertionError("Unexpected addition " + entry.getName());
                    added++;
                }
            }
        }
        if (changed < 4 || added != 3) throw new AssertionError("Incomplete patch " + changed + "/" + added);
        System.out.println("Artifact verified: " + preserved + " entries preserved, " + changed + " changed, " + added + " added");
    }
    private static boolean allowed(String name) {
        return name.matches("royale/RoyaleProtectionBlocks/Plugin/Bukkit/Inventories/Protections/(Members/ProtectionMembersInventory|Banneds/ProtectionBannedsInventory)(\\$[^/]+)?\\.class")
            || name.equals("royale/RoyaleProtectionBlocks/Plugin/Bukkit/Inventories/Shared/SearchResultGuard.class")
            || name.equals("royale/RoyaleProtectionBlocks/Plugin/Modules/PlayersDataPckg/PlayerNameLookupService.class")
            || name.equals("royale/RoyaleProtectionBlocks/PandaUtils/Services/PandaCachedPlayersModule/Utils/AsyncNameLookup.class")
            || name.equals("royale/RoyaleProtectionBlocks/PandaUtils/Services/PandaCachedPlayersModule/Utils/OfflinePlayerUtilities.class")
            || name.equals("royale/RoyaleProtectionBlocks/PandaUtils/PandaUtilities/OfflinePlayerUtilities.class");
    }
}
