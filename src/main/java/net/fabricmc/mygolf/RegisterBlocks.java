package net.fabricmc.mygolf;

import net.fabricmc.mygolf.blocks.GolfBallBlock;
import net.fabricmc.mygolf.blocks.base.BaseBlock;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class RegisterBlocks {
    /**
     * 方块声明
     */
    public static final GolfBallBlock GOLF_BALL_BLOCK = GolfBallBlock.defaultInstance();    //高尔夫球方块

    /**
     * 注册方块
     */
    public static void registryBlocks() {
        registryBlock(GOLF_BALL_BLOCK);
    }

    /**
     * 注册新BaseBlock
     * @param block 物品
     */
    public static void registryBlock(BaseBlock block) {
        registryBlock(block, block.codeName());
    }


    /**
     * 注册新block
     * @param block 方块
     * @param codeName 代号
     */
    public static void registryBlock(Block block, String codeName) {
        Registry.register(Registry.BLOCK, new Identifier(CommonStr.modId, codeName), block);
    }
}
