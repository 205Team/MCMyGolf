package net.fabricmc.mygolf.items;

import net.minecraft.item.ItemGroup;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;

public class GolfClub extends SwordItem {
    public GolfClub(ToolMaterial toolMaterial, int attackDamage, float attackSpeed, Settings settings) {
        super(toolMaterial, attackDamage, attackSpeed, settings);
    }

    //初始化方法
    public static GolfClub defaultItem() {
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

}
