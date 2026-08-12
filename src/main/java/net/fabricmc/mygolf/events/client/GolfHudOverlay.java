package net.fabricmc.mygolf.events.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.mygolf.items.GolfClubItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class GolfHudOverlay implements HudRenderCallback {
    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) return;

        // Check if player is currently actively using a GolfClubItem
        ItemStack activeItem = player.getActiveItem();
        if (player.isUsingItem() && activeItem.getItem() instanceof GolfClubItem) {

            // Calculate charge ratio (0.0 to 1.0)
            int useTicks = player.getItemUseTime();

            // Only show meter if held past the tap threshold (5+ ticks)
            if (useTicks < 5) return;

            float progress = Math.min(1.0f, (float) useTicks / GolfClubItem.MAX_CHARGE_TICKS);

            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            // Bar dimensions & center-screen coordinates
            int barWidth = 100;
            int barHeight = 8;
            int x = (screenWidth - barWidth) / 2;
            int y = (screenHeight / 2) + 30; // 30 pixels below crosshair

            // Color interpolates from Yellow (low charge) to Green (100% charge)
            int filledWidth = (int) (barWidth * progress);
            int fillColor = progress >= 1.0f ? 0xFF00FF00 : 0xFFFFFF00;

            // 1. Draw Background & Progress Bar
            drawContext.fill(x - 1, y - 1, x + barWidth + 1, y + barHeight + 1, 0x88000000);
            drawContext.fill(x, y, x + filledWidth, y + barHeight, fillColor);
        }
    }
}
