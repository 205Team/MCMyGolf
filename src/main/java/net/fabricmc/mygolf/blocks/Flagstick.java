package net.fabricmc.mygolf.blocks;

import net.fabricmc.mygolf.blocks.base.BaseBlock;
import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class Flagstick extends BaseBlock {
    static int maxCount = 1;    //最大堆叠数量
    public Flagstick(Settings settings) {
        super(settings);
    }

    public static Flagstick defaultInstance() {
        return new Flagstick(defaultSetting());
    }

    //默认设置
    private static AbstractBlock.Settings defaultSetting() {    return Settings.of(Material.PLANT);}

    public Item.Settings itemDefaultSetting() {
        return new Item.Settings().maxCount(maxCount).group(ItemGroup.MISC);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView view, BlockPos pos, ShapeContext context) {
        return VoxelShapes.cuboid(0.4375f, 0, 0.4375f, 0.5625f, 1f, 0.5625f);
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
    public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify){
        Block downBlock = world.getBlockState(pos.down()).getBlock();
        if(downBlock == Blocks.GRASS_BLOCK)
            System.out.println("w");
        //检查下方一个方块是不是草/泥方块，是就在下面挖洞
    }
}
