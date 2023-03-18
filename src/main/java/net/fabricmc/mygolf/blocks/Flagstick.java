package net.fabricmc.mygolf.blocks;

import net.fabricmc.mygolf.RegisterBlocks;
import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.fabricmc.mygolf.blocks.base.BaseBlockWithEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class Flagstick extends BaseBlockWithEntity implements BlockEntityProvider {
    static int maxCount = 1;    //最大堆叠数量
    public static final IntProperty ROTATION;
    protected static final VoxelShape SHAPE;
    static {
        ROTATION = Properties.ROTATION;
        SHAPE = VoxelShapes.cuboid(0.375F, 0, 0.375F, 0.625F, 1f, 0.625F);
    }

    public Flagstick(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(ROTATION, 0));
    }

    public static Flagstick defaultInstance() {
        return new Flagstick(defaultSetting());
    }

    //默认设置
    private static AbstractBlock.Settings defaultSetting() {    return Settings.of(Material.STONE);}

    public Item.Settings itemDefaultSetting() {
        return new Item.Settings().maxCount(maxCount);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            player.sendMessage(Text.of("这是一个红旗杆，用力插吧!"), false);
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack){
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
                    if(downBlockState.isOf(RegisterBlocks.GOLF_HOLE)){
                        world.breakBlock(downBlockPos, false);
                    }
                    this.dropStack(world, pos, new ItemStack(RegisterBlocks.FLAGSTICK));
                }
                world.updateComparators(pos, this);
            }
            super.onStateReplaced(state, world, pos, newState, moved);
        }
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos downBlockPos = pos.down();
        BlockState downBlockState = world.getBlockState(downBlockPos);
        return downBlockState.isIn(BlockTags.DIRT) || downBlockState.isOf(Blocks.FARMLAND);
    }
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return this.getDefaultState().with(ROTATION, MathHelper.floor((double)(ctx.getPlayerYaw() * 16.0F / 360.0F) + 0.5) & 15);
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
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(ROTATION);
    }
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FlagstickEntity(pos, state);
    }

}
