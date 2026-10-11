package me.cortex.voxy.client.config;

import net.minecraft.client.gui.screens.Screen;

import me.cortex.voxy.commonImpl.VoxyCommon;
import net.caffeinemc.mods.sodium.client.gui.VideoSettingsScreen;

public class ModMenuIntegration {
    public static Screen createScreen(Screen parent) {
            if (VoxyCommon.isAvailable()) {
                return VideoSettingsScreen.createScreen(parent);
            } else {
                return null;
            }
    }
}
