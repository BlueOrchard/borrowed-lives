package com.borrowedlives.lives;

import com.borrowedlives.BorrowedLives;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = BorrowedLives.MODID)
public final class LivesEvents {
    private LivesEvents() {}

    @SubscribeEvent
    static void onRegisterCommands(RegisterCommandsEvent event) {
        LivesCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LivesManager.sync(player);
            if (LivesManager.isActive(player.server) && !LivesManager.applyPendingRevive(player)) {
                LivesManager.enforceSpectator(player);
            }
        }
    }

    // Lowest priority so a death another mod cancels does not cost a life.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && LivesManager.isActive(player.server)) {
            LivesManager.onDeath(player);
        }
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        LivesManager.sync(player);
        if (!LivesManager.isActive(player.server) || event.isEndConquered() || LivesManager.getLives(player) > 0) {
            return;
        }
        // Their soul was redeemed while they were still on the death screen.
        if (LivesManager.applyPendingRevive(player)) {
            return;
        }
        LivesManager.enforceSpectator(player);
        // Send the new spectator back to where they died, so they can watch over their soul.
        player.getLastDeathLocation().ifPresent(pos -> {
            ServerLevel level = player.server.getLevel(pos.dimension());
            if (level != null) {
                player.teleportTo(level, pos.pos().getX() + 0.5, pos.pos().getY(), pos.pos().getZ() + 0.5,
                        player.getYRot(), player.getXRot());
            }
        });
    }
}
