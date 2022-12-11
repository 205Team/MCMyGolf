package net.fabricmc.mygolf.blocks;

import net.fabricmc.mygolf.blocks.base.BaseBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Material;
import net.minecraft.item.Item;

public class GolfHole extends BaseBlock {

    public GolfHole(AbstractBlock.Settings settings) {
        super(settings);
    }

    public static GolfHole defaultInstance() { return new GolfHole(defaultSetting());   }

    //默认设置
    private static AbstractBlock.Settings defaultSetting() {
        return AbstractBlock.Settings.of(Material.SOIL);
    }

    public Item.Settings itemDefaultSetting() {
        return new Item.Settings();
    }
}
