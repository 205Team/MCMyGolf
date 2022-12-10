package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.items.base.ItemAbstract;
import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
//item：高尔夫球杆
public class GolfClub extends SwordItem implements ItemAbstract {
    public GolfClub(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    //初始化方法
    public static GolfClub defaultInstance() {
        return new GolfClub(defaultToolMaterial(), 3, -2.4F, defaultSetting());
    }

    //默认工具材料
    private static ToolMaterial defaultToolMaterial() {
        return GolfClubToolMaterial.INSTANCE;
    }

    //默认设置
    private static Settings defaultSetting() {
        return new Settings().group(ItemGroup.COMBAT);
    }

    @Override
    public String codeName() {
        return StringTool.getIdFrom(getClass().getSimpleName());
    }
}
