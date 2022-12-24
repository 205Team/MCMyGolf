package net.fabricmc.mygolf.entity.base;

import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Arm;
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
