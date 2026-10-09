package company.pluginName.Modules.ProtectionsPckg;

public final class PlacementGeometryTest {
    public static void main(String[] args) {
        check(new PlacementGeometry.Footprint(1193, -268, 1208, -253).intersects(0, 0, 1500));
        check(new PlacementGeometry.Footprint(1500, -7, 1515, 8).intersects(0, 0, 1500));
        check(!new PlacementGeometry.Footprint(1501, -7, 1516, 8).intersects(0, 0, 1500));
        check(!new PlacementGeometry.Footprint(-1516, -7, -1501, 8).intersects(0, 0, 1500));
        var boundary = new PlacementGeometry.Footprint(1024, -7, 1039, 8);
        check(boundary.intersects(0, 0, 1024));
        check(!new PlacementGeometry.Footprint(1025, -7, 1040, 8).intersects(0, 0, 1024));
        check(new PlacementGeometry.Footprint(-1039, -7, -1024, 8).intersects(0, 0, 1024));
        var area = new PlacementGeometry.Footprint(2000, 2000, 2015, 2015);
        check(area.intersects(1936, 2000, 64));
        check(!area.intersects(1935, 2000, 64));
        check(!area.intersects(1936, 1936, 64));
        check(area.intersects(2015, 2015, 0));
        check(area.chunks(64) <= 128);
        check(new PlacementGeometry.Footprint(-1, -1, 0, 0).minChunkX(0) == -1);
        check(new PlacementGeometry.Footprint(-1, -1, 0, 0).chunks(0) == 4);
        try { new PlacementGeometry.Footprint(Double.NaN, 0, 0, 0); throw new AssertionError(); }
        catch (IllegalArgumentException expected) {}
        System.out.println("PASS: placement footprint boundaries, negative chunks and scan bounds");
    }
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
}
