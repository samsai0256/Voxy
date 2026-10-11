package me.cortex.voxy.client.mixin.minecraft;

import com.mojang.blaze3d.systems.RenderSystem;
import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void voxy$adjustFog(Camera camera, FogRenderer.FogMode mode,
                                       float distance, boolean thickFog, float partialTick,
                                       CallbackInfo ci) {
        if (mode != FogRenderer.FogMode.FOG_TERRAIN || VoxyConfig.CONFIG.renderVanillaFog) return;
        var renderer = (IGetVoxyRenderSystem) Minecraft.getInstance().levelRenderer;
        if (renderer == null || renderer.getVoxyRenderSystem() == null) return;
        if (VoxyConfig.CONFIG.useEnvironmentalFog &&
                (thickFog || camera.getFluidInCamera() != FogType.NONE ||
                 camera.getEntity() instanceof net.minecraft.world.entity.LivingEntity entity &&
                 (entity.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) ||
                  entity.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS)))) return;
        RenderSystem.setShaderFogStart(99999999);
        RenderSystem.setShaderFogEnd(999999999);
    }
}
