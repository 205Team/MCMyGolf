package net.fabricmc.mygolf.items.base;

import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.item.Item;
//物品抽象类
public abstract class BaseItem extends Item implements ItemAbstract{

    public BaseItem(Settings settings) {
        super(settings);
    }

    /**
     * 获取代号
     * @return item的代号
     */
    public String codeName() {
        return StringTool.getItemId(getClass().getSimpleName());
    }
}
