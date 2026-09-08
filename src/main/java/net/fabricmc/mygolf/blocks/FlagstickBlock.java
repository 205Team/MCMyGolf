package net.fabricmc.mygolf.blocks;

import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.fabricmc.mygolf.blocks.base.BaseBlockWithEntity;
import net.fabricmc.mygolf.registry.RegisterBlocks;
import net.fabricmc.mygolf.registry.RegisterItems;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class FlagstickBlock extends BaseBlockWithEntity {
    static int maxCount = 1;
    public static final IntProperty ROTATION;
    protected static final VoxelShape SHAPE;
    public static final BooleanProperty BEAM_TOGGLE = BooleanProperty.of("beam_toggle");

    static {
        ROTATION = Properties.ROTATION;
        SHAPE = VoxelShapes.cuboid(0.4375, -1F, 0.4375, 0.5625, 1F, 0.5625);
    }

    public FlagstickBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(ROTATION, 0)
                .with(BEAM_TOGGLE, true)
        );
    }

    //默认设置
    public static FlagstickBlock defaultInstance() {
        return new FlagstickBlock(
                AbstractBlock.Settings
                        .copy(Blocks.STONE)
                        .pistonBehavior(PistonBehavior.DESTROY)
                        .nonOpaque()
                        .luminance(state -> state.get(BEAM_TOGGLE) ? 12 : 0)
        );
    }

    public Item.Settings itemDefaultSetting() {
        return new Item.Settings().maxCount(maxCount);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            // Toggle the boolean state
            boolean currentShowBeam = state.get(BEAM_TOGGLE);
            world.setBlockState(pos, state.with(BEAM_TOGGLE, !currentShowBeam), Block.NOTIFY_ALL);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        world.setBlockState(pos.down(), RegisterBlocks.GOLF_HOLE.getDefaultState());
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof FlagstickEntity) {
                if (world instanceof ServerWorld) {
                    BlockPos downBlockPos = pos.down();
                    BlockState downBlockState = world.getBlockState(downBlockPos);
                    if (downBlockState.isOf(RegisterBlocks.GOLF_HOLE)) {
                        world.breakBlock(downBlockPos, false);
                    }
                }
                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && !player.isCreative()) {
            dropStack(world, pos, new ItemStack(RegisterItems.FLAGSTICK_ITEM));
        }

        super.onBreak(world, pos, state, player);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos downBlockPos = pos.down();
        BlockState downBlockState = world.getBlockState(downBlockPos);
        // Place when dirt or farmland below
        return downBlockState.isIn(BlockTags.DIRT) || downBlockState.isOf(Blocks.FARMLAND);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(ROTATION, MathHelper.floor((double) (ctx.getPlayerYaw() * 16.0F / 360.0F) + 0.5) & 15);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(ROTATION, rotation.rotate(state.get(ROTATION), 16));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.with(ROTATION, mirror.mirror(state.get(ROTATION), 16));
    }

    @Override
    public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
        return new ItemStack(RegisterItems.FLAGSTICK_ITEM);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ROTATION, BEAM_TOGGLE);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FlagstickEntity(pos, state);
    }

}
