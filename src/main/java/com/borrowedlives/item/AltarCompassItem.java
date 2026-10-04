package com.borrowedlives.item;

import java.util.List;
import java.util.Optional;

import com.borrowedlives.BorrowedLives;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

/** A compass that, when used, locks on to the nearest altar. */
public class AltarCompassItem extends Item {
    public static final TagKey<Structure> ALTARS = TagKey.create(Registries.STRUCTURE, BorrowedLives.id("altar"));
    private static final int SEARCH_RADIUS_CHUNKS = 100;
    private static final int COOLDOWN_TICKS = 60;

    public AltarCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        BlockPos found = serverLevel.findNearestMapStructure(ALTARS, player.blockPosition(), SEARCH_RADIUS_CHUNKS, false);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        if (found == null) {
            player.displayClientMessage(Component.translatable("item.borrowedlives.altar_compass.none"), true);
            return InteractionResultHolder.fail(stack);
        }

        // Not "tracked": that flag is for lodestones, and vanilla would clear the target without one.
        stack.set(DataComponents.LODESTONE_TRACKER,
                new LodestoneTracker(Optional.of(GlobalPos.of(serverLevel.dimension(), found)), false));
        level.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        player.displayClientMessage(Component.translatable("item.borrowedlives.altar_compass.found"), true);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(DataComponents.LODESTONE_TRACKER);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(stack.has(DataComponents.LODESTONE_TRACKER)
                ? "item.borrowedlives.altar_compass.tooltip.locked"
                : "item.borrowedlives.altar_compass.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
