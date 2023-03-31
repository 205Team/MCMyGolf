package net.fabricmc.mygolf.blocks;

import net.fabricmc.mygolf.RegisterBlocks;
import net.fabricmc.mygolf.RegisterEntities;
import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.fabricmc.mygolf.blocks.base.BaseBlock;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class GolfHole extends BaseBlock {

    public static final IntProperty SCORE;
    private static final VoxelShape RAYCAST_SHAPE;
    protected static final VoxelShape OUTLINE_SHAPE;
    public GolfHole(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState((this.stateManager.getDefaultState()).with(SCORE, 0));
    }

    public static GolfHole defaultInstance() { return new GolfHole(defaultSetting());   }

    //默认设置
    private static AbstractBlock.Settings defaultSetting() {
        return AbstractBlock.Settings.of(Material.SOLID_ORGANIC, MapColor.RED).ticksRandomly().strength(0.6F).sounds(BlockSoundGroup.GRASS);
    }

    public Item.Settings itemDefaultSetting() {
        return new Item.Settings();
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return OUTLINE_SHAPE;
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        return RAYCAST_SHAPE;
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        return state.get(SCORE);
    }

    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(SCORE);
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

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        System.out.println("in");
        if(entity instanceof GolfBallEntity){
            world.setBlockState(pos, state.with(SCORE, state.get(SCORE) + 1));
            System.out.println("Holed");
        }
        if(entity instanceof ChickenEntity){
            System.out.println("Chickened");
        }
    }

    static {
        SCORE = IntProperty.of("score", 0, 32);
        RAYCAST_SHAPE = VoxelShapes.union(createCuboidShape(2.0, 2.0, 2.0, 14.0, 16.0, 14.0), new VoxelShape[]{createCuboidShape(1.0, 2.0, 3.0, 15.0, 16.0, 13.0), createCuboidShape(3.0, 2.0, 1.0, 13.0, 16.0, 15.0)});
                        //createCuboidShape(2.0, 2.0, 2.0, 14.0, 16.0, 14.0);
        OUTLINE_SHAPE = VoxelShapes.combineAndSimplify(VoxelShapes.fullCube(), RAYCAST_SHAPE, BooleanBiFunction.ONLY_FIRST);
    }
}
