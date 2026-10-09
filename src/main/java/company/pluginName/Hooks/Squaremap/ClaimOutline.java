package company.pluginName.Hooks.Squaremap;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Immutable public projection. Contains no owner or protection-block location. */
public record ClaimOutline(String key, String world, String label,
                           double minX, double minZ, double maxX, double maxZ) {
    public static ClaimOutline clipped(String id, String world, String label,
                                       double minX, double minZ, double maxX, double maxZ,
                                       double centerX, double centerZ, double size) {
        double half = size / 2;
        minX = Math.max(minX, centerX - half);
        minZ = Math.max(minZ, centerZ - half);
        maxX = Math.min(maxX, centerX + half);
        maxZ = Math.min(maxZ, centerZ + half);
        if (!(Double.isFinite(minX) && Double.isFinite(minZ) && Double.isFinite(maxX)
                && Double.isFinite(maxZ) && minX < maxX && minZ < maxZ)) return null;
        String key = "pb_" + UUID.nameUUIDFromBytes((world + "\0" + id).getBytes(StandardCharsets.UTF_8));
        return new ClaimOutline(key, world, html(label), minX, minZ, maxX, maxZ);
    }

    static String html(String text) {
        if (text == null || text.isBlank()) return "Protección";
        // Bound tooltip size, strip controls and escape HTML rather than interpreting it.
        text = text.replaceAll("[\\p{Cntrl}]", "");
        if (text.length() > 160) text = text.substring(0, 160);
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
