package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.items.base.BaseItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

public class Flagstick extends BaseItem {

    static int maxCount = 1;    //最大堆叠数量
    public Flagstick(Settings settings) {
        super(settings);
    }

    //初始化方法
    public static Flagstick defaultInstance() {
        return new Flagstick(defaultSetting());
    }

    //默认设置
    private static Settings defaultSetting() {
        return new Settings().maxCount(maxCount).group(ItemGroup.MISC);
    }
}