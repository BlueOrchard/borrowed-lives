package com.borrowedlives.client;

import com.borrowedlives.BorrowedLives;
import com.borrowedlives.registry.ModRegistries;

import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.LodestoneTracker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = BorrowedLives.MODID, dist = Dist.CLIENT)
public class BorrowedLivesClient {
    public BorrowedLivesClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(LivesHud::register);
        modEventBus.addListener(BorrowedLivesClient::registerRenderers);
        modEventBus.addListener(BorrowedLivesClient::clientSetup);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModRegistries.ALTAR_BLOCK_ENTITY.get(), AltarRenderer::new);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        // Same needle animation as the vanilla compass, aimed at the altar the compass has locked on to.
        event.enqueueWork(() -> ItemProperties.register(ModRegistries.ALTAR_COMPASS.get(),
                ResourceLocation.withDefaultNamespace("angle"),
                new CompassItemPropertyFunction((level, stack, entity) -> {
                    LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
                    return tracker == null ? null : tracker.target().orElse(null);
                })));
    }
}
