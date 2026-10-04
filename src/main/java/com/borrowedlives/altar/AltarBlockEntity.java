package com.borrowedlives.altar;

import com.borrowedlives.lives.LivesManager;
import com.borrowedlives.registry.ModRegistries;
import com.borrowedlives.soul.Requirement;
import com.borrowedlives.soul.SoulData;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AltarBlockEntity extends BlockEntity {
    private ItemStack soul = ItemStack.EMPTY;

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistries.ALTAR_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStack getSoul() {
        return soul;
    }

    /** A player used an item on the altar: either a soul to place, or an offering for the soul already there. */
    public void useItem(ServerPlayer player, ItemStack stack) {
        if (soul.isEmpty()) {
            SoulData data = stack.get(ModRegistries.SOUL_DATA);
            if (data == null) {
                message(player, Component.translatable("borrowedlives.altar.empty"));
            } else if (!LivesManager.isSoulCurrent(player.server, data)) {
                message(player, Component.translatable("borrowedlives.altar.faded").withStyle(ChatFormatting.GRAY));
            } else {
                soul = stack.split(1);
                soul.set(ModRegistries.SOUL_DATA, data.withRevealed());
                update();
                play(SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0F);
                showStatus(player);
            }
            return;
        }

        SoulData data = soul.get(ModRegistries.SOUL_DATA);
        if (data == null || !stack.is(data.requirement().item())) {
            showStatus(player);
            return;
        }
        int paid = Math.min(data.remaining(), stack.getCount());
        stack.consume(paid, player);
        SoulData updated = data.withDeposited(data.deposited() + paid);
        if (updated.remaining() > 0) {
            soul.set(ModRegistries.SOUL_DATA, updated);
            update();
            play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F);
            showStatus(player);
        } else {
            complete(updated);
        }
    }

    /** Empty hand: show what is still owed, or take the soul back when sneaking. */
    public void useEmptyHanded(ServerPlayer player) {
        if (soul.isEmpty()) {
            message(player, Component.translatable("borrowedlives.altar.empty"));
        } else if (player.isShiftKeyDown()) {
            // Progress stays on the soul, so nothing is lost by moving it to another altar.
            player.getInventory().placeItemBackInInventory(soul);
            soul = ItemStack.EMPTY;
            update();
            play(SoundEvents.ITEM_PICKUP, 0.6F);
        } else {
            showStatus(player);
        }
    }

    private void complete(SoulData data) {
        soul = ItemStack.EMPTY;
        update();
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = getBlockPos();
            serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1.5,
                    pos.getZ() + 0.5, 80, 0.4, 0.6, 0.4, 0.3);
            play(SoundEvents.TOTEM_USE, 1.0F);
            LivesManager.reviveAt(serverLevel.getServer(), data, GlobalPos.of(serverLevel.dimension(), pos));
        }
    }

    private void showStatus(ServerPlayer player) {
        SoulData data = soul.get(ModRegistries.SOUL_DATA);
        if (data != null) {
            Requirement requirement = data.requirement();
            message(player, Component.translatable("borrowedlives.altar.status", data.ownerName(), requirement.count(),
                    requirement.item().getDescription(), data.deposited(), requirement.count())
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private static void message(ServerPlayer player, Component text) {
        player.displayClientMessage(text, true);
    }

    private void play(SoundEvent sound, float volume) {
        if (level != null) {
            level.playSound(null, getBlockPos(), sound, SoundSource.BLOCKS, volume, 1.0F);
        }
    }

    private void update() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Written even when empty: an empty tag would be ignored by the client, leaving a removed soul on show.
        tag.putBoolean("has_soul", !soul.isEmpty());
        if (!soul.isEmpty()) {
            tag.put("soul", soul.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        soul = tag.contains("soul") ? ItemStack.parseOptional(registries, tag.getCompound("soul")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
