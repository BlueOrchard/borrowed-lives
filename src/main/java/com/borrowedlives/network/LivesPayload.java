package com.borrowedlives.network;

import com.borrowedlives.BorrowedLives;
import com.borrowedlives.client.ClientLives;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Tells a client how many lives it has left, or {@link #INACTIVE} when the mod is not running. */
public record LivesPayload(int lives) implements CustomPacketPayload {
    public static final int INACTIVE = -1;

    public static final Type<LivesPayload> TYPE = new Type<>(BorrowedLives.id("lives"));
    public static final StreamCodec<ByteBuf, LivesPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LivesPayload::lives, LivesPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC,
                (payload, context) -> ClientLives.lives = payload.lives());
    }
}
