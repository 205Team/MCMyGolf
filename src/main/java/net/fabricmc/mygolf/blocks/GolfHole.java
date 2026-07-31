package net.fabricmc.mygolf.blocks;

import net.fabricmc.mygolf.blockEntity.GolfHoleEntity;
import net.fabricmc.mygolf.blocks.base.BaseBlock;
import net.fabricmc.mygolf.registry.RegisterBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class GolfHole extends BaseBlock {

    protected static final VoxelShape OUTLINE_SHAPE;
    private static final VoxelShape RAYCAST_SHAPE;

    static {
        RAYCAST_SHAPE = VoxelShapes.union(createCuboidShape(2.0, 2.0, 2.0, 14.0, 16.0, 14.0), new VoxelShape[]{createCuboidShape(1.0, 2.0, 3.0, 15.0, 16.0, 13.0), createCuboidShape(3.0, 2.0, 1.0, 13.0, 16.0, 15.0)});
        //createCuboidShape(2.0, 2.0, 2.0, 14.0, 16.0, 14.0);
        OUTLINE_SHAPE = VoxelShapes.combineAndSimplify(VoxelShapes.fullCube(), RAYCAST_SHAPE, BooleanBiFunction.ONLY_FIRST);
    }

    public GolfHole(AbstractBlock.Settings settings) {
        super(settings);
    }

    public static GolfHole defaultInstance() {
        return new GolfHole(
                AbstractBlock.Settings.copy(Blocks.GRASS_BLOCK)
                        .strength(1.5F, 6.0F)
                        .pistonBehavior(PistonBehavior.DESTROY)
        );
    }

    //默认设置
    private static AbstractBlock.Settings defaultSetting() {
        return AbstractBlock.Settings.create(); //of(Material.SOLID_ORGANIC, MapColor.RED).ticksRandomly().strength(0.6F).sounds(BlockSoundGroup.GRASS);
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
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Return VoxelShapes.empty() if you want the ball to roll straight through without hitting any collision top,
        // OR return a custom hollow shape (with walls/bottom) so the ball physically rests inside the cup.
        return RAYCAST_SHAPE;
    }

    //生物不可寻路通过
    @Override
    public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
        return false;
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

            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof GolfHoleEntity) {
                ItemScatterer.spawn(world, pos, (GolfHoleEntity) blockEntity);
            }

            world.updateComparators(pos, this);
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    //存储
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult blockHitResult) {
        if (!world.isClient) {
            //This will call the createScreenHandlerFactory method from BlockWithEntity, which will return our blockEntity cast to
            //a namedScreenHandlerFactory. If your block class does not extend BlockWithEntity, it needs to implement createScreenHandlerFactory.
            BlockState upBlockState = world.getBlockState(pos.up());
            if (upBlockState.isOf(RegisterBlocks.FLAGSTICK)) {
                NamedScreenHandlerFactory screenHandlerFactory = upBlockState.createScreenHandlerFactory(world, pos);

                if (screenHandlerFactory != null) {
                    //With this call the server will request the client to open the appropriate screen handler
                    player.openHandledScreen(screenHandlerFactory);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

}


