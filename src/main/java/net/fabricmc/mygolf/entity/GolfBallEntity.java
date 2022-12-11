package net.fabricmc.mygolf.entity;

import net.fabricmc.mygolf.entity.base.BaseEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;

public class GolfBallEntity extends BaseEntity {

    public GolfBallEntity(EntityType<? extends MobEntity> entityType, World world) {
        super(entityType, world);
    }


}
