package company.pluginName.Hooks.Squaremap;

import java.util.*;
import xyz.jpenilla.squaremap.api.*;

public final class SquaremapRendererTest {
    private static int tests;
    private static void check(boolean value) { tests++; if (!value) throw new AssertionError("test " + tests); }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        var renderer = new SquaremapRenderer("Protecciones");
        var layer = SimpleLayerProvider.builder("test").build();
        var field = SquaremapRenderer.class.getDeclaredField("layers"); field.setAccessible(true);
        ((Map<String,Object[]>)field.get(renderer)).put("world", new Object[]{null,layer});
        var first = ClaimOutline.clipped("a","world","Primera",0,0,10,10,0,0,100);
        var second = ClaimOutline.clipped("b","world","Segunda",10,10,20,20,0,0,100);
        renderer.reconcile(Map.of(first.key(),first,second.key(),second));
        check(renderer.drain(1)==1 && renderer.busy() && layer.registeredMarkers().size()==1);
        try { renderer.reconcile(Map.of()); throw new AssertionError("overlapping batch accepted"); }
        catch (IllegalStateException expected) { tests++; }
        check(renderer.drain(1)==1 && !renderer.busy() && layer.registeredMarkers().size()==2);
        var original = layer.registeredMarkers().get(Key.of(first.key()));
        renderer.reconcile(Map.of(first.key(),first,second.key(),second));
        check(!renderer.busy() && layer.registeredMarkers().get(Key.of(first.key()))==original);
        var renamed = ClaimOutline.clipped("a","world","Renombrada",0,0,10,10,0,0,100);
        renderer.reconcile(Map.of(renamed.key(),renamed));
        check(renderer.drain(25)==2 && layer.registeredMarkers().size()==1);
        check(layer.registeredMarkers().get(Key.of(first.key()))!=original);
        renderer.reconcile(Map.of()); renderer.drain(25);
        check(layer.registeredMarkers().isEmpty());
        System.out.println("SquaremapRendererTest: " + tests + " comprobaciones OK");
    }
}
