package company.pluginName.Modules.ProtectionBlocksPckg;

import java.util.List;

public final class PriceLoreTest {
    private static int checks;
    private static void equal(Object actual, Object expected) {
        checks++;
        if (!java.util.Objects.equals(actual, expected)) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        PriceLore modern = new PriceLore(true, "{price}", "", "");
        PriceLore legacy = new PriceLore(true, "{price}", "$", "⛃");
        var original = List.of("§7Área 30×30", "§ePrecio: ${price}⛃", "Permiso intacto");
        equal(modern.render(17500d, original), List.of(original.get(0), "§ePrecio: $17500⛃", original.get(2)));
        equal(original.get(1), "§ePrecio: ${price}⛃");
        equal(modern.render(399.5d, List.of("{price}")), List.of("399.5"));
        equal(modern.render(0d, List.of("{price}")), List.of("0"));
        equal(modern.render(1e7, List.of("{price}")), List.of("10000000"));
        for (double price : new double[]{15000, 35000, 70000, 120000, 200000, 12345.67}) {
            var result = legacy.render(price, List.of("§ePrecio: $5000⛃"));
            equal(result, legacy.render(price, result));
            equal(result.getFirst(), "§ePrecio: $" + java.math.BigDecimal.valueOf(price).stripTrailingZeros().toPlainString() + "⛃");
        }
        var customized = List.of("Área: 5000", "Precio: $5000", "Precio manual", "$5000 otro símbolo");
        equal(legacy.render(123d, customized), customized);
        equal(modern.render(123d, List.of("$5000⛃")), List.of("$5000⛃"));
        equal(new PriceLore(false, "{price}", "$", "⛃").render(123d, original), original);
        for (Double price : new Double[]{null, -1d, Double.NaN, Double.POSITIVE_INFINITY})
            equal(legacy.render(price, original), original);
        equal(legacy.render(123d, null), null);
        equal(new PriceLore(true, "<price>", "[", "]").render(9.5, List.of("[8] <price>")), List.of("[9.5] 9.5"));
        equal(new PriceLore(true, "{price}", "\\$", "$").render(7d, List.of("\\$8$")), List.of("\\$7$"));
        for (String[] bad : new String[][]{{"", "", ""}, {"{price}", "$", ""}, {"{price}", "", "⛃"}, {"{price}", "\n", "x"}}) {
            try { new PriceLore(true, bad[0], bad[1], bad[2]); throw new AssertionError("Accepted invalid config"); }
            catch (IllegalArgumentException expected) { checks++; }
        }
        System.out.println("PASS: " + checks + " price-lore checks (templates, arbitrary prices, compatibility, disabled, validation)");
    }
}
