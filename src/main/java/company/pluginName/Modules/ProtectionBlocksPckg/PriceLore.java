package company.pluginName.Modules.ProtectionBlocksPckg;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Presentation only. No block IDs, price overrides, persistence or economic operations. */
public final class PriceLore {
    private final boolean enabled;
    private final String placeholder;
    private final String prefix;
    private final String suffix;
    private final Pattern legacy;

    public PriceLore(boolean enabled, String placeholder, String prefix, String suffix) {
        if (placeholder == null || placeholder.isBlank() || placeholder.length() > 64
                || placeholder.contains("\n") || placeholder.contains("\r"))
            throw new IllegalArgumentException("Invalid price-lore placeholder");
        if (prefix == null || suffix == null || prefix.length() > 64 || suffix.length() > 64
                || prefix.contains("\n") || suffix.contains("\n") || prefix.contains("\r") || suffix.contains("\r"))
            throw new IllegalArgumentException("Invalid price-lore legacy delimiters");
        if (prefix.isEmpty() != suffix.isEmpty())
            throw new IllegalArgumentException("Both legacy delimiters must be set, or both empty");
        this.enabled = enabled;
        this.placeholder = placeholder;
        this.prefix = prefix;
        this.suffix = suffix;
        this.legacy = prefix.isEmpty() ? null
                : Pattern.compile(Pattern.quote(prefix) + "[0-9]+(?:\\.[0-9]+)?" + Pattern.quote(suffix));
    }

    public List<String> render(Double price, List<String> lore) {
        if (!enabled || lore == null || price == null || !Double.isFinite(price) || price < 0) return lore;
        String amount = BigDecimal.valueOf(price).stripTrailingZeros().toPlainString();
        return lore.stream().map(line -> {
            String rendered = line.replace(placeholder, amount);
            return legacy == null ? rendered : legacy.matcher(rendered)
                    .replaceAll(Matcher.quoteReplacement(prefix + amount + suffix));
        }).toList();
    }
}
