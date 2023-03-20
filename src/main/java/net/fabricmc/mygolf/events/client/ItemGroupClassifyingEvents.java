package net.fabricmc.mygolf.events.client;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.mygolf.RegisterBlocks;
import net.fabricmc.mygolf.RegisterItems;
import net.minecraft.item.ItemGroups;

public class ItemGroupClassifyingEvents {

    public static void registerEvents() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(RegisterItems.IRON_STICK_ITEM));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(RegisterItems.GOLF_CLUB_TOOL));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> entries.add(RegisterBlocks.FLAGSTICK));
    }

}
