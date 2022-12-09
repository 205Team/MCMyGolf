package net.fabricmc.mygolf;

import net.fabricmc.mygolf.global.CommonStr;
import net.fabricmc.mygolf.items.GolfItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/**
 * 注册物品类
 */
public class RegisterItems {
    /**
     * 物品声明
     */
    public static final GolfItem GOLF_ITEM = GolfItem.defaultItem();    //棒球

    /**
     * function - 注册物品
     */
    public static void registryItems() {
        Registry.register(Registry.ITEM, new Identifier(CommonStr.modId, CommonStr.ItemCodeName.GolfItem), GOLF_ITEM);
    }

}
