package net.fabricmc.mygolf.events.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.mygolf.items.GolfClubItem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import static net.fabricmc.mygolf.MyGolfModClient.undoKey;

public class GolfClubEvents {
    public static final Identifier UNDO_PACKET_ID = new Identifier("mygolf", "undo_shot");
    private static long pendingUndoTimestamp = 0;
    private static boolean enterWasDown = false;

    public static ClientTickEvents.EndTick golfClubUndoEvent() {
        return client -> {
            if (client.player == null || client.getWindow() == null) return;

            // Automatically cancel pending confirmation if player opens ANY menu/screen
            if (client.currentScreen != null) {
                pendingUndoTimestamp = 0;
                return;
            }

            long windowHandle = client.getWindow().getHandle();
            long currentTime = System.currentTimeMillis();
            boolean isEnterDown = InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_ENTER)
                                || InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_KP_ENTER);

            // 1. Trigger confirmation window when undoKey is pressed
            if (undoKey.wasPressed()) {
                if (client.player.getMainHandStack().getItem() instanceof GolfClubItem) {
                    pendingUndoTimestamp = currentTime;
                    client.player.sendMessage(
                            Text.literal("Press ENTER within 3s to confirm Undo!").formatted(Formatting.YELLOW),true);
                }
            }

            // 2. Process Enter key press while window is active (3000ms limit)
            if (pendingUndoTimestamp > 0) {
                if (currentTime - pendingUndoTimestamp <= 3000) {
                    // Check for single keypress edge (press down)
                    if (isEnterDown && !enterWasDown) {
                        ClientPlayNetworking.send(UNDO_PACKET_ID, PacketByteBufs.empty());
                        pendingUndoTimestamp = 0; // Consume confirmation
                    }
                } else {
                    // Window expired
                    pendingUndoTimestamp = 0;
                    client.player.sendMessage(Text.literal("Undo timed out.").formatted(Formatting.RED), true);
                }
            }

            enterWasDown = isEnterDown;
        };
    }
}
