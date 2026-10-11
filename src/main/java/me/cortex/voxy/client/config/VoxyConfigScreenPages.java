package me.cortex.voxy.client.config;

import me.cortex.voxy.client.RenderStatistics;
import me.cortex.voxy.client.VoxyClientInstance;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.common.util.cpu.CpuLayout;
import me.cortex.voxy.commonImpl.VoxyCommon;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.ConfigEntryPointForge;
import net.caffeinemc.mods.sodium.api.config.option.OptionFlag;
import net.caffeinemc.mods.sodium.api.config.option.OptionImpact;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Registers Voxy's settings through Sodium 0.8's public configuration API. */
@ConfigEntryPointForge("voxy")
public final class VoxyConfigScreenPages implements ConfigEntryPoint {
    private static final int SUBDIV_IN_MAX = 100;
    private static final double SUBDIV_MIN = 28;
    private static final double SUBDIV_MAX = 256;
    private static final double SUBDIV_CONST = Math.log(SUBDIV_MAX / SUBDIV_MIN) / Math.log(2);

    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        var storage = VoxyConfig.CONFIG;
        var page = builder.createOptionPage().setName(Component.translatable("voxy.config.title"));
        var general = builder.createOptionGroup();

        general.addOption(builder.createBooleanOption(id("enabled"))
                .setName(Component.translatable("voxy.config.general.enabled"))
                .setTooltip(Component.translatable("voxy.config.general.enabled.tooltip"))
                .setDefaultValue(true)
                .setStorageHandler(storage::save)
                .setBinding(value -> setEnabled(storage, value), () -> storage.enabled));
        general.addOption(builder.createIntegerOption(id("service_threads"))
                .setName(Component.translatable("voxy.config.general.serviceThreads"))
                .setTooltip(Component.translatable("voxy.config.general.serviceThreads.tooltip"))
                .setRange(1, CpuLayout.CORES.length, 1)
                .setValueFormatter(value -> Component.literal(Integer.toString(value)))
                .setDefaultValue(Math.max(1, (int) Math.max(CpuLayout.CORES.length / 1.5, 1)))
                .setImpact(OptionImpact.HIGH)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .setStorageHandler(storage::save)
                .setBinding(value -> setServiceThreads(storage, value), () -> storage.serviceThreads));
        general.addOption(builder.createBooleanOption(id("ingest"))
                .setName(Component.translatable("voxy.config.general.ingest"))
                .setTooltip(Component.translatable("voxy.config.general.ingest.tooltip"))
                .setDefaultValue(true)
                .setImpact(OptionImpact.MEDIUM)
                .setStorageHandler(storage::save)
                .setBinding(value -> storage.ingestEnabled = value, () -> storage.ingestEnabled));
        page.addOptionGroup(general);

        var rendering = builder.createOptionGroup();
        rendering.addOption(builder.createBooleanOption(id("rendering"))
                .setName(Component.translatable("voxy.config.general.rendering"))
                .setTooltip(Component.translatable("voxy.config.general.rendering.tooltip"))
                .setDefaultValue(true)
                .setImpact(OptionImpact.HIGH)
                .setStorageHandler(storage::save)
                .setBinding(value -> setRendering(storage, value), () -> storage.enableRendering));
        rendering.addOption(builder.createIntegerOption(id("subdivision_size"))
                .setName(Component.translatable("voxy.config.general.subDivisionSize"))
                .setTooltip(Component.translatable("voxy.config.general.subDivisionSize.tooltip"))
                .setRange(0, SUBDIV_IN_MAX, 1)
                .setValueFormatter(value -> Component.literal(Integer.toString(Math.round(ln2subDiv(value)))))
                .setDefaultValue(subDiv2ln(64))
                .setImpact(OptionImpact.HIGH)
                .setStorageHandler(storage::save)
                .setBinding(value -> storage.subDivisionSize = ln2subDiv(value), () -> subDiv2ln(storage.subDivisionSize)));
        rendering.addOption(builder.createIntegerOption(id("render_distance"))
                .setName(Component.translatable("voxy.config.general.renderDistance"))
                .setTooltip(Component.translatable("voxy.config.general.renderDistance.tooltip"))
                .setRange(2, 64, 1)
                .setValueFormatter(value -> Component.literal(Integer.toString(value * 32)))
                .setDefaultValue(16)
                .setImpact(OptionImpact.LOW)
                .setStorageHandler(storage::save)
                .setBinding(value -> setRenderDistance(storage, value), () -> storage.sectionRenderDistance));
        rendering.addOption(builder.createBooleanOption(id("environmental_fog"))
                .setName(Component.translatable("voxy.config.general.environmental_fog"))
                .setTooltip(Component.translatable("voxy.config.general.environmental_fog.tooltip"))
                .setDefaultValue(false)
                .setImpact(OptionImpact.VARIES)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .setStorageHandler(storage::save)
                .setBinding(value -> storage.useEnvironmentalFog = value, () -> storage.useEnvironmentalFog));
        rendering.addOption(builder.createBooleanOption(id("vanilla_fog"))
                .setName(Component.translatable("voxy.config.general.vanilla_fog"))
                .setTooltip(Component.translatable("voxy.config.general.vanilla_fog.tooltip"))
                .setDefaultValue(false)
                .setStorageHandler(storage::save)
                .setBinding(value -> storage.renderVanillaFog = value, () -> storage.renderVanillaFog));
        rendering.addOption(builder.createBooleanOption(id("render_statistics"))
                .setName(Component.translatable("voxy.config.general.render_statistics"))
                .setTooltip(Component.translatable("voxy.config.general.render_statistics.tooltip"))
                .setDefaultValue(false)
                .setFlags(OptionFlag.REQUIRES_RENDERER_RELOAD)
                .setStorageHandler(storage::save)
                .setBinding(value -> RenderStatistics.enabled = value, () -> RenderStatistics.enabled));
        page.addOptionGroup(rendering);

        builder.registerOwnModOptions().setName("Voxy").setVersion(VoxyCommon.MOD_VERSION)
                .setIcon(id("icon.png")).addPage(page);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("voxy", path);
    }

    private static void setEnabled(VoxyConfig storage, boolean value) {
        storage.enabled = value;
        var renderer = (IGetVoxyRenderSystem) Minecraft.getInstance().levelRenderer;
        if (value) {
            if (VoxyClientInstance.isInGame) {
                VoxyCommon.createInstance();
                if (renderer != null && storage.enableRendering) renderer.createRenderer();
            }
        } else {
            if (renderer != null) renderer.shutdownRenderer();
            VoxyCommon.shutdownInstance();
        }
    }

    private static void setServiceThreads(VoxyConfig storage, int value) {
        boolean wasEnabled = VoxyCommon.getInstance() != null;
        var renderer = (IGetVoxyRenderSystem) Minecraft.getInstance().levelRenderer;
        if (wasEnabled) {
            if (renderer != null) renderer.shutdownRenderer();
            VoxyCommon.shutdownInstance();
        }
        storage.serviceThreads = value;
        if (wasEnabled) VoxyCommon.createInstance();
    }

    private static void setRendering(VoxyConfig storage, boolean value) {
        storage.enableRendering = value;
        var renderer = (IGetVoxyRenderSystem) Minecraft.getInstance().levelRenderer;
        if (renderer != null) {
            if (value) renderer.createRenderer(); else renderer.shutdownRenderer();
        }
    }

    private static void setRenderDistance(VoxyConfig storage, int value) {
        storage.sectionRenderDistance = value;
        var renderer = (IGetVoxyRenderSystem) Minecraft.getInstance().levelRenderer;
        if (renderer != null && renderer.getVoxyRenderSystem() != null) renderer.getVoxyRenderSystem().setRenderDistance(value);
    }

    private static float ln2subDiv(int value) {
        return (float) (SUBDIV_MIN * Math.pow(2, SUBDIV_CONST * ((double) value / SUBDIV_IN_MAX)));
    }

    private static int subDiv2ln(float value) {
        return (int) (((Math.log(value / SUBDIV_MIN) / Math.log(2)) / SUBDIV_CONST) * SUBDIV_IN_MAX);
    }
}
