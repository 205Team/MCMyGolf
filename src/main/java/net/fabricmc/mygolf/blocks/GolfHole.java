package net.fabricmc.mygolf.blocks;

import dev.lazurite.rayon.api.event.collision.ElementCollisionEvents;
import net.fabricmc.mygolf.RegisterBlocks;
import net.fabricmc.mygolf.blocks.base.BaseBlock;
import net.fabricmc.mygolf.entity.GolfBallEntity;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class GolfHole extends BaseBlock {

    public static final IntProperty SCORE;
    protected static final VoxelShape OUTLINE_SHAPE;
    private static final VoxelShape RAYCAST_SHAPE;

    static {
        SCORE = IntProperty.of("score", 0, 32);
        RAYCAST_SHAPE = VoxelShapes.union(createCuboidShape(2.0, 2.0, 2.0, 14.0, 16.0, 14.0), new VoxelShape[]{createCuboidShape(1.0, 2.0, 3.0, 15.0, 16.0, 13.0), createCuboidShape(3.0, 2.0, 1.0, 13.0, 16.0, 15.0)});
        //createCuboidShape(2.0, 2.0, 2.0, 14.0, 16.0, 14.0);
        OUTLINE_SHAPE = VoxelShapes.combineAndSimplify(VoxelShapes.fullCube(), RAYCAST_SHAPE, BooleanBiFunction.ONLY_FIRST);
    }

    public GolfHole(AbstractBlock.Settings settings) {
        super(settings);
        this.setDefaultState((this.stateManager.getDefaultState()).with(SCORE, 0));
    }

    public static GolfHole defaultInstance() {
        return new GolfHole(defaultSetting());
    }

    //默认设置
    private static AbstractBlock.Settings defaultSetting() {
        return AbstractBlock.Settings.of(Material.SOLID_ORGANIC, MapColor.RED).ticksRandomly().strength(0.6F).sounds(BlockSoundGroup.GRASS);
    }

    /**
     * 事件注册
     */
    public static void registerEvents() {
        //进球事件
        ElementCollisionEvents.BLOCK_COLLISION.register(golfBallInside());
    }

    //进球事件
    private static ElementCollisionEvents.BlockCollision golfBallInside() {
        return (element, terrain, impulse) -> {
            if (element instanceof GolfBallEntity && terrain.getBlockState().getBlock().equals(RegisterBlocks.GOLF_HOLE)) {
                //类型转换
                GolfBallEntity golfBall = (GolfBallEntity) element;

                World world = golfBall.getWorld();
                // 获取球洞方块的碰撞箱
                Box holeBox = terrain.getBlockState().getCollisionShape(world, terrain.getBlockPos()).getBoundingBox();
                // 将碰撞箱转换为世界坐标系中的位置和大小
                holeBox = holeBox.offset(terrain.getBlockPos());
                // 获取球体的碰撞箱
                Box ballBox = golfBall.getBoundingBox();
                // 检测球体中心是否包含在球洞的碰撞箱内部
                if (holeBox.contains(ballBox.getCenter())) {
                    System.out.println("Golf ball enters the hole!!");
                }
            }
        };
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
                if (upBlockState.isOf(RegisterBlocks.FLAGSTICK)) {
                    world.breakBlock(upBlockPos, false);
                }
            }
            world.updateComparators(pos, this);
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }
}


