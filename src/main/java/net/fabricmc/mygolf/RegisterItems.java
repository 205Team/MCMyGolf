package net.fabricmc.mygolf;

import net.fabricmc.mygolf.global.CommonStr;
import net.fabricmc.mygolf.items.GolfBall;
import net.fabricmc.mygolf.items.Flagstick;
import net.fabricmc.mygolf.items.GolfClub;
import net.fabricmc.mygolf.items.IronStick;
import net.fabricmc.mygolf.items.base.BaseItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

/**
 * 注册物品类
 */
public class RegisterItems {
    /**
     * 物品声明
     */
    public static final GolfBall GOLF_BALL_ITEM = GolfBall.defaultItem();            //棒球
    public static final Flagstick FLAGSTICK_ITEM = Flagstick.defaultItem();     //红旗杆
    public static final GolfClub GOLF_CLUB_TOOL = GolfClub.defaultItem();       //高尔夫球杆
    public static final IronStick IRON_STICK_ITEM = IronStick.defaultItem();       //铁棒
    /**
     * 注册物品
     */
    public static void registryItems() {
        registryItem(GOLF_BALL_ITEM);
        registryItem(FLAGSTICK_ITEM);
        registryItem(GOLF_CLUB_TOOL,GOLF_CLUB_TOOL.codeName());
        registryItem(IRON_STICK_ITEM);
    }

    /**
     * 注册新BaseItem
     * @param item 物品
     */
    public static void registryItem(BaseItem item) {
        registryItem(item, item.codeName());
    }


    /**
     * 注册新item
     * @param item 物品
     * @param codeName 代号
     */
    public static void registryItem(Item item, String codeName) {
        Registry.register(Registry.ITEM, new Identifier(CommonStr.modId, codeName), item);
    }

}
