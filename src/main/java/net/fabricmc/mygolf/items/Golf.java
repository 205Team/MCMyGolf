package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.items.base.BaseItem;
import net.minecraft.item.ItemGroup;

//item：棒球
public class Golf extends BaseItem {

    static int maxCount = 4;    //最大堆叠数量

    public Golf(Settings settings) {
        super(settings);
    }

    //初始化方法
    public static Golf defaultItem() {
        return new Golf(defaultSetting());
    }

    //默认设置
    private static Settings defaultSetting() {
        return new Settings().maxCount(maxCount).group(ItemGroup.MISC);
    }
}
