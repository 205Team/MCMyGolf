package net.fabricmc.mygolf;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.mygolf.global.CommonStr;
import net.fabricmc.mygolf.items.GolfBall;
import net.fabricmc.mygolf.items.GolfClub;
import net.fabricmc.mygolf.items.IntroBook;
import net.fabricmc.mygolf.items.IronStick;
import net.fabricmc.mygolf.items.base.BaseItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * 注册物品类
 */
public class RegisterItems {
    /**
     * 物品声明
     */
    public static final GolfBall GOLF_BALL = GolfBall.defaultInstance();    //高尔夫球
    public static final GolfClub GOLF_CLUB_TOOL = GolfClub.defaultInstance();       //高尔夫球杆
    public static final IronStick IRON_STICK_ITEM = IronStick.defaultInstance();       //铁棒
    public static final IntroBook INTRO_BOOK = IntroBook.defaultInstance(); //介绍书
    /**
     * 注册物品
     */
    public static void registryItems() {
        registryItem(GOLF_BALL);
        registryItem(GOLF_CLUB_TOOL,GOLF_CLUB_TOOL.codeName());
        registryItem(IRON_STICK_ITEM);
        registryItem(INTRO_BOOK);
    }

    /**
     * 注册新BaseItem
     * @param item 物品
     */
    private static void registryItem(BaseItem item) {
        registryItem(item, item.codeName());
    }


    /**
     * 注册新item
     * @param item 物品
     * @param codeName 代号
     */
    private static void registryItem(Item item, String codeName) {
        Registry.register(Registries.ITEM, new Identifier(CommonStr.modId, codeName), item);
    }
}
