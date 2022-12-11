package net.fabricmc.mygolf.entity.base;

import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;

public class BaseEntity extends MobEntity implements EntityAbstract {

    protected BaseEntity(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    /**
     * 获取代号
     * @return 代号
     */
    public String codeName() {
        return StringTool.getIdFrom(getClass().getSimpleName());
    }
}
