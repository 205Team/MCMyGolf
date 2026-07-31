package net.fabricmc.mygolf.blockEntity.base;

import net.fabricmc.mygolf.blocks.base.BlockAbstract;
import net.fabricmc.mygolf.tools.StringTool;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;

public abstract class BaseBlockEntity extends BlockEntity implements BlockEntityAbstract {

    public BaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

}
