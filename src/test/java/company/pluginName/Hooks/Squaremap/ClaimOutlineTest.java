package company.pluginName.Hooks.Squaremap;

public final class ClaimOutlineTest {
    private static int tests;
    private static void check(boolean value) { tests++; if (!value) throw new AssertionError("test " + tests); }
    public static void main(String[] args) {
        var claim = ClaimOutline.clipped("secret", "world", "<script> & ' \"", -16, -16, 17, 17, 0, 0, 100);
        check(claim.minX() == -16 && claim.maxX() == 17); // 33 blocks, upper bound exclusive
        check(!claim.label().contains("<script>") && claim.label().contains("&lt;script&gt;"));
        check(!claim.key().contains("secret"));
        check(claim.equals(ClaimOutline.clipped("secret", "world", "<script> & ' \"", -16,-16,17,17,0,0,100)));
        check(!claim.key().equals(ClaimOutline.clipped("secret","other","x",-16,-16,17,17,0,0,100).key()));
        check(ClaimOutline.clipped("a","world","x",60,60,70,70,0,0,100) == null);
        var edge = ClaimOutline.clipped("a","world",null,45,45,65,65,0,0,100);
        check(edge.maxX() == 50 && edge.maxZ() == 50 && edge.label().equals("Protección"));
        check(ClaimOutline.clipped("a","world","x",50,0,60,1,0,0,100) == null);
        check(ClaimOutline.clipped("a","world","x",Double.NaN,0,1,1,0,0,100) == null);
        check(ClaimOutline.html("a\nb").equals("ab"));
        check(ClaimOutline.html("x".repeat(500)).length() == 160);
        System.out.println("ClaimOutlineTest: " + tests + " comprobaciones OK");
    }
}
