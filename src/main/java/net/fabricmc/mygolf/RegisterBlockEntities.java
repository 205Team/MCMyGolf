package net.fabricmc.mygolf;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.mygolf.blockEntity.FlagstickEntity;
import net.fabricmc.mygolf.global.CommonStr;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static net.fabricmc.mygolf.RegisterBlocks.FLAGSTICK;

public class RegisterBlockEntities {

    public static final BlockEntityType<FlagstickEntity> FLAGSTICK_ENTITY = Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            new Identifier(CommonStr.modId, "flagstick_entity"),
            FabricBlockEntityTypeBuilder.create(FlagstickEntity::new, FLAGSTICK).build()
    );

}

