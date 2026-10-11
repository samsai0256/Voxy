package me.cortex.voxy.client;

import me.cortex.voxy.client.core.gl.Capabilities;
import me.cortex.voxy.client.core.model.bakery.BudgetBufferRenderer;
import me.cortex.voxy.client.core.rendering.util.SharedIndexBuffer;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.commonImpl.VoxyCommon;

import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;


public class VoxyClient {



    public static void initVoxyClient() {
        Capabilities.init();//Ensure clinit is called

        boolean systemSupported = org.lwjgl.opengl.GL.getCapabilities().OpenGL46
                && Capabilities.INSTANCE.compute && Capabilities.INSTANCE.indirectParameters;
        if (systemSupported) {

            SharedIndexBuffer.INSTANCE.id();
            BudgetBufferRenderer.init();

            VoxyCommon.setInstanceFactory(VoxyClientInstance::new);

            if (!Capabilities.INSTANCE.subgroup) {
                Logger.warn("GPU does not support subgroup operations, expect some performance degradation");
            }

        } else {
            Logger.error("Voxy requires OpenGL 4.6, compute shaders and indirect draw parameters; LOD rendering is disabled on this system.");
        }
    }

    public static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(VoxyCommands.register());
    }

    public static boolean isFrexActive() {
        return net.caffeinemc.mods.sodium.client.util.FlawlessFrames.isActive();
    }
}
