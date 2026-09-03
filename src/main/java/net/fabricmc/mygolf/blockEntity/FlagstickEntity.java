package net.fabricmc.mygolf.blockEntity;

import net.fabricmc.mygolf.registry.RegisterBlockEntities;
import net.fabricmc.mygolf.blockEntity.base.BaseBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class FlagstickEntity extends BaseBlockEntity {
    public FlagstickEntity(BlockPos pos, BlockState state) {
        super(RegisterBlockEntities.FLAGSTICK_ENTITY, pos, state);
    }
}
