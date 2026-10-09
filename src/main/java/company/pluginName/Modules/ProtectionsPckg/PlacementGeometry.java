package company.pluginName.Modules.ProtectionsPckg;

/** Horizontal block footprint, including the protection border. No world access. */
public final class PlacementGeometry {
    private PlacementGeometry() {}
    public record Footprint(double minX, double minZ, double maxX, double maxZ) {
        public Footprint {
            if (!Double.isFinite(minX) || !Double.isFinite(minZ) || !Double.isFinite(maxX)
                    || !Double.isFinite(maxZ) || minX > maxX || minZ > maxZ)
                throw new IllegalArgumentException("Invalid footprint");
        }
        public boolean intersects(double x, double z, double radius) {
            if (!Double.isFinite(x) || !Double.isFinite(z) || !Double.isFinite(radius) || radius < 0)
                throw new IllegalArgumentException("Invalid exclusion");
            double dx = Math.max(Math.max(minX - x, x - maxX), 0);
            double dz = Math.max(Math.max(minZ - z, z - maxZ), 0);
            return dx * dx + dz * dz <= radius * radius;
        }
        public int minChunkX(double margin) { return chunk(minX - margin); }
        public int maxChunkX(double margin) { return chunk(maxX + margin); }
        public int minChunkZ(double margin) { return chunk(minZ - margin); }
        public int maxChunkZ(double margin) { return chunk(maxZ + margin); }
        public long chunks(double margin) {
            return ((long) maxChunkX(margin) - minChunkX(margin) + 1)
                    * ((long) maxChunkZ(margin) - minChunkZ(margin) + 1);
        }
        private static int chunk(double coordinate) {
            double value = Math.floor(coordinate / 16);
            if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE)
                throw new IllegalArgumentException("Chunk coordinate out of range");
            return (int) value;
        }
    }
}
