package com.borrowedlives;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.borrowedlives.network.LivesPayload;
import com.borrowedlives.registry.ModRegistries;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(BorrowedLives.MODID)
public class BorrowedLives {
    public static final String MODID = "borrowedlives";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BorrowedLives(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistries.register(modEventBus);
        modEventBus.addListener(LivesPayload::register);

        // Server config: stored per world and synced to clients.
        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
