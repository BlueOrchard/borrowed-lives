package com.borrowedlives.soul;

import java.util.List;
import java.util.UUID;

import com.borrowedlives.registry.ModRegistries;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** The soul a player leaves behind on their final death. Carry it to an altar to bring them back. */
public class SoulItem extends Item {
    public SoulItem(Properties properties) {
        super(properties);
    }

    /** Drops a new soul for the player where they stand, with a freshly rolled requirement. */
    public static void drop(ServerPlayer player, UUID soulId) {
        ServerLevel level = player.serverLevel();
        ItemStack stack = new ItemStack(ModRegistries.SOUL.get());
        stack.set(ModRegistries.SOUL_DATA, new SoulData(player.getUUID(), player.getGameProfile().getName(), soulId,
                Requirement.roll(level), 0, false));

        ItemEntity entity = new ItemEntity(level, player.getX(), player.getY(), player.getZ(), stack);
        entity.setDefaultPickUpDelay();
        // Glowing makes the soul visible through walls for teammates racing the despawn timer.
        entity.setGlowingTag(true);
        level.addFreshEntity(entity);
    }

    // Nothing damages a dropped soul: not fire, lava, explosions or cactus. Only the void or
    // despawning can destroy it.
    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        SoulData data = stack.get(ModRegistries.SOUL_DATA);
        return data == null ? super.getName(stack) : Component.translatable("item.borrowedlives.soul.named", data.ownerName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SoulData data = stack.get(ModRegistries.SOUL_DATA);
        if (data == null) {
            return;
        }
        if (data.revealed()) {
            Requirement requirement = data.requirement();
            tooltip.add(Component.translatable("item.borrowedlives.soul.requirement", requirement.count(),
                    requirement.item().getDescription()).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("item.borrowedlives.soul.progress", data.deposited(), requirement.count())
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.borrowedlives.soul.unrevealed").withStyle(ChatFormatting.GRAY));
        }
    }
}
