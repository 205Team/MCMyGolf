package net.fabricmc.mygolf.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.mygolf.global.CommonStr;
import net.fabricmc.mygolf.items.*;
import net.fabricmc.mygolf.items.base.BaseItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
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
    public static final GolfClubItem GOLF_CLUB_TOOL = GolfClubItem.defaultInstance();       //高尔夫球杆
    public static final IronStick IRON_STICK_ITEM = IronStick.defaultInstance();       //铁棒
    public static final IntroBook INTRO_BOOK = IntroBook.defaultInstance(); //介绍书
    public static final FlagstickItem FLAGSTICK_ITEM = FlagstickItem.defaultInstance(); //介绍书

    /**
     * 注册新item group
     */
    public static final RegistryKey<ItemGroup> GOLF_GROUP_KEY = RegistryKey.of(
            RegistryKeys.ITEM_GROUP,
            new Identifier(CommonStr.modId, "golf_item_group")
    );

    public static final ItemGroup GOLF_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            GOLF_GROUP_KEY,
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(GOLF_BALL))
                    .displayName(Text.translatable("itemGroup.mygolf.golf_group"))
                    .entries((displayContext, entries) -> {
                        entries.add(INTRO_BOOK);
                        entries.add(GOLF_CLUB_TOOL);
                        entries.add(GOLF_BALL);
                        entries.add(IRON_STICK_ITEM);
                        entries.add(FLAGSTICK_ITEM);
                    })
                    .build()
    );

    /**
     * 注册物品
     */
    public static void registryItems() {
        registryItem(GOLF_BALL);
        registryItem(GOLF_CLUB_TOOL, GOLF_CLUB_TOOL.codeName());
        registryItem(IRON_STICK_ITEM);
        registryItem(INTRO_BOOK);
        registryItem(FLAGSTICK_ITEM);
    }

    /**
     * 注册新BaseItem
     *
     * @param item 物品
     */
    private static void registryItem(BaseItem item) {
        registryItem(item, item.codeName());
    }


    /**
     * 注册新item
     *
     * @param item     物品
     * @param codeName 代号
     */
    private static void registryItem(Item item, String codeName) {
        Registry.register(Registries.ITEM, new Identifier(CommonStr.modId, codeName), item);
    }
}
