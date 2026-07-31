package net.fabricmc.mygolf.blocks.base;

import net.minecraft.item.Item;

public interface BlockAbstract {
    /**
     * 代号
     * @return 该block的代号
     */
    abstract String codeName();

    /**
     * blockItem的默认设置
     * @return 该block对应blockItem的设置
     */
    abstract Item.Settings itemDefaultSetting();
}
