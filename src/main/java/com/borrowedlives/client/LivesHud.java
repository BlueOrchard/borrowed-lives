package com.borrowedlives.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.borrowedlives.BorrowedLives;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Draws a heart with the remaining lives in it, just left of the hotbar. */
public final class LivesHud {
    private static final ResourceLocation HEART = BorrowedLives.id("textures/gui/life_heart.png");
    private static final int WIDTH = 17;
    private static final int HEIGHT = 16;
    private static final int GAP = 4;

    private static final int HOTBAR_HALF_WIDTH = 91;
    private static final int HOTBAR_HEIGHT = 22;
    // Vanilla draws the offhand slot this far out from the hotbar's edge.
    private static final int OFFHAND_SLOT_WIDTH = 29;

    private static final int GREEN = 0xFF55FF55;
    private static final int YELLOW = 0xFFFFFF55;
    private static final int RED = 0xFFFF5555;
    private static final int OUTLINE = 0xFF000000;

    private LivesHud() {}

    static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, BorrowedLives.id("lives"), LivesHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        int lives = ClientLives.lives;
        if (player == null || mc.options.hideGui || player.isSpectator() || lives <= 0) {
            return;
        }

        int x = graphics.guiWidth() / 2 - HOTBAR_HALF_WIDTH - GAP - WIDTH;
        // A right-handed player's offhand slot appears on the left, where the heart would be.
        if (player.getMainArm() == HumanoidArm.RIGHT && !player.getOffhandItem().isEmpty()) {
            x -= OFFHAND_SLOT_WIDTH;
        }
        int y = graphics.guiHeight() - HOTBAR_HEIGHT + (HOTBAR_HEIGHT - HEIGHT) / 2;

        RenderSystem.enableBlend();
        graphics.blit(HEART, x, y, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);

        Font font = mc.font;
        String text = Integer.toString(lives);
        // Font widths include one pixel of trailing spacing.
        int textX = x + (WIDTH - (font.width(text) - 1)) / 2;
        int textY = y + 3;
        // A dark outline keeps the number readable against the red heart, whatever its colour.
        graphics.drawString(font, text, textX + 1, textY, OUTLINE, false);
        graphics.drawString(font, text, textX - 1, textY, OUTLINE, false);
        graphics.drawString(font, text, textX, textY + 1, OUTLINE, false);
        graphics.drawString(font, text, textX, textY - 1, OUTLINE, false);
        graphics.drawString(font, text, textX, textY, lives == 1 ? RED : lives == 2 ? YELLOW : GREEN, false);
    }
}
