package net.fabricmc.mygolf;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class RegisterItemGroups {
    public static void registryItemGroup() {
        FabricItemGroup.builder(new Identifier(CommonStr.modId, "golf_item_group"))
                .displayName(Text.literal("Golf"))
                .icon(() -> new ItemStack(RegisterItems.GOLF_CLUB_TOOL))
                .entries((enabledFeatures, entries, operatorEnabled) -> {
                    entries.add(RegisterItems.INTRO_BOOK);
                    entries.add(RegisterItems.GOLF_CLUB_TOOL);
                    entries.add(RegisterItems.GOLF_BALL);
                    entries.add(RegisterItems.IRON_STICK_ITEM);
                    entries.add(RegisterBlocks.FLAGSTICK);
                })
                .build();
    }
}
