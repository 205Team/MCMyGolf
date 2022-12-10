package net.fabricmc.mygolf.blocks;

import net.fabricmc.mygolf.blocks.base.BaseBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class GolfBallBlock extends BaseBlock {

    public GolfBallBlock(AbstractBlock.Settings settings) {
        super(settings);
    }

    public static GolfBallBlock defaultInstance() {
        return new GolfBallBlock(defaultSetting());
    }

    //默认设置
    private static AbstractBlock.Settings defaultSetting() {
        return Settings.of(Material.STONE).hardness(2.0f);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            player.sendMessage(Text.of("这是一个高尔夫球，将它打入洞中吧!"), false);
        }

        return ActionResult.SUCCESS;
    }
}
