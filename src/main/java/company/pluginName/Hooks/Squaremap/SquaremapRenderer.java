package company.pluginName.Hooks.Squaremap;

import java.awt.Color;
import java.util.*;
import org.bukkit.World;
import xyz.jpenilla.squaremap.api.*;
import xyz.jpenilla.squaremap.api.marker.*;

/** Optional API boundary: API types occur only in method bodies, not service metadata. */
final class SquaremapRenderer implements OutlineRenderer {
    private final Map<String, Object[]> layers = new HashMap<>();
    private final Map<String, ClaimOutline> displayed = new HashMap<>();
    private final ArrayDeque<Runnable> pending = new ArrayDeque<>();
    private final String title;
    SquaremapRenderer(String title) { this.title = title; }

    public boolean attach(World world) {
        var map = SquaremapProvider.get().getWorldIfEnabled(BukkitAdapter.worldIdentifier(world));
        if (map.isEmpty()) return false;
        var registry = map.get().layerRegistry();
        var key = Key.of("valerin_protecciones");
        Object[] existing = layers.get(world.getName());
        if (existing != null && existing[0] == map.get() && registry.hasEntry(key)
                && registry.get(key) == existing[1]) return true;
        if (registry.hasEntry(key)) throw new IllegalStateException("La capa valerin_protecciones ya tiene otro propietario");
        var layer = SimpleLayerProvider.builder(title).showControls(true).defaultHidden(false)
                .layerPriority(20).zIndex(20).build();
        registry.register(key, layer);
        layers.put(world.getName(), new Object[]{map.get(), layer});
        displayed.entrySet().removeIf(e -> e.getValue().world().equals(world.getName()));
        return true;
    }

    public void reconcile(Map<String, ClaimOutline> desired) {
        if (!pending.isEmpty()) throw new IllegalStateException("Lote anterior sin terminar");
        for (var old : List.copyOf(displayed.values())) {
            if (!desired.containsKey(old.key())) pending.add(() -> {
                var state = layers.get(old.world());
                if (state != null) ((SimpleLayerProvider) state[1]).removeMarker(Key.of(old.key()));
                displayed.remove(old.key());
            });
        }
        for (var outline : desired.values()) {
            if (outline.equals(displayed.get(outline.key()))) continue;
            pending.add(() -> {
                var state = layers.get(outline.world());
                if (state == null) return;
                var marker = Marker.rectangle(Point.of(outline.minX(), outline.minZ()),
                        Point.of(outline.maxX(), outline.maxZ()));
                marker.markerOptions(MarkerOptions.builder().stroke(true).strokeColor(new Color(255, 209, 102))
                        .strokeWeight(2).strokeOpacity(0.85).fill(true).fillColor(new Color(255, 209, 102))
                        .fillOpacity(0.12).hoverTooltip(outline.label()).clickTooltip(outline.label()).build());
                var layer = (SimpleLayerProvider) state[1];
                // addMarker replaces an existing key in squaremap's concurrent marker map.
                layer.addMarker(Key.of(outline.key()), marker);
                displayed.put(outline.key(), outline);
            });
        }
    }

    public boolean busy() { return !pending.isEmpty(); }
    public int drain(int limit) {
        int count = 0;
        while (count < limit && !pending.isEmpty()) { pending.getFirst().run(); pending.removeFirst(); count++; }
        return count;
    }
    public void close() {
        pending.clear();
        for (var state : layers.values()) {
            var registry = ((MapWorld) state[0]).layerRegistry();
            var key = Key.of("valerin_protecciones");
            if (registry.hasEntry(key) && registry.get(key) == state[1]) registry.unregister(key);
        }
        layers.clear(); displayed.clear();
    }
}
