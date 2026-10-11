package me.cortex.voxy.client.core.rendering;

import me.cortex.voxy.client.core.util.IrisUtil;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;


import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ViewportSelector <T extends Viewport<?>> {
    public static final boolean VIVECRAFT_INSTALLED = ModList.get().isLoaded("vivecraft");

    private final Supplier<T> creator;
    private final T defaultViewport;
    private final Map<Object, T> extraViewports = new HashMap<>();

    public ViewportSelector(Supplier<T> viewportCreator) {
        this.creator = viewportCreator;
        this.defaultViewport = viewportCreator.get();
    }

    private T getOrCreate(Object holder) {
        return this.extraViewports.computeIfAbsent(holder, a->this.creator.get());
    }

    private T getVivecraftViewport() {
        try {
            var type = Class.forName("org.vivecraft.client_vr.ClientDataHolderVR");
            var holder = type.getMethod("getInstance").invoke(null);
            var pass = type.getField("currentPass").get(holder);
            return pass == null ? this.defaultViewport : this.getOrCreate(pass);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unsupported Vivecraft rendering API", e);
        }
    }

    private static final Object IRIS_SHADOW_OBJECT = new Object();
    public T getViewport() {
        if (VIVECRAFT_INSTALLED) {
            return getVivecraftViewport();
        }

        if (IrisUtil.irisShadowActive()) {
            return this.getOrCreate(IRIS_SHADOW_OBJECT);
        }
        return this.defaultViewport;
    }

    public void free() {
        this.defaultViewport.delete();
        this.extraViewports.values().forEach(Viewport::delete);
        this.extraViewports.clear();
    }
}
