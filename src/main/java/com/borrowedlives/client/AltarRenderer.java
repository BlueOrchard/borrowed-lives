package com.borrowedlives.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.borrowedlives.altar.AltarBlockEntity;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shows the soul resting on an altar, slowly turning and bobbing above it. */
public class AltarRenderer implements BlockEntityRenderer<AltarBlockEntity> {
    private final ItemRenderer itemRenderer;

    public AltarRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(AltarBlockEntity altar, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light, int overlay) {
        ItemStack soul = altar.getSoul();
        Level level = altar.getLevel();
        if (soul.isEmpty() || level == null) {
            return;
        }
        float time = level.getGameTime() + partialTick;

        poseStack.pushPose();
        poseStack.translate(0.5, 1.1 + Math.sin(time / 10.0) * 0.05, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 2.0F));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        itemRenderer.renderStatic(soul, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                poseStack, buffers, level, 0);
        poseStack.popPose();
    }
}
