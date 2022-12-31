package net.fabricmc.mygolf.blocks;

import net.fabricmc.mygolf.RegisterBlocks;
import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.fabricmc.mygolf.blocks.base.BaseBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GolfHole extends BaseBlock {

    public GolfHole(AbstractBlock.Settings settings) {
        super(settings);
    }

    public static GolfHole defaultInstance() { return new GolfHole(defaultSetting());   }

    //默认设置
    private static AbstractBlock.Settings defaultSetting() {
        return AbstractBlock.Settings.of(Material.SOLID_ORGANIC).ticksRandomly().strength(0.6F).sounds(BlockSoundGroup.GRASS);
    }

    public Item.Settings itemDefaultSetting() {
        return new Item.Settings();
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world instanceof ServerWorld) {
                BlockPos upBlockPos = pos.up();
                BlockState upBlockState = world.getBlockState(upBlockPos);
                if(upBlockState.isOf(RegisterBlocks.FLAGSTICK)){
                    world.breakBlock(upBlockPos, false);
                }
            }
            world.updateComparators(pos, this);
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }
}
