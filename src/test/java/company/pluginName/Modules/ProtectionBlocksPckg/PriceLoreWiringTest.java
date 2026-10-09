package company.pluginName.Modules.ProtectionBlocksPckg;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/** Runs against the actual shaded candidate. No server, database or economic operation. */
public final class PriceLoreWiringTest {
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void main(String[] args) throws Exception {
        var type = Class.forName("royale.RoyaleProtectionBlocks.Plugin.Modules.ProtectionBlocksPckg.ProtectionBlocksService");
        String[] fields = {"PRICE_LORE_ENABLED", "PRICE_LORE_PLACEHOLDER", "PRICE_LORE_LEGACY_PREFIX", "PRICE_LORE_LEGACY_SUFFIX"};
        String[] suffixes = {"Enabled", "Placeholder", "Legacy-prefix", "Legacy-suffix"};
        for (int i = 0; i < fields.length; i++) {
            var field = type.getDeclaredField(fields[i]);
            field.setAccessible(true);
            var configField = field.get(null);
            String path = (String) configField.getClass().getMethod("getPath").invoke(configField);
            if (!path.equals("Protection-block-presentation.Price-lore." + suffixes[i])) throw new AssertionError(path);
            var annotation = java.util.Arrays.stream(field.getAnnotations())
                    .filter(a -> a.annotationType().getSimpleName().equals("RegisteredPandaField"))
                    .findFirst().orElseThrow();
            if (!annotation.annotationType().getMethod("value").invoke(annotation).equals("config"))
                throw new AssertionError("Wrong configuration file");
        }
        Object service = type.getConstructor().newInstance();
        Map cache = (Map) type.getMethod("getProtectionBlocks").invoke(service);
        cache.put("retained", null);
        var field = type.getDeclaredField("PRICE_LORE_PLACEHOLDER");
        field.setAccessible(true);
        Object configField = field.get(null);
        var setter = configField.getClass().getMethod("setObjectContent", Object.class);
        Object previous = configField.getClass().getMethod("getContent").invoke(configField);
        var reload = type.getDeclaredMethod("reload");
        reload.setAccessible(true);
        try {
            setter.invoke(configField, "");
            try { reload.invoke(service); throw new AssertionError("Invalid config accepted"); }
            catch (InvocationTargetException expected) {
                if (!(expected.getCause() instanceof IllegalArgumentException)) throw expected;
            }
            if (!cache.containsKey("retained")) throw new AssertionError("Reload erased valid cache");
        } finally {
            setter.invoke(configField, previous);
        }
        System.out.println("PASS: shaded config registration and invalid reload preserves previous definitions");
    }
}
