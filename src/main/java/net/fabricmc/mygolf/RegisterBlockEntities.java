package net.fabricmc.mygolf;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class RegisterBlockEntities {
    public static BlockEntityType<FlagstickEntity> FLAGSTICK_ENTITY;
    public static void registerBlockEntities(){
        FLAGSTICK_ENTITY = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(CommonStr.modId, "flagstick_entity"),
                FabricBlockEntityTypeBuilder.create(FlagstickEntity::new, RegisterBlocks.FLAGSTICK).build()
        );
    }

}

