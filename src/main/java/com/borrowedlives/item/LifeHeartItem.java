package com.borrowedlives.item;

import java.util.List;

import com.borrowedlives.Config;
import com.borrowedlives.lives.LivesManager;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Restores one life to a living player, up to the configured maximum. */
public class LifeHeartItem extends Item {
    public LifeHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (!LivesManager.isActive(serverPlayer.server)) {
            return InteractionResultHolder.fail(stack);
        }
        int lives = LivesManager.getLives(serverPlayer);
        if (lives <= 0 || lives >= Config.maxLives()) {
            player.displayClientMessage(Component.translatable("item.borrowedlives.life_heart.full"), true);
            return InteractionResultHolder.fail(stack);
        }

        LivesManager.setLives(serverPlayer, lives + 1);
        stack.consume(1, player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS,
                0.6F, 1.2F);
        player.displayClientMessage(Component.translatable("item.borrowedlives.life_heart.used", lives + 1)
                .withStyle(ChatFormatting.GREEN), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.borrowedlives.life_heart.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
