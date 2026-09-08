package net.fabricmc.mygolf.events.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.BookScreen;
import net.minecraft.item.ItemStack;

@Environment(EnvType.CLIENT)
public class BookGuiHelper {
    public static void openBookScreen(ItemStack bookStack) {
        MinecraftClient.getInstance().setScreen(new BookScreen(new BookScreen.WrittenBookContents(bookStack)));
    }
}