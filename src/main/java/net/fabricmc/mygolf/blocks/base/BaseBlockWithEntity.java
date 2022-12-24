package net.fabricmc.mygolf.blocks.base;

import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.block.Block;

public abstract class BaseBlockWithEntity extends Block implements BlockAbstract{

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
