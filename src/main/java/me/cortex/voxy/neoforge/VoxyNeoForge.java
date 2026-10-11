package me.cortex.voxy.neoforge;

import me.cortex.voxy.client.VoxyClient;
import me.cortex.voxy.client.config.ModMenuIntegration;
import me.cortex.voxy.commonImpl.VoxyCommon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "voxy", dist = Dist.CLIENT)
public final class VoxyNeoForge {
    public VoxyNeoForge(ModContainer container, IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(VoxyClient::initVoxyClient));
        NativeLibraries.initialize();
        // Initialize serialization before a world can create the storage backend.
        VoxyCommon.isAvailable();
        NeoForge.EVENT_BUS.addListener(VoxyClient::registerCommands);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> ModMenuIntegration.createScreen(parent));
    }
}
