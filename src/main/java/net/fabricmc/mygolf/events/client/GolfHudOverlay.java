package net.fabricmc.mygolf.events.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.mygolf.items.GolfClubItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

public class GolfHudOverlay implements HudRenderCallback {

    private static final int MASK_ALPHA = 0x80; // 50% translucency
    private static final int SLOT_SIZE = 16;

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;

        if (player == null || !player.isUsingItem()) return;

        ItemStack activeStack = player.getActiveItem();
        if (!(activeStack.getItem() instanceof GolfClubItem)) return;

        // Calculate smooth charge progress incorporating tickDelta frame interpolation
        float heldTicks = (activeStack.getMaxUseTime() - player.getItemUseTimeLeft()) + tickDelta;
        if (heldTicks < GolfClubItem.MIN_CHARGE_TICKS) return;

        float chargeTicks = heldTicks - GolfClubItem.MIN_CHARGE_TICKS;
        float maxAllowedTicks = GolfClubItem.MAX_CHARGE_TICKS * GolfClubItem.MAX_LOOPS;
        if (chargeTicks >= maxAllowedTicks) return;

        float currentLoft = GolfClubItem.getSelectedLoft(activeStack);
        float powerRatio = GolfClubItem.calculatePowerRatio(chargeTicks, currentLoft);
        if (powerRatio <= 0.0f) return;

        // Determine screen coordinates of the active hotbar slot
        int scaledWidth = drawContext.getScaledWindowWidth();
        int scaledHeight = drawContext.getScaledWindowHeight();
        int x;
        int y = scaledHeight - 19; // Standard vertical position for hotbar slot content

        if (player.getActiveHand() == Hand.MAIN_HAND) {
            int slot = player.getInventory().selectedSlot;
            x = scaledWidth / 2 - 88 + (slot * 20);
        } else {
            // Handle Offhand slot rendering based on player main arm setting
            boolean isRightArm = player.getMainArm() == Arm.RIGHT;
            x = isRightArm ? (scaledWidth / 2 - 117) : (scaledWidth / 2 + 101);
        }

        int progressWidth = Math.round(SLOT_SIZE * powerRatio);
        int x1 = x;
        int x2 = x + progressWidth;
        int y1 = y;
        int y2 = y + SLOT_SIZE;
        int red = (int) (255 * powerRatio);
        int green = (int) (255 * (1.0f - powerRatio));
        int dynamicColor = (MASK_ALPHA << 24) | (red << 16) | (green << 8);

        drawContext.fill(x1, y1, x2, y2, dynamicColor);
    }
}
