package me.cortex.voxy.neoforge;

import com.mojang.blaze3d.systems.RenderSystem;

/** Snapshot of the fog uniforms used by Minecraft 1.21.1. */
public record FogParameters(float environmentalStart, float environmentalEnd,
                            float red, float green, float blue) {
    public static FogParameters capture() {
        var color = RenderSystem.getShaderFogColor();
        return new FogParameters(RenderSystem.getShaderFogStart(), RenderSystem.getShaderFogEnd(),
                color[0], color[1], color[2]);
    }
}
