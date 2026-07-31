package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.items.base.BaseItem;
import net.minecraft.item.ItemGroup;

//item：铁棍
    public class IronStick extends BaseItem {

    static int maxCount = 64;    //最大堆叠数量

    public IronStick(Settings settings) {
        super(settings);
    }

    //初始化方法
    public static IronStick defaultInstance() {
        return new IronStick(defaultSetting());
    }

    //默认设置
    private static Settings defaultSetting() {
        return new Settings().maxCount(maxCount);
    }

}

