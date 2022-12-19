package net.fabricmc.mygolf.entity.base;

import dev.lazurite.rayon.impl.bullet.collision.body.EntityRigidBody;
import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;

public class BaseEntity extends LivingEntity implements EntityAbstract {

    protected BaseEntity(EntityType<? extends LivingEntity> entityType, World level) {
        super(entityType, level);
    }

    @Override
    /**
     * 获取代号
     * @return 代号
     */
    public String codeName() {
        return StringTool.getIdFrom(getClass().getSimpleName());
    }

    @Override
    public boolean isSilent() {
        return true;
    }

    @Override
    protected void fall(double d, boolean bl, BlockState blockState, BlockPos blockPos) {}

    @Override
    public boolean handleFallDamage(float f, float g, DamageSource damageSource) {
        return false;
    }
    @Override
    public Iterable<ItemStack> getArmorItems() {
        return new ArrayList<>();
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot equipmentSlot) {
        return new ItemStack(Items.AIR);
    }

    @Override
    public void equipStack(EquipmentSlot equipmentSlot, ItemStack itemStack) {}


    @Override
    public Arm getMainArm() {
        return null;
    }
}
