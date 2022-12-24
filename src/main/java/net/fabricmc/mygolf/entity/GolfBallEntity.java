package net.fabricmc.mygolf.entity;

import dev.lazurite.rayon.api.EntityPhysicsElement;
import dev.lazurite.rayon.impl.bullet.collision.body.EntityRigidBody;
import net.fabricmc.mygolf.entity.base.BaseEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.util.math.BlockPos;
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
    @Override
    public boolean isSilent() { return true;}
    @Override
    public boolean isInvulnerable() { return true; }
    @Override
    protected void fall(double d, boolean bl, BlockState blockState, BlockPos blockPos) {}
    @Override
    public boolean handleFallDamage(float f, float g, DamageSource damageSource) {
        return false;
    }
}