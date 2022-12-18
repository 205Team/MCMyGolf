package net.fabricmc.mygolf.blocks;

import net.fabricmc.mygolf.RegisterBlocks;
import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.fabricmc.mygolf.blocks.base.BaseBlockWithEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
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
import org.jetbrains.annotations.Nullable;

public class Flagstick extends BaseBlockWithEntity  {
    static int maxCount = 1;    //最大堆叠数量
    public static final IntProperty ROTATION;
    protected static final VoxelShape SHAPE;
    static {
        ROTATION = Properties.ROTATION;
        SHAPE = VoxelShapes.cuboid(0.4375f, 0, 0.4375f, 0.5625f, 1f, 0.5625f);
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
        return new Item.Settings().maxCount(maxCount).group(ItemGroup.MISC);
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

    /**
     * 红旗杆方块被添加到世界时
     *
    */
    @Override
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify){
        //检查下方一个方块是不是草/泥方块，是就在下面挖洞
        BlockPos downBlockPos = pos.down();
        Block downBlock = world.getBlockState(downBlockPos).getBlock();
        BlockState golfHoleState = RegisterBlocks.GOLF_HOLE.getDefaultState();
        if(downBlock == Blocks.GRASS_BLOCK || downBlock == Blocks.DIRT){
            world.setBlockState(downBlockPos, golfHoleState);
        }
    }
    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
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
        builder.add(new Property[]{ROTATION});
    }
    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FlagstickEntity(pos, state);
    }

}
