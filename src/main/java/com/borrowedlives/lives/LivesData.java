package com.borrowedlives.lives;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world record of every player's lives, keyed by UUID so offline players can be looked up. */
public class LivesData extends SavedData {
    private static final String NAME = "borrowedlives";
    private static final Factory<LivesData> FACTORY = new Factory<>(LivesData::new, LivesData::load);

    private static final class Entry {
        int lives;
        /** The soul dropped by this player's most recent final death, if they are still dead. */
        @Nullable
        UUID soulId;
        /** Where a player whose soul has been redeemed should come back, once they are able to. */
        @Nullable
        GlobalPos pendingRevive;
    }

    private final Map<UUID, Entry> players = new HashMap<>();

    public static LivesData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, NAME);
    }

    public OptionalInt getLives(UUID player) {
        Entry entry = players.get(player);
        return entry == null ? OptionalInt.empty() : OptionalInt.of(entry.lives);
    }

    public void setLives(UUID player, int value) {
        entry(player).lives = value;
        setDirty();
    }

    @Nullable
    public UUID getSoulId(UUID player) {
        Entry entry = players.get(player);
        return entry == null ? null : entry.soulId;
    }

    public void setSoulId(UUID player, @Nullable UUID soulId) {
        entry(player).soulId = soulId;
        setDirty();
    }

    @Nullable
    public GlobalPos getPendingRevive(UUID player) {
        Entry entry = players.get(player);
        return entry == null ? null : entry.pendingRevive;
    }

    public void setPendingRevive(UUID player, @Nullable GlobalPos pos) {
        entry(player).pendingRevive = pos;
        setDirty();
    }

    private Entry entry(UUID player) {
        return players.computeIfAbsent(player, id -> new Entry());
    }

    private static LivesData load(CompoundTag tag, HolderLookup.Provider registries) {
        LivesData data = new LivesData();
        ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag saved = list.getCompound(i);
            Entry entry = new Entry();
            entry.lives = saved.getInt("lives");
            if (saved.hasUUID("soul")) {
                entry.soulId = saved.getUUID("soul");
            }
            if (saved.contains("pending_revive")) {
                entry.pendingRevive = GlobalPos.CODEC.parse(NbtOps.INSTANCE, saved.get("pending_revive"))
                        .result().orElse(null);
            }
            data.players.put(saved.getUUID("id"), entry);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        players.forEach((id, entry) -> {
            CompoundTag saved = new CompoundTag();
            saved.putUUID("id", id);
            saved.putInt("lives", entry.lives);
            if (entry.soulId != null) {
                saved.putUUID("soul", entry.soulId);
            }
            if (entry.pendingRevive != null) {
                GlobalPos.CODEC.encodeStart(NbtOps.INSTANCE, entry.pendingRevive).result()
                        .ifPresent(encoded -> saved.put("pending_revive", encoded));
            }
            list.add(saved);
        });
        tag.put("players", list);
        return tag;
    }
}
