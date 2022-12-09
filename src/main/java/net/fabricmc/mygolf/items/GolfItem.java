package net.fabricmc.mygolf.items;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

public class GolfItem extends Item {

    static int maxCount = 4;    //最大堆叠数量

    public GolfItem(Settings settings) {
        super(settings);
    }

    //初始化方法
    public static GolfItem defaultItem() {
        return new GolfItem(defaultSetting());
    }

    //默认设置
    private static Settings defaultSetting() {
        return new Settings().maxCount(maxCount).group(ItemGroup.MISC);
    }

}
