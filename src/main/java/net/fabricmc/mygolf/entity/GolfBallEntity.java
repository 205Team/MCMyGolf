package net.fabricmc.mygolf.entity;

import dev.lazurite.rayon.api.EntityPhysicsElement;
import dev.lazurite.rayon.impl.bullet.collision.body.EntityRigidBody;
import net.fabricmc.mygolf.RegisterItems;
import net.fabricmc.mygolf.entity.base.BaseEntity;
import net.fabricmc.mygolf.items.IronStick;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class GolfBallEntity extends BaseEntity implements EntityPhysicsElement {
    private final EntityRigidBody rigidBody = new EntityRigidBody(this);
    public GolfBallEntity(EntityType<? extends LivingEntity> entityType, World level) {
        super(entityType, level);
        this.rigidBody.setMass(0.457f);
    }

    @Override
    public EntityRigidBody getRigidBody() {
        return this.rigidBody;
    }
}