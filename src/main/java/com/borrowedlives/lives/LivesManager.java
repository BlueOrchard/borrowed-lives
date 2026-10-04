package com.borrowedlives.lives;

import java.util.UUID;

import com.borrowedlives.Config;
import com.borrowedlives.network.LivesPayload;
import com.borrowedlives.soul.SoulData;
import com.borrowedlives.soul.SoulItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side rules for reading and changing a player's lives. */
public final class LivesManager {
    private LivesManager() {}

    /** Vanilla Hardcore already has its own permanent death, so the mod stays out of the way there. */
    public static boolean isActive(MinecraftServer server) {
        return !server.isHardcore();
    }

    /** Returns the player's lives, giving them the configured maximum the first time they are seen. */
    public static int getLives(ServerPlayer player) {
        LivesData data = LivesData.get(player.server);
        var stored = data.getLives(player.getUUID());
        if (stored.isPresent()) {
            return stored.getAsInt();
        }
        int max = Config.maxLives();
        data.setLives(player.getUUID(), max);
        return max;
    }

    /**
     * Sets the player's lives, clamped to 0..maxLives, and handles the move into or out of
     * the eliminated state.
     */
    public static void setLives(ServerPlayer player, int lives) {
        int old = getLives(player);
        int updated = Mth.clamp(lives, 0, Config.maxLives());
        if (updated == old) {
            return;
        }
        LivesData data = LivesData.get(player.server);
        data.setLives(player.getUUID(), updated);
        sync(player);

        if (updated == 0) {
            UUID soulId = UUID.randomUUID();
            data.setSoulId(player.getUUID(), soulId);
            SoulItem.drop(player, soulId);
            player.server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("borrowedlives.eliminated", player.getDisplayName())
                            .withStyle(ChatFormatting.RED),
                    false);
            // A player who just died is switched when they respawn instead.
            if (player.isAlive()) {
                enforceSpectator(player);
            }
        } else if (old == 0) {
            data.setSoulId(player.getUUID(), null);
            data.setPendingRevive(player.getUUID(), null);
            GameType mode = player.server.getDefaultGameType();
            player.setGameMode(mode == GameType.SPECTATOR ? GameType.SURVIVAL : mode);
            player.server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("borrowedlives.revived", player.getDisplayName())
                            .withStyle(ChatFormatting.GREEN),
                    false);
        }
    }

    /** Takes one life for a death. Does nothing if the player is already eliminated. */
    public static void onDeath(ServerPlayer player) {
        int lives = getLives(player);
        if (lives <= 0) {
            return;
        }
        setLives(player, lives - 1);
        int left = getLives(player);
        if (left > 0) {
            player.sendSystemMessage(Component.translatable(
                    left == 1 ? "borrowedlives.lives_left.one" : "borrowedlives.lives_left", left)
                    .withStyle(left == 1 ? ChatFormatting.RED : ChatFormatting.YELLOW));
        }
    }

    public static void revive(ServerPlayer player) {
        if (getLives(player) == 0) {
            setLives(player, Config.livesOnRevive());
        }
    }

    /** Whether this soul still belongs to a dead player, rather than being left over from an earlier death. */
    public static boolean isSoulCurrent(MinecraftServer server, SoulData soul) {
        LivesData data = LivesData.get(server);
        return data.getLives(soul.owner()).orElse(-1) == 0 && soul.soulId().equals(data.getSoulId(soul.owner()));
    }

    /**
     * Brings a dead player back at an altar. If they are offline, or still on the death screen,
     * the revive waits until they next join or respawn.
     */
    public static void reviveAt(MinecraftServer server, SoulData soul, GlobalPos altar) {
        LivesData.get(server).setPendingRevive(soul.owner(), altar);
        ServerPlayer player = server.getPlayerList().getPlayer(soul.owner());
        if (player == null || !applyPendingRevive(player)) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("borrowedlives.revive_pending", soul.ownerName())
                            .withStyle(ChatFormatting.GREEN),
                    false);
        }
    }

    /** Carries out a waiting altar revive, if there is one and the player is alive to receive it. */
    public static boolean applyPendingRevive(ServerPlayer player) {
        GlobalPos altar = LivesData.get(player.server).getPendingRevive(player.getUUID());
        if (altar == null || !player.isAlive()) {
            return false;
        }
        ServerLevel level = player.server.getLevel(altar.dimension());
        if (level != null) {
            player.teleportTo(level, altar.pos().getX() + 0.5, altar.pos().getY() + 1.0, altar.pos().getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        }
        revive(player);
        return true;
    }

    /** Puts an eliminated player into spectator mode. Safe to call on any living player. */
    public static void enforceSpectator(ServerPlayer player) {
        if (getLives(player) == 0 && player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            player.setGameMode(GameType.SPECTATOR);
        }
    }

    public static void sync(ServerPlayer player) {
        // Skip connections that never negotiated the channel, such as a client without the mod.
        if (!player.connection.hasChannel(LivesPayload.TYPE)) {
            return;
        }
        int lives = isActive(player.server) ? getLives(player) : LivesPayload.INACTIVE;
        PacketDistributor.sendToPlayer(player, new LivesPayload(lives));
    }
}
