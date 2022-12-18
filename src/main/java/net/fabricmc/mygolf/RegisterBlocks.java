package net.fabricmc.mygolf;

import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.fabricmc.mygolf.blocks.Flagstick;
import net.fabricmc.mygolf.blocks.GolfHole;
import net.fabricmc.mygolf.blocks.base.BaseBlock;
import net.fabricmc.mygolf.blockEntity.base.BaseBlockEntity;
import net.fabricmc.mygolf.blocks.base.BaseBlockWithEntity;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class RegisterBlocks {
    /**
     * 方块声明
     */
    public static final Flagstick FLAGSTICK = Flagstick.defaultInstance();    //红旗杆方块
    public static final GolfBall GOLF_BALL = GolfBall.defaultInstance();    //高尔夫球方块
    public static final GolfHole GOLF_HOLE = GolfHole.defaultInstance();    //球洞方块

    /**
     * 注册方块
     */
    public static void registryBlocks() {
        registryBlock(FLAGSTICK);
        registryBlock(GOLF_BALL);
        registryBlock(GOLF_HOLE);
    }

    /**
     * 注册新BaseBlock
     * @param block 物品
     */
    public static void registryBlock(BaseBlock block) {
        registryBlock(block, block.codeName(), block.itemDefaultSetting());
    }

    /**
     * 注册新BaseBlockWithEntity
     * @param block 物品
     */
    public static void registryBlock(BaseBlockWithEntity block) {
        registryBlock(block, block.codeName(), block.itemDefaultSetting());
    }

    /**
     * 注册新block
     * @param block 方块
     * @param codeName 代号
     * @param settings BlockItem的设置
     */
    public static void registryBlock(Block block, String codeName, Item.Settings settings) {
        Registry.register(Registry.BLOCK, new Identifier(CommonStr.modId, codeName), block);
        Registry.register(Registry.ITEM, new Identifier(CommonStr.modId, codeName), new BlockItem(block, settings));
    }
}
