package net.fabricmc.mygolf.blocks.base;

import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.block.Block;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.item.Item;

public abstract class BaseBlockWithEntity extends BlockWithEntity implements BlockAbstract{

    public BaseBlockWithEntity(Settings settings) {
        super(settings);
    }

    /**
     * 获取代号
     * @return 代号
     */
    public String codeName() {
        return StringTool.getIdFrom(getClass().getSimpleName());
    }

}
