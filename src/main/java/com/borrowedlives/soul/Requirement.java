package com.borrowedlives.soul;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.borrowedlives.BorrowedLives;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

/** What an altar demands in exchange for a soul: one kind of item and how many. */
public record Requirement(Item item, int count) {
    /** The weighted table of possible requirements. Datapacks can override it. */
    public static final ResourceKey<LootTable> TABLE =
            ResourceKey.create(Registries.LOOT_TABLE, BorrowedLives.id("revive_requirement"));

    public static final Codec<Requirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Requirement::item),
            ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(Requirement::count))
            .apply(instance, Requirement::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, Requirement> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), Requirement::item,
            ByteBufCodecs.VAR_INT, Requirement::count,
            Requirement::new);

    public static Requirement roll(ServerLevel level) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(TABLE);
        LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
        for (ItemStack stack : table.getRandomItems(params)) {
            if (!stack.isEmpty()) {
                return new Requirement(stack.getItem(), stack.getCount());
            }
        }
        BorrowedLives.LOGGER.warn("Loot table {} produced nothing; falling back to cobblestone", TABLE.location());
        return new Requirement(Items.COBBLESTONE, 64);
    }
}
