package net.fabricmc.mygolf.events.client;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.mygolf.RegisterItems;
import net.minecraft.item.ItemGroups;

public class ItemGroupClassifyingEvents {

    public static void registerEvents() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INVENTORY).register(entries -> {
            entries.add(RegisterItems.IRON_STICK_ITEM);
            entries.add(RegisterItems.GOLF_BALL);
            entries.add(RegisterItems.INTRO_BOOK);
        });
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(RegisterItems.GOLF_CLUB_TOOL));
    }

}
