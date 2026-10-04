package com.borrowedlives.soul;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Everything a soul item knows about itself.
 *
 * @param soulId    identifies this particular death, so a soul left over from an earlier death is worthless
 * @param deposited how much of the requirement has been paid so far
 * @param revealed  whether the soul has been placed on an altar, which is what reveals the requirement
 */
public record SoulData(UUID owner, String ownerName, UUID soulId, Requirement requirement, int deposited,
        boolean revealed) {
    public static final Codec<SoulData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(SoulData::owner),
            Codec.STRING.fieldOf("owner_name").forGetter(SoulData::ownerName),
            UUIDUtil.CODEC.fieldOf("soul_id").forGetter(SoulData::soulId),
            Requirement.CODEC.fieldOf("requirement").forGetter(SoulData::requirement),
            Codec.INT.optionalFieldOf("deposited", 0).forGetter(SoulData::deposited),
            Codec.BOOL.optionalFieldOf("revealed", false).forGetter(SoulData::revealed))
            .apply(instance, SoulData::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SoulData::owner,
            ByteBufCodecs.STRING_UTF8, SoulData::ownerName,
            UUIDUtil.STREAM_CODEC, SoulData::soulId,
            Requirement.STREAM_CODEC, SoulData::requirement,
            ByteBufCodecs.VAR_INT, SoulData::deposited,
            ByteBufCodecs.BOOL, SoulData::revealed,
            SoulData::new);

    public int remaining() {
        return Math.max(0, requirement.count() - deposited);
    }

    public SoulData withRevealed() {
        return new SoulData(owner, ownerName, soulId, requirement, deposited, true);
    }

    public SoulData withDeposited(int amount) {
        return new SoulData(owner, ownerName, soulId, requirement, amount, revealed);
    }
}
