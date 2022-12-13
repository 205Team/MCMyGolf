package net.fabricmc.mygolf.items;

import net.fabricmc.mygolf.RegisterEntities;
import net.fabricmc.mygolf.items.base.BaseItem;
import net.fabricmc.mygolf.items.base.ItemAbstract;
import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.SpawnEggItem;

public class GolfBallSpawnEgg extends SpawnEggItem implements ItemAbstract {

    static int maxCount = 64;    //最大堆叠数量

    public GolfBallSpawnEgg(EntityType<? extends MobEntity> type, int primaryColor, int secondaryColor, Settings settings) {
        super(type, primaryColor, secondaryColor, settings);
    }


    //初始化方法
    public static GolfBallSpawnEgg defaultInstance() {
        return new GolfBallSpawnEgg(RegisterEntities.GOLF_BALL,12895428, 11382189, defaultSetting());
    }

    //默认设置
    private static Settings defaultSetting() {
        return new Settings().maxCount(maxCount).group(ItemGroup.MISC);
    }

    @Override
    public String codeName() {
        return StringTool.getIdFrom(getClass().getSimpleName());
    }
}
